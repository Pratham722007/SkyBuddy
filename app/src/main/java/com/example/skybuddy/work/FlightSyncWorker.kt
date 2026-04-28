package com.example.skybuddy.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.skybuddy.domain.usecase.SyncFlightsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class FlightSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncFlights: SyncFlightsUseCase,
    private val notifications: NotificationHelper
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        val results = syncFlights()
        results.forEach { (old, new) ->
            if (new == null || new.lastSyncedAt <= old.lastSyncedAt) return@forEach

            if (old.gate != new.gate && new.gate != "TBD") {
                notifications.notifyFlightUpdate(
                    flightNumber = new.flightNumber,
                    title = "Gate Change: ${new.airline} ${new.flightNumber}",
                    message = "Your flight to ${new.destCity} is now boarding at Gate ${new.gate}."
                )
            }

            if (old.status != new.status && new.status.contains("Delayed", ignoreCase = true)) {
                notifications.notifyFlightUpdate(
                    flightNumber = new.flightNumber,
                    title = "Flight Delayed: ${new.flightNumber}",
                    message = "Your flight's status has changed to: ${new.status}. Estimated time: ${new.time}."
                )
            }
        }
        Result.success()
    } catch (t: Throwable) {
        Log.e(TAG, "Background sync failed", t)
        Result.retry()
    }

    companion object { private const val TAG = "FlightSyncWorker" }
}
