package com.jensssen.wafflepod.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jensssen.wafflepod.R
import com.jensssen.wafflepod.classes.Message


class MessageAdapter(val context: Context, val messageList: ArrayList<Message>):
    RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view: View = LayoutInflater.from(context).inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val currentMessage = messageList[position]
        holder.tvTitle.text = currentMessage.author
        holder.tvMessage.text = currentMessage.message
        holder.tvDate.text = currentMessage.date
    }

    override fun getItemCount(): Int {
        return messageList.size
    }

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)   {
        val tvTitle: TextView = itemView.findViewById<TextView>(R.id.tvAuthor)
        val tvDate: TextView = itemView.findViewById<TextView>(R.id.tvDate)
        val tvMessage: TextView = itemView.findViewById<TextView>(R.id.tvMessage)
    }
}