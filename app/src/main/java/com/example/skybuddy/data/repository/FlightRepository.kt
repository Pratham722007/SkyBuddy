package com.example.skybuddy.data.repository

import android.util.Log
import com.example.skybuddy.BuildConfig
import com.example.skybuddy.core.result.AppResult
import com.example.skybuddy.core.result.ErrorReason
import com.example.skybuddy.core.time.Clock
import com.example.skybuddy.data.db.FlightDao
import com.example.skybuddy.data.db.FlightEntity
import com.example.skybuddy.data.db.TrackingState
import com.example.skybuddy.data.network.NetworkMonitor
import com.example.skybuddy.data.network.airlabs.AirLabsApi
import com.example.skybuddy.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlightRepository @Inject constructor(
    private val flightDao: FlightDao,
    private val airLabsApi: AirLabsApi,
    private val networkMonitor: NetworkMonitor,
    private val clock: Clock,
    @IoDispatcher private val io: CoroutineDispatcher
) {
    fun observeUpcoming(): Flow<List<FlightEntity>> = flightDao.observeUpcoming()
    fun observePast(): Flow<List<FlightEntity>> = flightDao.observePast()
    fun observeFlight(flightNumber: String): Flow<FlightEntity?> = flightDao.observeFlight(flightNumber.uppercase())

    suspend fun getFlight(flightNumber: String): FlightEntity? = withContext(io) {
        flightDao.getFlight(flightNumber.uppercase())
    }

    suspend fun getAll(): List<FlightEntity> = withContext(io) { flightDao.getAll() }

    suspend fun updateSeat(flightNumber: String, seat: String) = withContext(io) {
        flightDao.updateSeat(flightNumber.uppercase(), seat)
    }

    suspend fun refresh(flightNumber: String): AppResult<FlightEntity> = withContext(io) {
        val number = flightNumber.uppercase()
        val apiKey = BuildConfig.AIRLABS_API_KEY
        if (apiKey.isBlank() || apiKey == "YOUR_AIRLABS_KEY") {
            val cached = flightDao.getFlight(number)
            return@withContext if (cached != null) AppResult.Success(cached)
            else AppResult.Error(ErrorReason.MissingApiKey)
        }
        if (!networkMonitor.isOnline()) {
            val cached = flightDao.getFlight(number)
            return@withContext if (cached != null) AppResult.Success(cached)
            else AppResult.Error(ErrorReason.Offline)
        }
        try {
            val response = airLabsApi.getFlightSchedule(apiKey, number)
            val schedules = response.response
            if (schedules.isNullOrEmpty()) {
                val cached = flightDao.getFlight(number)
                return@withContext if (cached != null) AppResult.Success(cached)
                else AppResult.Error(ErrorReason.NotFound)
            }
            val schedule = schedules.first()
            val statusStr = schedule.status?.replaceFirstChar { it.uppercase() } ?: "Scheduled"
            val isCompleted = statusStr.equals("Landed", true) ||
                statusStr.equals("Arrived", true) ||
                statusStr.equals("Cancelled", true)
            val state = if (isCompleted) TrackingState.COMPLETED else TrackingState.TRACKING

            val flight = FlightEntity(
                flightNumber = number,
                airline = parseAirline(number),
                origin = schedule.depIata ?: "Unknown",
                originCity = "Unknown",
                destination = schedule.arrIata ?: "Unknown",
                destCity = "Unknown",
                gate = schedule.depGate ?: "TBD",
                terminal = schedule.depTerminal ?: "TBD",
                status = statusStr,
                time = schedule.depTime ?: "Unknown",
                seat = "Unknown",
                lastSyncedAt = clock.nowMillis(),
                departureTimeEpoch = clock.nowMillis(),
                trackingState = state.name
            )
            flightDao.upsert(flight)
            AppResult.Success(flight)
        } catch (e: Exception) {
            Log.e(TAG, "AirLabs sync failed for $number", e)
            val cached = flightDao.getFlight(number)
            if (cached != null) AppResult.Success(cached)
            else AppResult.Error(ErrorReason.Network(e.message ?: "Unknown network error"), e)
        }
    }

    suspend fun refreshAllTracked(): List<Pair<FlightEntity, FlightEntity?>> = withContext(io) {
        val tracked = flightDao.getTracked()
        coroutineScope {
            tracked.map { old ->
                async {
                    val result = refresh(old.flightNumber)
                    val updated = (result as? AppResult.Success<FlightEntity>)?.value
                    old to updated
                }
            }.awaitAll()
        }
    }

    private fun parseAirline(flightNumber: String): String = when {
        flightNumber.startsWith("AA", true) -> "American Airlines"
        flightNumber.startsWith("DL", true) -> "Delta Air Lines"
        flightNumber.startsWith("UA", true) -> "United Airlines"
        flightNumber.startsWith("WN", true) -> "Southwest Airlines"
        flightNumber.startsWith("BA", true) -> "British Airways"
        flightNumber.startsWith("LH", true) -> "Lufthansa"
        flightNumber.startsWith("AI", true) -> "Air India"
        flightNumber.startsWith("6E", true) -> "IndiGo"
        flightNumber.startsWith("UK", true) -> "Vistara"
        flightNumber.startsWith("SG", true) -> "SpiceJet"
        flightNumber.startsWith("QP", true) -> "Akasa Air"
        flightNumber.startsWith("I5", true) -> "AIX Connect"
        else -> flightNumber.take(2).uppercase()
    }

    companion object { private const val TAG = "FlightRepository" }
}
