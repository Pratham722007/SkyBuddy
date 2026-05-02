package com.example.skybuddy.work

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedulePreflightAlarm(flightNumber: String, departureTimeEpoch: Long) {
        val intent = Intent(context, FlightAlarmReceiver::class.java).apply {
            putExtra(FlightAlarmReceiver.EXTRA_FLIGHT_NUMBER, flightNumber)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            flightNumber.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 6 hours before departure
        val alarmTime = departureTimeEpoch - (6 * 60 * 60 * 1000)

        // Only schedule if it's in the future
        if (alarmTime > System.currentTimeMillis()) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    alarmTime,
                    pendingIntent
                )
            } catch (e: SecurityException) {
                // Handle EXACT_ALARM permission missing on Android 12+ if necessary
                e.printStackTrace()
            }
        }
    }
}
