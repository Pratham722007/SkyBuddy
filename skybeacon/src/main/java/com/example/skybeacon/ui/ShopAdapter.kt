package com.example.skybeacon.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.skybeacon.R
import com.example.skybeacon.data.ShopProfile

class ShopAdapter(private val onClick: (ShopProfile) -> Unit) : 
    ListAdapter<ShopProfile, ShopAdapter.ShopViewHolder>(ShopDiffCallback()) {

    class ShopViewHolder(itemView: View, val onClick: (ShopProfile) -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tv_shop_name)
        private val tvCategory: TextView = itemView.findViewById(R.id.tv_shop_category)
        private var currentShop: ShopProfile? = null

        init {
            itemView.setOnClickListener {
                currentShop?.let { onClick(it) }
            }
        }

        fun bind(shop: ShopProfile) {
            currentShop = shop
            tvName.text = shop.shopName
            tvCategory.text = "${shop.category} • ${shop.terminal}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShopViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_shop, parent, false)
        return ShopViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: ShopViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class ShopDiffCallback : DiffUtil.ItemCallback<ShopProfile>() {
    override fun areItemsTheSame(oldItem: ShopProfile, newItem: ShopProfile): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: ShopProfile, newItem: ShopProfile): Boolean {
        return oldItem == newItem
    }
}
