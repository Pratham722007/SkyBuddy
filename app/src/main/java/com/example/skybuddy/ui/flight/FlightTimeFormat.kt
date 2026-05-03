package com.example.skybuddy.ui.flight

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatFlightTime(raw: String): String {
    if (raw.isBlank()) return "--:--"
    return try {
        val parsers = listOf(
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd'T'HH:mm:ss",
            "HH:mm"
        )
        for (pattern in parsers) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                val date = sdf.parse(raw) ?: continue
                return SimpleDateFormat("HH:mm", Locale.US).format(date)
            } catch (_: Exception) { /* try next */ }
        }
        raw
    } catch (_: Exception) { raw }
}

/** Full date + time display: "May 3, 11:32" */
fun formatFlightDate(epochMillis: Long): String {
    if (epochMillis <= 0) return ""
    return SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date(epochMillis))
}

/** Date-only display for compact chips: "May 3" */
fun formatFlightDateShort(epochMillis: Long): String {
    if (epochMillis <= 0) return ""
    return SimpleDateFormat("MMM d", Locale.US).format(Date(epochMillis))
}

/** Time-only from epoch: "05:35" */
fun formatFlightTimeFromEpoch(epochMillis: Long): String {
    if (epochMillis <= 0) return "--:--"
    return SimpleDateFormat("HH:mm", Locale.US).format(Date(epochMillis))
}
