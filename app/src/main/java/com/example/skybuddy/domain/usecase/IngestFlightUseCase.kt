package com.example.skybuddy.domain.usecase

import android.content.Context
import android.net.Uri
import com.example.skybuddy.ai.LlmEngine
import com.example.skybuddy.core.time.Clock
import com.example.skybuddy.data.db.FlightEntity
import com.example.skybuddy.data.db.TrackingState
import com.example.skybuddy.data.repository.FlightRepository
import com.example.skybuddy.vision.MlKitBarcodeScanner
import com.example.skybuddy.work.AlarmScheduler
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IngestFlightUseCase @Inject constructor(
    private val barcodeScanner: MlKitBarcodeScanner,
    private val llmEngine: LlmEngine,
    private val flightRepository: FlightRepository,
    private val clock: Clock,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke(context: Context, imageUri: Uri): Result<FlightEntity> {
        return try {
            val rawBarcodeText = barcodeScanner.scanBarcode(context, imageUri)
                ?: return Result.failure(Exception("No barcode found in image"))
            processBarcodeText(rawBarcodeText)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend operator fun invoke(bitmap: android.graphics.Bitmap): Result<FlightEntity> {
        return try {
            val rawBarcodeText = barcodeScanner.scanBarcodeBitmap(bitmap)
                ?: return Result.failure(Exception("No barcode found in image"))
            processBarcodeText(rawBarcodeText)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun processBarcodeText(rawBarcodeText: String): Result<FlightEntity> {
        return try {
            val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
            // Use the LLM to format the barcode text into a JSON
            val prompt = """
                You are a data extraction tool. Extract the following details from this boarding pass barcode string and return ONLY a valid JSON object. 
                Do NOT use the example values. Replace them with the actual data extracted from the barcode text.
                Today's date is $currentDate. The barcode usually contains a Julian date (e.g., 124 means the 124th day of the year). Calculate the exact YYYY-MM-DD date using the current year ($currentYear). If the departure date has already passed for the current year (e.g. it's December and the flight is in January), assume it is for the next year.
                
                Expected JSON Format:
                {
                  "flightNumber": "extracted flight number (e.g. AI101)",
                  "airline": "extracted airline name",
                  "origin": "extracted origin IATA code",
                  "destination": "extracted destination IATA code",
                  "gate": "extracted gate (or TBD)",
                  "seat": "extracted seat (e.g. 12A)",
                  "date": "calculated YYYY-MM-DD date",
                  "time": "extracted departure time (HH:mm) or Unknown"
                }
                
                Barcode Text: $rawBarcodeText
            """.trimIndent()

            val llmResponse = llmEngine.generateText(prompt)
            
            // Try to extract JSON from the response (removing markdown if present)
            val jsonString = llmResponse.substringAfter("{").substringBeforeLast("}")
            val json = JSONObject("{$jsonString}")
            
            val flightNumber = json.optString("flightNumber", "UNKNOWN").uppercase().replace("\\s+".toRegex(), "").replace("/", "")
            val airline = json.optString("airline", "Unknown")
            val origin = json.optString("origin", "Unknown")
            val destination = json.optString("destination", "Unknown")
            val gate = json.optString("gate", "TBD")
            val seat = json.optString("seat", "TBD")
            val date = json.optString("date", "")
            val time = json.optString("time", "Unknown")
            
            val fullTime = if (date.isNotBlank() && time != "Unknown") "$date $time" else time

            val flight = FlightEntity(
                flightNumber = flightNumber,
                airline = airline,
                origin = origin,
                originCity = "Unknown",
                destination = destination,
                destCity = "Unknown",
                gate = gate,
                terminal = "TBD",
                status = "Scheduled",
                time = time,
                seat = seat,
                lastSyncedAt = clock.nowMillis(),
                departureTimeEpoch = parseDepartureEpoch(fullTime, clock.nowMillis()),
                trackingState = TrackingState.TRACKING.name
            )

            flightRepository.upsert(flight)
            alarmScheduler.schedulePreflightAlarm(flight.flightNumber, flight.departureTimeEpoch)
            Result.success(flight)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Parse departure time string into epoch millis. */
    private fun parseDepartureEpoch(depTime: String?, fallback: Long): Long {
        if (depTime.isNullOrBlank()) return fallback
        val patterns = listOf(
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm",
            "HH:mm"
        )
        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                val date = sdf.parse(depTime) ?: continue
                val epoch = date.time
                if (epoch < 86_400_000L) {
                    val now = java.util.Calendar.getInstance()
                    now.set(java.util.Calendar.HOUR_OF_DAY, date.hours)
                    now.set(java.util.Calendar.MINUTE, date.minutes)
                    now.set(java.util.Calendar.SECOND, 0)
                    now.set(java.util.Calendar.MILLISECOND, 0)
                    return now.timeInMillis
                }
                return epoch
            } catch (_: Exception) { /* try next */ }
        }
        return fallback
    }
}
