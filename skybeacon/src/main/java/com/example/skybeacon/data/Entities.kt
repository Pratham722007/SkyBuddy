package com.example.skybeacon.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shop_profiles")
data class ShopProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ownerEmail: String,
    val shopName: String,
    val category: String,
    val terminal: String,
    val contactEmail: String
)

@Entity(tableName = "broadcast_messages")
data class BroadcastMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val shopId: Int,
    val messageText: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "offers")
data class Offer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val shopId: Int,
    val title: String,
    val discountPercent: Int,
    val validUntil: Long,
    val isActive: Boolean = true
)

@Entity(tableName = "broadcast_logs")
data class BroadcastLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val shopName: String,
    val broadcastType: String, // "SOS", "Offer", "Message"
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)
