package com.example.organizadorlibrosahoraenkotlin.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.organizadorlibrosahoraenkotlin.R
import com.example.organizadorlibrosahoraenkotlin.models.FirebaseWishlistBook

class WishlistAdapter(
    private var items: MutableList<FirebaseWishlistBook>
) : RecyclerView.Adapter<WishlistAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvAuthor: TextView = view.findViewById(R.id.tvAuthor)
        val tvPublisher: TextView = view.findViewById(R.id.tvPublisher)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_wishlist, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val book = items[position]
        holder.tvTitle.text = book.title
        holder.tvAuthor.text = book.author
        holder.tvPublisher.text = book.publisher
    }

    override fun getItemCount() = items.size

    fun update(newItems: List<FirebaseWishlistBook>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
