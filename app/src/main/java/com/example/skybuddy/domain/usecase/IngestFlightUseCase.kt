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
            // Use the LLM to format the barcode text into a JSON
            val prompt = """
                Extract the following details from this boarding pass barcode string and return ONLY a valid JSON object. 
                Do not include markdown or extra text.
                Format:
                {
                  "flightNumber": "XX123",
                  "airline": "Airline Name",
                  "origin": "ABC",
                  "destination": "XYZ",
                  "gate": "A1",
                  "seat": "12A",
                  "time": "14:30"
                }
                
                Barcode Text: $rawBarcodeText
            """.trimIndent()

            val llmResponse = llmEngine.generateText(prompt)
            
            // Try to extract JSON from the response (removing markdown if present)
            val jsonString = llmResponse.substringAfter("{").substringBeforeLast("}")
            val json = JSONObject("{$jsonString}")
            
            val flightNumber = json.optString("flightNumber", "UNKNOWN").uppercase()
            val airline = json.optString("airline", "Unknown")
            val origin = json.optString("origin", "Unknown")
            val destination = json.optString("destination", "Unknown")
            val gate = json.optString("gate", "TBD")
            val seat = json.optString("seat", "TBD")
            val time = json.optString("time", "Unknown")

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
                departureTimeEpoch = clock.nowMillis() + (24 * 60 * 60 * 1000), // Default to 24h later
                trackingState = TrackingState.TRACKING.name
            )

            flightRepository.upsert(flight)
            alarmScheduler.schedulePreflightAlarm(flight.flightNumber, flight.departureTimeEpoch)
            Result.success(flight)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
