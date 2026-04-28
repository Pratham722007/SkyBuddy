package com.example.skybuddy.data.repository

import com.example.skybuddy.core.time.Clock
import com.example.skybuddy.data.db.ReceiptDao
import com.example.skybuddy.data.db.ReceiptEntity
import com.example.skybuddy.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReceiptRepository @Inject constructor(
    private val dao: ReceiptDao,
    private val clock: Clock,
    @IoDispatcher private val io: CoroutineDispatcher
) {
    fun observeAll(): Flow<List<ReceiptEntity>> = dao.observeAll()

    suspend fun all(): List<ReceiptEntity> = withContext(io) { dao.getAll() }

    suspend fun allForFlight(flightNumber: String): List<ReceiptEntity> = withContext(io) {
        dao.getAllForFlight(flightNumber.uppercase())
    }

    suspend fun save(vendor: String, amount: String, currency: String, flightNumber: String? = null) =
        withContext(io) {
            dao.insert(
                ReceiptEntity(
                    vendor = vendor,
                    amount = amount,
                    currency = currency,
                    flightNumber = flightNumber?.uppercase(),
                    dateAdded = clock.nowMillis()
                )
            )
        }

    suspend fun clear() = withContext(io) { dao.clear() }
}
