package com.example.data.local

import androidx.room.*
import com.example.data.model.DailyGoals
import com.example.data.model.MealLog
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    @Query("SELECT * FROM meal_logs ORDER BY timestamp DESC")
    fun getAllMealLogs(): Flow<List<MealLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealLog(mealLog: MealLog): Long

    @Update
    suspend fun updateMealLog(mealLog: MealLog)

    @Delete
    suspend fun deleteMealLog(mealLog: MealLog)

    @Query("DELETE FROM meal_logs WHERE id = :id")
    suspend fun deleteMealLogById(id: Int)

    @Query("SELECT * FROM daily_goals WHERE id = 1")
    fun getDailyGoals(): Flow<DailyGoals?>

    @Query("SELECT * FROM daily_goals WHERE id = 1")
    suspend fun getDailyGoalsOnce(): DailyGoals?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateGoals(goals: DailyGoals)
}
