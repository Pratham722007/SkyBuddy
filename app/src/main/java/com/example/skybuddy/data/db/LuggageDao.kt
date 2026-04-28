package com.example.skybuddy.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LuggageDao {
    @Query("SELECT * FROM luggage ORDER BY dateAdded DESC LIMIT 1")
    suspend fun getLatest(): LuggageEntity?

    @Query("SELECT * FROM luggage WHERE flightNumber = :flightNumber ORDER BY dateAdded DESC LIMIT 1")
    suspend fun getLatestForFlight(flightNumber: String): LuggageEntity?

    @Query("SELECT * FROM luggage ORDER BY dateAdded DESC")
    fun observeAll(): Flow<List<LuggageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(luggage: LuggageEntity)

    @Query("DELETE FROM luggage")
    suspend fun clear()
}
