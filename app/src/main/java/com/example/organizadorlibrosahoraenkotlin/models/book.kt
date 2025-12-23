package com.example.organizadorlibrosahoraenkotlin.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class Book(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val author: String,
    val publisher: String,
    val note: String? = null,

    // intercambio
    val forTrade: Boolean = false,

    // lista de deseados
    val wishlist: Boolean = false
)
