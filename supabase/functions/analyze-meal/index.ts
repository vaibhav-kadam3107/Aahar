import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type, x-idempotency-key",
};

serve(async (req) => {
  // Handle CORS
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    // 1. Validate auth token/session and reject unauthenticated requests
    const authHeader = req.headers.get("Authorization");
    if (!authHeader) {
      return new Response(JSON.stringify({ error: "Missing authorization token" }), {
        status: 401,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
    const supabaseServiceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
    const supabase = createClient(supabaseUrl, supabaseServiceKey);

    const token = authHeader.replace("Bearer ", "");
    const { data: { user }, error: authError } = await supabase.auth.getUser(token);

    if (authError || !user) {
      return new Response(JSON.stringify({ error: "Unauthorized user session" }), {
        status: 401,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    const userId = user.id;

    // 2. Parse request body
    const { image_path } = await req.json();
    if (!image_path) {
      return new Response(JSON.stringify({ error: "Missing image_path parameter" }), {
        status: 400,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    const idempotencyKey = req.headers.get("x-idempotency-key") || null;

    // 3. Check for existing meal with idempotency key
    if (idempotencyKey) {
      const { data: existingMeal, error: checkError } = await supabase
        .from("meals")
        .select(`*, meal_food_items(*)`)
        .eq("idempotency_key", idempotencyKey)
        .eq("user_id", userId)
        .single();

      if (existingMeal) {
        console.log(`Idempotency match found for key: ${idempotencyKey}. Returning saved meal.`);
        // Generate signed URL
        const { data: signedData, error: signError } = await supabase.storage
          .from("meals")
          .createSignedUrl(existingMeal.image_path, 3600);

        return new Response(JSON.stringify({
          meal: existingMeal,
          signed_url: signedData?.signedUrl || null,
          idempotent: true
        }), {
          status: 200,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        });
      }
    }

    // 4. Retrieve the image securely server-side
    const { data: imageBlob, error: downloadError } = await supabase.storage
      .from("meals")
      .download(image_path);

    if (downloadError || !imageBlob) {
      return new Response(JSON.stringify({ error: `Failed to download image from storage: ${downloadError?.message}` }), {
        status: 400,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    // Convert blob to base64
    const arrayBuffer = await imageBlob.arrayBuffer();
    const bytes = new Uint8Array(arrayBuffer);
    let binary = "";
    for (let i = 0; i < bytes.byteLength; i++) {
      binary += String.fromCharCode(bytes[i]);
    }
    const base64Image = btoa(binary);

    // 5. Send to Gemini using the server-side API key with retry and fallback
    const geminiKey = Deno.env.get("GEMINI_API_KEY");
    if (!geminiKey) {
      return new Response(JSON.stringify({ error: "Server-side Gemini API key not configured" }), {
        status: 500,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    const promptText = `You are a nutrition analysis assistant.
Analyze the provided food image. Identify all visible food items.
Estimate: food name, portion size, estimated weight in grams, calories, protein, carbohydrates, fat, fiber, and any clearly relevant micronutrients (e.g. iron, magnesium) if identifiable.
Return only valid structured JSON. Do not include markdown. Do not include explanatory text outside the JSON.
Use this structure:
{
  "meal_name": "string",
  "confidence_score": 0.0,
  "food_items": [
    {
      "food_name": "string",
      "estimated_quantity": "string",
      "estimated_grams": 0,
      "calories": 0,
      "protein_grams": 0,
      "carbohydrates_grams": 0,
      "fat_grams": 0,
      "fiber_grams": 0,
      "micronutrients": {}
    }
  ],
  "total_nutrition": {
    "calories": 0,
    "protein_grams": 0,
    "carbohydrates_grams": 0,
    "fat_grams": 0,
    "fiber_grams": 0,
    "micronutrients": {}
  }
}
Nutrition values are estimates. If the image is ambiguous, reduce the confidence score rather than inventing excessive precision.`;

    const geminiRequestBody = {
      contents: [{
        parts: [
          { text: promptText },
          { inlineData: { mimeType: "image/jpeg", data: base64Image } }
        ]
      }],
      generationConfig: {
        responseMimeType: "application/json"
      }
    };

    // Helper for robust retry and fallback
    let lastError = null;
    let geminiResponseJson = null;

    const modelsToTry = ["gemini-1.5-flash", "gemini-1.5-pro"];

    for (const model of modelsToTry) {
      let delayMs = 1000;
      for (let attempt = 1; attempt <= 3; attempt++) {
        try {
          console.log(`Calling Gemini Model: ${model} (Attempt ${attempt}/3)...`);
          const geminiUrl = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${geminiKey}`;
          
          const geminiRes = await fetch(geminiUrl, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(geminiRequestBody),
          });

          if (!geminiRes.ok) {
            const errBody = await geminiRes.text();
            throw new Error(`HTTP ${geminiRes.status}: ${errBody}`);
          }

          const resData = await geminiRes.json();
          const textResponse = resData.candidates?.[0]?.content?.parts?.[0]?.text;
          
          if (!textResponse) {
            throw new Error("Empty text in Gemini response");
          }

          // Clean markdown structure if any remains
          let cleaned = textResponse.trim();
          if (cleaned.startsWith("```json")) cleaned = cleaned.substring(7);
          if (cleaned.startsWith("```")) cleaned = cleaned.substring(3);
          if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.length - 3);
          cleaned = cleaned.trim();

          // Try parsing
          const parsed = JSON.parse(cleaned);
          
          // Validate Gemini output schema
          if (
            !parsed.meal_name ||
            typeof parsed.confidence_score !== "number" ||
            !Array.isArray(parsed.food_items) ||
            !parsed.total_nutrition
          ) {
            throw new Error("Invalid output JSON schema from Gemini");
          }

          geminiResponseJson = parsed;
          break; // success!
        } catch (err) {
          console.warn(`Attempt ${attempt} on model ${model} failed: ${err.message}`);
          lastError = err;
          // Exponential backoff with jitter
          if (attempt < 3) {
            const jitter = Math.random() * 500;
            await new Promise((r) => setTimeout(r, delayMs + jitter));
            delayMs *= 2;
          }
        }
      }

      if (geminiResponseJson) {
        break; // if we successfully got results, don't try fallback models
      }
    }

    if (!geminiResponseJson) {
      return new Response(JSON.stringify({ error: `Gemini API failure after retry and fallback. Last error: ${lastError?.message}` }), {
        status: 502,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    // 6. Save meal and food items transactionally using custom Postgres RPC
    const mealData = geminiResponseJson;
    const totalNutrition = mealData.total_nutrition;

    // Call the postgres rpc transaction function
    const { data: dbResult, error: txError } = await supabase.rpc("create_meal_transaction", {
      p_user_id: userId,
      p_image_path: image_path,
      p_meal_type: "Any",
      p_meal_name: mealData.meal_name,
      p_total_calories: totalNutrition.calories || 0,
      p_total_protein: totalNutrition.protein_grams || totalNutrition.protein || 0,
      p_total_carbohydrates: totalNutrition.carbohydrates_grams || totalNutrition.carbohydrates || 0,
      p_total_fat: totalNutrition.fat_grams || totalNutrition.fat || 0,
      p_total_fiber: totalNutrition.fiber_grams || totalNutrition.fiber || 0,
      p_micronutrients: totalNutrition.micronutrients || {},
      p_confidence_score: mealData.confidence_score || 1.0,
      p_food_items: mealData.food_items,
      p_idempotency_key: idempotencyKey
    });

    if (txError) {
      console.error("Database transaction failed:", txError);
      return new Response(JSON.stringify({ error: `Failed to persist meal transaction: ${txError.message}` }), {
        status: 500,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    // 7. Generate temporary signed URL to show image in app
    const { data: signedData } = await supabase.storage
      .from("meals")
      .createSignedUrl(image_path, 3600);

    return new Response(JSON.stringify({
      success: true,
      meal_id: dbResult.meal_id,
      meal_name: mealData.meal_name,
      total_nutrition: totalNutrition,
      food_items: mealData.food_items,
      signed_url: signedData?.signedUrl || null
    }), {
      status: 200,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });

  } catch (err) {
    console.error("Unhandled Edge Function error:", err);
    return new Response(JSON.stringify({ error: `Internal server error: ${err.message}` }), {
      status: 500,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }
});
