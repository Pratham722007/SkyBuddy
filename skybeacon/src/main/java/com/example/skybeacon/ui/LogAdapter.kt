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
import com.example.skybeacon.data.BroadcastLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogAdapter(private val onBroadcast: (BroadcastLog) -> Unit) : 
    ListAdapter<BroadcastLog, LogAdapter.LogViewHolder>(LogDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_log, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val log = getItem(position)
        holder.bind(log, onBroadcast)
    }

    class LogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvText: TextView = itemView.findViewById(R.id.tv_log_text)
        private val btnBroadcast: Button = itemView.findViewById(R.id.btn_rebroadcast)

        fun bind(log: BroadcastLog, onBroadcast: (BroadcastLog) -> Unit) {
            val date = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(log.createdAt))
            tvText.text = "${log.shopName} [${log.broadcastType}]\n${log.content}\n$date"
            
            btnBroadcast.setOnClickListener { onBroadcast(log) }
        }
    }

    class LogDiffCallback : DiffUtil.ItemCallback<BroadcastLog>() {
        override fun areItemsTheSame(oldItem: BroadcastLog, newItem: BroadcastLog): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: BroadcastLog, newItem: BroadcastLog): Boolean = oldItem == newItem
    }
}
