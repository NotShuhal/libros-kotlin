package com.example.organizadorlibrosahoraenkotlin.models

data class UserNearby(
    val uid: String,
    val username: String,
    val distanceKm: Double,
    val matchedBooks: List<String>
)
