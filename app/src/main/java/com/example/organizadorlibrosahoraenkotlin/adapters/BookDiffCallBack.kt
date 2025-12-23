package com.example.organizadorlibrosahoraenkotlin.adapters

import androidx.recyclerview.widget.DiffUtil
import com.example.organizadorlibrosahoraenkotlin.models.Book

class BookDiffCallBack (
    private val oldList: List<Book>,
    private val newList: List<Book>
) : DiffUtil.Callback() {

    override fun getOldListSize() = oldList.size
    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
        return oldList[oldPos].id == newList[newPos].id
    }

    override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean {
        return oldList[oldPos] == newList[newPos]
    }
}