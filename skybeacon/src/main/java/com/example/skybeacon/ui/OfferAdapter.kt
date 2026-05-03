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
import com.example.skybeacon.data.Offer

class OfferAdapter(
    private val onAdvertise: (Offer) -> Unit,
    private val onDelete: (Offer) -> Unit
) : ListAdapter<Offer, OfferAdapter.OfferViewHolder>(OfferDiffCallback()) {

    class OfferViewHolder(
        itemView: View,
        val onAdvertise: (Offer) -> Unit,
        val onDelete: (Offer) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_offer_title)
        private val tvDiscount: TextView = itemView.findViewById(R.id.tv_offer_discount)
        private val btnAdvertise: Button = itemView.findViewById(R.id.btn_advertise_offer)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete_offer)

        fun bind(offer: Offer) {
            tvTitle.text = offer.title
            tvDiscount.text = "${offer.discountPercent}% OFF"
            
            btnAdvertise.setOnClickListener { onAdvertise(offer) }
            btnDelete.setOnClickListener { onDelete(offer) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfferViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_offer, parent, false)
        return OfferViewHolder(view, onAdvertise, onDelete)
    }

    override fun onBindViewHolder(holder: OfferViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class OfferDiffCallback : DiffUtil.ItemCallback<Offer>() {
    override fun areItemsTheSame(oldItem: Offer, newItem: Offer): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Offer, newItem: Offer): Boolean = oldItem == newItem
}
