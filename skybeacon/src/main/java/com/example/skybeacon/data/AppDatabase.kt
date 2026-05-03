package com.example.skybeacon.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ShopProfile::class, BroadcastMessage::class, Offer::class, BroadcastLog::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun shopProfileDao(): ShopProfileDao
    abstract fun broadcastMessageDao(): BroadcastMessageDao
    abstract fun offerDao(): OfferDao
    abstract fun broadcastLogDao(): BroadcastLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "skybeacon_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
