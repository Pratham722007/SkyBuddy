package com.example.skybuddy.domain.usecase

import android.graphics.Bitmap
import com.example.skybuddy.ai.LlmEngine
import com.example.skybuddy.ai.tools.SkyBuddyToolSet
import com.example.skybuddy.data.db.FlightEntity
import com.example.skybuddy.data.db.LuggageEntity
import com.example.skybuddy.data.db.ReceiptEntity
import com.example.skybuddy.data.repository.FlightRepository
import com.example.skybuddy.data.repository.LuggageRepository
import com.example.skybuddy.data.repository.ReceiptRepository
import javax.inject.Inject

data class ChatTurnResult(
    val response: String,
    val flight: FlightEntity? = null,
    val luggage: LuggageEntity? = null,
    val receipts: List<ReceiptEntity>? = null
)

class ChatTurnUseCase @Inject constructor(
    private val llm: LlmEngine,
    private val tools: SkyBuddyToolSet,
    private val flightRepository: FlightRepository,
    private val luggageRepository: LuggageRepository,
    private val receiptRepository: ReceiptRepository
) {
    suspend fun text(prompt: String, activeFlightNumber: String?): ChatTurnResult {
        tools.resetTracking()
        tools.setActiveFlight(activeFlightNumber)
        val wrapped = withFlightContext(prompt, activeFlightNumber)
        val response = llm.generateText(wrapped)
        return collectSideEffects(response, activeFlightNumber)
    }

    suspend fun image(prompt: String, bitmap: Bitmap, activeFlightNumber: String?): ChatTurnResult {
        tools.resetTracking()
        tools.setActiveFlight(activeFlightNumber)
        val wrapped = withFlightContext(prompt, activeFlightNumber)
        val response = llm.generateMultimodal(wrapped, bitmap)
        return collectSideEffects(response, activeFlightNumber)
    }

    private suspend fun withFlightContext(prompt: String, flightNumber: String?): String {
        val systemPrompt = buildString {
            appendLine("[SYSTEM: You are SkyBuddy, a helpful airport travel assistant specializing in Surat Airport (STV). Be concise, friendly, and precise. Never use emojis.]")
        }
        if (flightNumber.isNullOrBlank()) return "$systemPrompt\n$prompt"
        val flight = flightRepository.getFlight(flightNumber) ?: return "$systemPrompt\n$prompt"
        val seatKnown = flight.seat.isNotBlank() && !flight.seat.equals("Unknown", true)
        val seatNote = if (seatKnown) "seat ${flight.seat}"
        else "seat is UNKNOWN — politely ask the user to upload a boarding pass photo or tell you their seat, then call setMySeat to save it"
        val flightContext = buildString {
            append("[Active flight: ${flight.flightNumber}")
            append(", ${flight.airline}")
            append(", ${flight.origin} -> ${flight.destination}")
            if (flight.originCity.isNotBlank() && !flight.originCity.equals("Unknown", true)) {
                append(" (${flight.originCity} -> ${flight.destCity})")
            }
            append(", departure: ${flight.time}")
            append(", gate: ${flight.gate}")
            append(", terminal: ${flight.terminal}")
            append(", status: ${flight.status}")
            append(", $seatNote")
            append("]")
        }
        return "$systemPrompt\n$flightContext\n$prompt"
    }

    private suspend fun collectSideEffects(response: String, activeFlightNumber: String?): ChatTurnResult {
        val receipts = if (tools.didQueryReceipts) {
            val rows = activeFlightNumber?.let { receiptRepository.allForFlight(it) }
                ?: receiptRepository.all()
            rows.takeIf { it.isNotEmpty() }
        } else null

        val luggage = if (tools.didQueryLuggage) {
            activeFlightNumber?.let { luggageRepository.latestForFlight(it) }
                ?: luggageRepository.latest()
        } else null

        val flight = activeFlightNumber?.takeIf { tools.didTouchFlight }
            ?.let { flightRepository.getFlight(it) }

        return ChatTurnResult(
            response = response,
            flight = flight,
            luggage = luggage,
            receipts = receipts
        )
    }
}
