package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.FoodItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class Converters {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
        
    private val listMyModelType = Types.newParameterizedType(List::class.java, FoodItem::class.java)
    private val adapter = moshi.adapter<List<FoodItem>>(listMyModelType)

    @TypeConverter
    fun fromFoodItemList(value: List<FoodItem>?): String {
        return if (value == null) "[]" else adapter.toJson(value)
    }

    @TypeConverter
    fun toFoodItemList(value: String?): List<FoodItem> {
        if (value.isNullOrEmpty()) return emptyList()
        return try {
            adapter.fromJson(value) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
