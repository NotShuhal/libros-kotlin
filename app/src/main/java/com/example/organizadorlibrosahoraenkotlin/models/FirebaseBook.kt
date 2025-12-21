package com.example.organizadorlibrosahoraenkotlin.models

data class FirebaseBook(
    val title: String = "",
    val author: String = "",
    val publisher: String = "",
    val note: String = "",
    val forTrade: Boolean = true
)
