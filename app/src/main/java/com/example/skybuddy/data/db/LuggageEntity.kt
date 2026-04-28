package com.example.skybuddy.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "luggage")
data class LuggageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val description: String,
    val flightNumber: String?,
    val dateAdded: Long
)
