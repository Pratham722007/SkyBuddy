package com.example.skybuddy.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TrackingState { TRACKING, COMPLETED }

@Entity(tableName = "flights")
data class FlightEntity(
    @PrimaryKey val flightNumber: String,
    val airline: String,
    val origin: String,
    val originCity: String,
    val destination: String,
    val destCity: String,
    val gate: String,
    val terminal: String,
    val status: String,
    val time: String,
    val seat: String,
    val lastSyncedAt: Long,
    val departureTimeEpoch: Long,
    val trackingState: String
)
