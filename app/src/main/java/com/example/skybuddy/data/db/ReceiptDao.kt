package com.example.skybuddy.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceiptDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(receipt: ReceiptEntity)

    @Query("SELECT * FROM receipts ORDER BY dateAdded DESC")
    suspend fun getAll(): List<ReceiptEntity>

    @Query("SELECT * FROM receipts WHERE flightNumber = :flightNumber ORDER BY dateAdded DESC")
    suspend fun getAllForFlight(flightNumber: String): List<ReceiptEntity>

    @Query("SELECT * FROM receipts ORDER BY dateAdded DESC")
    fun observeAll(): Flow<List<ReceiptEntity>>

    @Query("DELETE FROM receipts")
    suspend fun clear()
}
