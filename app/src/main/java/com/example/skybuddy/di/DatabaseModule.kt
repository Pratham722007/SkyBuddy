package com.example.skybuddy.di

import android.content.Context
import androidx.room.Room
import com.example.skybuddy.data.db.AppDatabase
import com.example.skybuddy.data.db.FlightDao
import com.example.skybuddy.data.db.LuggageDao
import com.example.skybuddy.data.db.MapNodeDao
import com.example.skybuddy.data.db.ReceiptDao
import com.example.skybuddy.data.db.TimelineEventDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            // Schema v5 supersedes the pre-rewrite schemas (1–4). Old DBs only held
            // re-fetchable flight/luggage/receipt rows — safe to drop. The model file
            // lives in filesDir and is not affected.
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideFlightDao(db: AppDatabase): FlightDao = db.flightDao()
    @Provides fun provideLuggageDao(db: AppDatabase): LuggageDao = db.luggageDao()
    @Provides fun provideReceiptDao(db: AppDatabase): ReceiptDao = db.receiptDao()
    @Provides fun provideMapNodeDao(db: AppDatabase): MapNodeDao = db.mapNodeDao()
    @Provides fun provideTimelineEventDao(db: AppDatabase): TimelineEventDao = db.timelineEventDao()
}
