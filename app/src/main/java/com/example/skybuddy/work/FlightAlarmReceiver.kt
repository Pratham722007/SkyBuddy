package com.example.skybuddy.work

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FlightAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        val flightNumber = intent.getStringExtra(EXTRA_FLIGHT_NUMBER) ?: return
        Log.d("FlightAlarmReceiver", "Alarm fired for $flightNumber")
        
        notificationHelper.notifyFlightUpdate(
            flightNumber = flightNumber,
            title = "T-Minus 6 Hours!",
            message = "Time to pack and review your SkyBuddy checklist for flight $flightNumber."
        )
    }

    companion object {
        const val EXTRA_FLIGHT_NUMBER = "flight_number"
    }
}
