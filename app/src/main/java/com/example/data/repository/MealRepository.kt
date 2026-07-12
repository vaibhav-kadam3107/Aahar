package com.example.data.repository

import android.graphics.Bitmap
import com.example.data.api.GeminiClient
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

    suspend fun analyzePhoto(bitmap: Bitmap): List<FoodItem> {
        return GeminiClient.analyzeMealPhoto(bitmap)
    }
}
