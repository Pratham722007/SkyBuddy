package com.example.skybuddy.data.repository

import com.example.skybuddy.core.time.Clock
import com.example.skybuddy.data.db.LuggageDao
import com.example.skybuddy.data.db.LuggageEntity
import com.example.skybuddy.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LuggageRepository @Inject constructor(
    private val dao: LuggageDao,
    private val clock: Clock,
    @IoDispatcher private val io: CoroutineDispatcher
) {
    fun observeAll(): Flow<List<LuggageEntity>> = dao.observeAll()

    suspend fun latest(): LuggageEntity? = withContext(io) { dao.getLatest() }

    suspend fun latestForFlight(flightNumber: String): LuggageEntity? = withContext(io) {
        dao.getLatestForFlight(flightNumber.uppercase())
    }

    suspend fun save(description: String, flightNumber: String? = null) = withContext(io) {
        dao.insert(
            LuggageEntity(
                description = description,
                flightNumber = flightNumber?.uppercase(),
                dateAdded = clock.nowMillis()
            )
        )
    }

    suspend fun clear() = withContext(io) { dao.clear() }
}
