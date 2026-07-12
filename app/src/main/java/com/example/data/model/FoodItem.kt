package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FoodItem(
    val name: String,
    val portionSize: String, // e.g. "1.0 Serving", "1.5 Servings", "150g"
    val portionValue: Float = 1.0f, // numeric value for multiplier calculations
    val baseCalories: Int,   // Calories for 1.0 portion
    val baseProtein: Int,    // Protein (g) for 1.0 portion
    val baseCarbs: Int,      // Carbs (g) for 1.0 portion
    val baseFat: Int,        // Fat (g) for 1.0 portion
    val baseFiber: Int,      // Fiber (g) for 1.0 portion
    val calories: Int = baseCalories, // Current calculated values
    val protein: Int = baseProtein,
    val carbs: Int = baseCarbs,
    val fat: Int = baseFat,
    val fiber: Int = baseFiber
) {
    // Return recalculated item based on portion multiplier
    fun recalculate(newPortion: Float): FoodItem {
        val multiplier = newPortion / portionValue
        return copy(
            portionValue = newPortion,
            portionSize = if (newPortion == 1.0f) "1.0 Serving" else "$newPortion Servings",
            calories = (baseCalories * newPortion).toInt(),
            protein = (baseProtein * newPortion).toInt(),
            carbs = (baseCarbs * newPortion).toInt(),
            fat = (baseFat * newPortion).toInt(),
            fiber = (baseFiber * newPortion).toInt()
        )
    }
}
