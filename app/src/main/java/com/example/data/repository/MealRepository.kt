package com.example.data.repository

import android.graphics.Bitmap
import com.example.data.api.InsForgeClient
import com.example.data.local.MealDao
import com.example.data.model.DailyGoals
import com.example.data.model.FoodItem
import com.example.data.model.MealLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class MealRepository(private val mealDao: MealDao) {

    val allMealLogs: Flow<List<MealLog>> = mealDao.getAllMealLogs()
    
    val dailyGoals: Flow<DailyGoals?> = mealDao.getDailyGoals()

    suspend fun insertMealLog(mealLog: MealLog): Long {
        return mealDao.insertMealLog(mealLog)
    }

    suspend fun updateMealLog(mealLog: MealLog) {
        mealDao.updateMealLog(mealLog)
    }

    suspend fun deleteMealLog(mealLog: MealLog) {
        mealDao.deleteMealLog(mealLog)
    }

    suspend fun deleteMealLogById(id: Int) {
        mealDao.deleteMealLogById(id)
    }

    suspend fun saveGoals(goals: DailyGoals) {
        mealDao.insertOrUpdateGoals(goals)
    }

    suspend fun ensureDefaultGoalsExist() {
        val current = mealDao.getDailyGoalsOnce()
        if (current == null) {
            mealDao.insertOrUpdateGoals(DailyGoals())
        }
    }

    suspend fun analyzePhoto(bitmap: Bitmap, uniqueMealId: String, idempotencyKey: String): List<FoodItem> {
        // Upload image to user-scoped private storage
        val path = InsForgeClient.uploadMealPhoto(bitmap, uniqueMealId)
        
        // Invoke secure server-side edge function
        val result = InsForgeClient.analyzeMeal(path, idempotencyKey)
        
        return result.food_items.map { edgeItem ->
            FoodItem(
                name = edgeItem.food_name,
                portionSize = edgeItem.estimated_quantity ?: "1 Serving",
                portionValue = 1.0f,
                baseCalories = edgeItem.calories,
                baseProtein = edgeItem.protein_grams ?: edgeItem.protein ?: 0,
                baseCarbs = edgeItem.carbohydrates_grams ?: edgeItem.carbohydrates ?: 0,
                baseFat = edgeItem.fat_grams ?: edgeItem.fat ?: 0,
                baseFiber = edgeItem.fiber_grams ?: edgeItem.fiber ?: 0,
                calories = edgeItem.calories,
                protein = edgeItem.protein_grams ?: edgeItem.protein ?: 0,
                carbs = edgeItem.carbohydrates_grams ?: edgeItem.carbohydrates ?: 0,
                fat = edgeItem.fat_grams ?: edgeItem.fat ?: 0,
                fiber = edgeItem.fiber_grams ?: edgeItem.fiber ?: 0
            )
        }
    }
}
