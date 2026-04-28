package com.example.skybuddy.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "receipts")
data class ReceiptEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val vendor: String,
    val amount: String,
    val currency: String,
    val flightNumber: String?,
    val dateAdded: Long
)
