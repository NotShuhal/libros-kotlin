package com.example.organizadorlibrosahoraenkotlin.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.organizadorlibrosahoraenkotlin.R
import com.example.organizadorlibrosahoraenkotlin.models.ChatMessage
import com.google.firebase.auth.FirebaseAuth
import android.widget.ImageView
import com.bumptech.glide.Glide

class ChatAdapter(
    private val messages: List<ChatMessage>
) : RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

    private val currentUid = FirebaseAuth.getInstance().currentUser?.uid

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        val ivImage: ImageView = view.findViewById(R.id.ivMessageImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = if (viewType == 0)
            R.layout.item_message_sent
        else
            R.layout.item_message_received

        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = messages[position]

        if (!message.imageUrl.isNullOrEmpty()) {
            holder.ivImage.visibility = View.VISIBLE
            holder.tvMessage.visibility = View.GONE

            Glide.with(holder.itemView.context)
                .load(message.imageUrl)
                .into(holder.ivImage)
        } else {
            holder.ivImage.visibility = View.GONE
            holder.tvMessage.visibility = View.VISIBLE
            holder.tvMessage.text = message.text
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderId == currentUid) 0 else 1
    }

    override fun getItemCount() = messages.size
}

