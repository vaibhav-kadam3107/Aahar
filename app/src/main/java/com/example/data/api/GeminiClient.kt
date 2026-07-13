package com.example.data.api

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.FoodItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.HttpException
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Path
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val apiService: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    /**
     * Resizes the bitmap to safe limits to prevent OutOfMemory issues and reduces network payload.
     * Resizing to 512 max dimension reduces base64 size significantly, helping prevent HTTP 503 error.
     */
    private fun resizeBitmap(bitmap: Bitmap, maxDimension: Int = 512): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap

        val ratio = width.toFloat() / height.toFloat()
        val (newWidth, newHeight) = if (ratio > 1) {
            Pair(maxDimension, (maxDimension / ratio).toInt())
        } else {
            Pair((maxDimension * ratio).toInt(), maxDimension)
        }
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Converts a Bitmap into a JPEG base64 string.
     */
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val resized = resizeBitmap(bitmap, 512)
        val outputStream = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /**
     * Custom Exception to carry the detailed HTTP error body from Gemini API.
     */
    class HttpExceptionWithDetail(
        val code: Int,
        val errorBody: String,
        cause: Throwable
    ) : Exception("HTTP $code: $errorBody", cause)

    /**
     * Helper to execute suspend blocks with exponential backoff retry and jitter.
     */
    private suspend fun <T> retryWithBackoff(
        retries: Int = 5,
        initialDelayMillis: Long = 1000,
        maxDelayMillis: Long = 16000,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelayMillis
        repeat(retries - 1) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                // Add jitter: ±25% random variation to avoid synchronization of retries
                val jitter = (currentDelay * (0.75 + Math.random() * 0.5)).toLong()
                Log.w(TAG, "API call failed (attempt ${attempt + 1} of $retries): ${e.message}. Retrying in ${jitter}ms...")
                delay(jitter)
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelayMillis)
            }
        }
        return block()
    }

    /**
     * Executes the Gemini call with comprehensive diagnostic logs.
     */
    private suspend fun executeWithDiagnostics(
        modelName: String,
        apiKey: String,
        request: GeminiRequest
    ): GeminiResponse {
        Log.d(TAG, "[DIAGNOSTIC] Dispatching API request to model: $modelName")
        try {
            val response = apiService.generateContent(modelName, apiKey, request)
            Log.d(TAG, "[DIAGNOSTIC] Model $modelName call succeeded.")
            return response
        } catch (e: HttpException) {
            val code = e.code()
            val errorBody = e.response()?.errorBody()?.string() ?: "Empty body"
            val headers = e.response()?.headers()?.toString() ?: "No headers"
            Log.e(TAG, "[DIAGNOSTIC] Model $modelName failed with HttpException ($code)")
            Log.e(TAG, "[DIAGNOSTIC] Headers:\n$headers")
            Log.e(TAG, "[DIAGNOSTIC] Error Response Body:\n$errorBody")
            throw HttpExceptionWithDetail(code, errorBody, e)
        } catch (e: Exception) {
            Log.e(TAG, "[DIAGNOSTIC] Model $modelName failed with non-HTTP Exception: ${e.message}", e)
            throw e
        }
    }

    /**
     * Sends the bitmap to Gemini to identify foods, portions, and estimate nutrition.
     */
    suspend fun analyzeMealPhoto(bitmap: Bitmap): List<FoodItem> {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API key is not configured or is a placeholder.")
            throw IllegalStateException("Gemini API Key is missing. Please add it via the Secrets panel in AI Studio.")
        }

        val base64Image = bitmapToBase64(bitmap)
        val payloadSizeKB = (base64Image.length * 3 / 4) / 1024.0
        Log.d(TAG, "[DIAGNOSTIC] Image base64 compression complete. Base64 length: ${base64Image.length} characters. Final payload size: ${"%.2f".format(payloadSizeKB)} KB")
        
        val prompt = """
            Identify all food items in this photo, estimating their portion sizes (e.g. "1.0 Serving", "1.5 Servings", "150g").
            For each item, return its estimated calories, protein (g), carbs (g), fat (g), and fiber (g) for a standard "1.0" multiplier of that item (baseCalories, baseProtein, baseCarbs, baseFat, baseFiber), and also return current computed values for the given portion.
            
            Return this as a structured JSON array of food objects with these exact keys:
            - name (String): Short descriptive food name (e.g. "Grilled Salmon Fillet", "Boiled Quinoa", "Steamed Broccoli")
            - portionSize (String): Friendly portion description (e.g. "1.0 Serving", "1.5 Servings")
            - portionValue (Float): The estimated serving multiplier (e.g., 1.0, 1.5, 2.0)
            - baseCalories (Int): Calories for 1.0 portion
            - baseProtein (Int): Protein in grams for 1.0 portion
            - baseCarbs (Int): Carbs in grams for 1.0 portion
            - baseFat (Int): Fat in grams for 1.0 portion
            - baseFiber (Int): Fiber in grams for 1.0 portion
            - calories (Int): Total calories for estimated portion (baseCalories * portionValue)
            - protein (Int): Total protein for estimated portion (baseProtein * portionValue)
            - carbs (Int): Total carbs for estimated portion (baseCarbs * portionValue)
            - fat (Int): Total fat for estimated portion (baseFat * portionValue)
            - fiber (Int): Total fiber for estimated portion (baseFiber * portionValue)
            
            Ensure nutrition values are reasonable estimates based on nutritional science.
            Return ONLY a raw JSON array. Do not enclose it in backticks, markdown markers, or write any introductory conversational text.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(text = prompt),
                        GeminiPart(inlineData = GeminiInlineData(mimeType = "image/jpeg", data = base64Image))
                    )
                )
            ),
            generationConfig = GeminiGenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.2f
            )
        )

        try {
            val response = retryWithBackoff {
                try {
                    executeWithDiagnostics("gemini-3.5-flash", apiKey, request)
                } catch (e: Exception) {
                    Log.w(TAG, "[DIAGNOSTIC] Primary model gemini-3.5-flash failed, attempting fallback model gemini-3.1-pro-preview...", e)
                    executeWithDiagnostics("gemini-3.1-pro-preview", apiKey, request)
                }
            }
            
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (jsonText.isNullOrEmpty()) {
                Log.e(TAG, "Empty text response from Gemini API")
                return emptyList()
            }
            
            Log.d(TAG, "Raw response from Gemini: $jsonText")

            // Clean markdown syntax if Gemini ignores responseMimeType instructions
            var cleanedJson = jsonText.trim()
            if (cleanedJson.startsWith("```json")) {
                cleanedJson = cleanedJson.removePrefix("```json")
            }
            if (cleanedJson.startsWith("```")) {
                cleanedJson = cleanedJson.removePrefix("```")
            }
            if (cleanedJson.endsWith("```")) {
                cleanedJson = cleanedJson.removeSuffix("```")
            }
            cleanedJson = cleanedJson.trim()

            val listType = Types.newParameterizedType(List::class.java, FoodItem::class.java)
            val adapter = moshi.adapter<List<FoodItem>>(listType)
            return adapter.fromJson(cleanedJson) ?: emptyList()

        } catch (e: Exception) {
            Log.e(TAG, "Error calling Gemini API: ", e)
            throw e
        }
    }

    /**
     * Sends a chat message to Gemini with conversation history and system instructions (user context).
     */
    suspend fun getCoachResponse(
        history: List<com.example.data.model.ChatMessage>,
        userContextSystemInstruction: String
    ): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API key is not configured or is a placeholder.")
            throw IllegalStateException("Gemini API Key is missing. Please add it via the Secrets panel in AI Studio.")
        }

        val systemInstructionContent = GeminiContent(
            role = "system",
            parts = listOf(GeminiPart(text = userContextSystemInstruction))
        )

        val contents = history.map { message ->
            val role = if (message.sender == "user") "user" else "model"
            GeminiContent(
                role = role,
                parts = listOf(GeminiPart(text = message.text))
            )
        }

        val request = GeminiRequest(
            contents = contents,
            generationConfig = GeminiGenerationConfig(
                temperature = 0.7f
            ),
            systemInstruction = systemInstructionContent
        )

        try {
            val response = retryWithBackoff {
                try {
                    executeWithDiagnostics("gemini-3.5-flash", apiKey, request)
                } catch (e: Exception) {
                    Log.w(TAG, "[DIAGNOSTIC] Primary model gemini-3.5-flash failed, attempting fallback model gemini-3.1-pro-preview...", e)
                    executeWithDiagnostics("gemini-3.1-pro-preview", apiKey, request)
                }
            }
            
            val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (responseText.isNullOrEmpty()) {
                Log.e(TAG, "Empty text response from Gemini API")
                return "I'm sorry, I couldn't generate a response. Please try again."
            }
            return responseText
        } catch (e: Exception) {
            Log.e(TAG, "Error in getCoachResponse calling Gemini API: ", e)
            throw e
        }
    }
}
