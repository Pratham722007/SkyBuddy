package com.example.skybuddy.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "timeline_events")
data class TimelineEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val role: String, // "USER", "GEMMA", "SYSTEM_SPATIAL"
    val uiComponentType: String, // "TEXT", "RECEIPT_CARD", "MAP_TOAST", "LUGGAGE_CARD", "FLIGHT_CARD"
    val content: String, // Raw text, or structured JSON payload for interactive UI cards
    val localImageUri: String? = null // If the user uploaded a photo for visual help
)
