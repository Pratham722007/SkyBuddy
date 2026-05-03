package com.example.skybuddy.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FlightDao {
    @Query("SELECT * FROM flights WHERE flightNumber = :flightNumber LIMIT 1")
    suspend fun getFlight(flightNumber: String): FlightEntity?

    @Query("SELECT * FROM flights WHERE flightNumber = :flightNumber LIMIT 1")
    fun observeFlight(flightNumber: String): Flow<FlightEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(flight: FlightEntity)

    @Query("SELECT * FROM flights")
    suspend fun getAll(): List<FlightEntity>

    @Query("SELECT * FROM flights WHERE trackingState = 'TRACKING' ORDER BY departureTimeEpoch ASC")
    fun observeUpcoming(): Flow<List<FlightEntity>>

    @Query("SELECT * FROM flights WHERE trackingState = 'COMPLETED' ORDER BY departureTimeEpoch DESC LIMIT 3")
    fun observePast(): Flow<List<FlightEntity>>

    @Query("SELECT * FROM flights WHERE trackingState = 'TRACKING'")
    suspend fun getTracked(): List<FlightEntity>

    @Query("UPDATE flights SET seat = :seat WHERE flightNumber = :flightNumber")
    suspend fun updateSeat(flightNumber: String, seat: String)

    @Query("DELETE FROM flights WHERE flightNumber = :flightNumber")
    suspend fun delete(flightNumber: String)

    @Query("UPDATE flights SET trackingState = 'COMPLETED' WHERE trackingState = 'TRACKING' AND departureTimeEpoch > 0 AND departureTimeEpoch < :cutoffMillis")
    suspend fun completePastFlights(cutoffMillis: Long)
}
