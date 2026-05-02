package com.example.skybuddy.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        FlightEntity::class, 
        LuggageEntity::class, 
        ReceiptEntity::class,
        MapNodeEntity::class,
        TimelineEventEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun flightDao(): FlightDao
    abstract fun luggageDao(): LuggageDao
    abstract fun receiptDao(): ReceiptDao
    abstract fun mapNodeDao(): MapNodeDao
    abstract fun timelineEventDao(): TimelineEventDao

    companion object {
        const val NAME = "skybuddy_database"
    }
}
