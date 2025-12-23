package com.example.organizadorlibrosahoraenkotlin.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.organizadorlibrosahoraenkotlin.models.Book

@Database(entities = [Book::class], version = 3)
abstract class BookDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
}
