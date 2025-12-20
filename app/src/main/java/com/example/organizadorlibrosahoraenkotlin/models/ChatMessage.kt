package com.example.organizadorlibrosahoraenkotlin.models

data class ChatMessage(
    val senderId: String = "",
    val text: String? = null,
    val imageUrl: String? = null,
    val timestamp: Long = 0L
)