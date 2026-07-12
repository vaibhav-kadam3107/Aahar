package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "meal_logs")
@JsonClass(generateAdapter = true)
data class MealLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val totalCalories: Int,
    val totalProtein: Int,
    val totalCarbs: Int,
    val totalFat: Int,
    val totalFiber: Int,
    val imageUri: String?, // Location of captured food image
    val itemsJson: String, // JSON serialized List<FoodItem>
    val timestamp: Long = System.currentTimeMillis()
)
