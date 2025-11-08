package com.example.organizadorlibrosahoraenkotlin.data

import androidx.room.*
import com.example.organizadorlibrosahoraenkotlin.models.Book

@Dao
interface BookDao {
    @Query("SELECT * FROM books")
    suspend fun getAll(): List<Book>

    @Insert
    suspend fun insert(book: Book)

    @Delete
    suspend fun delete(book: Book)
}