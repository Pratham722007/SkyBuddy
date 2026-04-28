package com.example.skybuddy.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.absoluteValue

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val manager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init { ensureChannel() }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Flight Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Alerts for gate changes and delays" }
            manager.createNotificationChannel(channel)
        }
    }

    fun notifyFlightUpdate(flightNumber: String, title: String, message: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        // Per-flight stable id: subsequent updates for the same flight
        // replace the prior notification rather than stacking up.
        manager.notify(flightNumber.hashCode().absoluteValue, notification)
    }

    companion object { private const val CHANNEL_ID = "flight_alerts" }
}
