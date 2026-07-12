package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_goals")
data class DailyGoals(
    @PrimaryKey val id: Int = 1, // Only 1 set of active targets stored
    val calories: Int = 2200,
    val protein: Int = 150,
    val carbs: Int = 250,
    val fat: Int = 70,
    val fiber: Int = 30,
    
    // Profile information
    val userName: String = "Vaibhav Kadam",
    val userEmail: String = "vaibhav.kadam21@vit.edu",
    val profilePhotoUri: String? = null,

    // Body metrics
    val heightCm: Float? = null,
    val weightKg: Float? = null,
    val age: Int? = null,
    val gender: String? = null,

    // Micronutrient targets
    val trackMagnesium: Boolean = true,
    val targetMagnesium: Int = 400, // mg
    val trackIron: Boolean = true,
    val targetIron: Int = 18, // mg
    val trackVitaminD: Boolean = true,
    val targetVitaminD: Int = 20, // mcg
    val trackZinc: Boolean = false,
    val targetZinc: Int = 11 // mg
)
