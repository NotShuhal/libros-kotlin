package com.example.organizadorlibrosahoraenkotlin.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.organizadorlibrosahoraenkotlin.R
import com.example.organizadorlibrosahoraenkotlin.models.Book

class BookAdapter(
    private var books: List<Book>,
    private val onDeleteClick: (Book) -> Unit,
    private val onTradeToggle: (Book, Boolean) -> Unit,
    private val onWishlistToggle: (Book, Boolean) -> Unit
) : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {

    inner class BookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.tvBookTitle)
        val author: TextView = itemView.findViewById(R.id.tvBookAuthor)
        val publisher: TextView = itemView.findViewById(R.id.tvBookPublisher)
        val note: TextView = itemView.findViewById(R.id.tvBookNote)
        val switchTrade: Switch = itemView.findViewById(R.id.switchForTrade)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteBook)
        val switchWishlist: Switch = itemView.findViewById(R.id.switchWishlist)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_book, parent, false)
        return BookViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = books[position]

        holder.title.text = book.title
        holder.author.text = book.author
        holder.publisher.text = book.publisher
        holder.note.text = book.note ?: ""

        // IMPORTANTE: limpiar listeners antes de setear estado
        holder.switchTrade.setOnCheckedChangeListener(null)
        holder.switchWishlist.setOnCheckedChangeListener(null)

        holder.switchTrade.isChecked = book.forTrade
        holder.switchWishlist.isChecked = book.wishlist

        holder.switchTrade.setOnCheckedChangeListener { _, isChecked ->
            onTradeToggle(book, isChecked)
        }

        holder.switchWishlist.setOnCheckedChangeListener { _, isChecked ->
            onWishlistToggle(book, isChecked)
        }

        holder.btnDelete.setOnClickListener {
            onDeleteClick(book)
        }
    }

    override fun getItemCount(): Int = books.size

    fun updateBooks(newBooks: List<Book>) {
        books = newBooks
        notifyDataSetChanged()
    }
}
