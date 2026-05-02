package com.example.skybuddy.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TimelineEventDao {
    @Query("SELECT * FROM timeline_events ORDER BY timestamp ASC")
    fun getAllEvents(): Flow<List<TimelineEventEntity>>
    
    @Query("SELECT * FROM timeline_events ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentEvents(limit: Int): List<TimelineEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: TimelineEventEntity)

    @Query("DELETE FROM timeline_events")
    suspend fun clearAll()
}
