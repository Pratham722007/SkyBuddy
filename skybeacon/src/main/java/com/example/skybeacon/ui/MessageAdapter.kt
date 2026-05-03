package com.example.skybeacon.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.skybeacon.R
import com.example.skybeacon.data.BroadcastMessage

class MessageAdapter(
    private val onBroadcast: (BroadcastMessage) -> Unit,
    private val onDelete: (BroadcastMessage) -> Unit
) : ListAdapter<BroadcastMessage, MessageAdapter.MessageViewHolder>(MessageDiffCallback()) {

    class MessageViewHolder(
        itemView: View,
        val onBroadcast: (BroadcastMessage) -> Unit,
        val onDelete: (BroadcastMessage) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val tvText: TextView = itemView.findViewById(R.id.tv_message_text)
        private val btnBroadcast: Button = itemView.findViewById(R.id.btn_broadcast)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete_msg)

        fun bind(msg: BroadcastMessage) {
            tvText.text = msg.messageText
            btnBroadcast.setOnClickListener { onBroadcast(msg) }
            btnDelete.setOnClickListener { onDelete(msg) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view, onBroadcast, onDelete)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class MessageDiffCallback : DiffUtil.ItemCallback<BroadcastMessage>() {
    override fun areItemsTheSame(oldItem: BroadcastMessage, newItem: BroadcastMessage): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: BroadcastMessage, newItem: BroadcastMessage): Boolean = oldItem == newItem
}
