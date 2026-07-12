package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ChatMessage(
    val id: String,
    val sender: String, // "user" or "coach"
    val text: String,
    val timestamp: Long
)
