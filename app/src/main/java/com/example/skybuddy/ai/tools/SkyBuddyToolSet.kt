package com.example.skybuddy.ai.tools

import androidx.annotation.Keep
import com.example.skybuddy.data.repository.FlightRepository
import com.example.skybuddy.data.repository.LuggageRepository
import com.example.skybuddy.data.repository.ReceiptRepository
import com.google.ai.edge.litertlm.Tool
import com.google.ai.edge.litertlm.ToolParam
import com.google.ai.edge.litertlm.ToolSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Keep
@Singleton
class SkyBuddyToolSet @Inject constructor(
    private val flightRepository: FlightRepository,
    private val luggageRepository: LuggageRepository,
    private val receiptRepository: ReceiptRepository,
    private val airportKb: AirportKnowledgeBaseTool
) : ToolSet {

    private val activeFlight = AtomicReference<String?>(null)

    // Set by ChatViewModel before each turn; fires a UI status update when a tool starts
    var onToolStarted: ((label: String) -> Unit)? = null

    @Volatile var didQueryReceipts: Boolean = false
        private set

    @Volatile var didQueryLuggage: Boolean = false
        private set

    @Volatile var didTouchFlight: Boolean = false
        private set

    fun setActiveFlight(flightNumber: String?) {
        activeFlight.set(flightNumber?.uppercase())
    }

    val activeFlightNumber: String? get() = activeFlight.get()

    fun resetTracking() {
        didQueryReceipts = false
        didQueryLuggage = false
        didTouchFlight = false
        onToolStarted = null
    }

    /** Fires the UI callback with a human-readable status label. */
    private fun notifyTool(label: String) = onToolStarted?.invoke(label)

    @Keep
    @Tool(description = "Search the Bangalore Airport (BLR) database for food, shops, and services. Use this for ANY question about what is available at the airport.")
    fun search(
        @ToolParam(description = "The search query, e.g. 'coffee' or 'pharmacy'.")
        query: String
    ): String = bridge {
        android.util.Log.d("SkyBuddy", "Tool: searchAirport($query)")
        notifyTool("🔍 Searching airport database...")
        try {
            val res = airportKb.search(query = query, topK = 5)
            android.util.Log.d("SkyBuddy", "Tool: searchAirport result length: ${res.length}")
            res
        } catch (e: Exception) {
            android.util.Log.e("SkyBuddy", "Error in searchAirport: ", e)
            "{\"error\": \"Search failed\", \"pois\": []}"
        }
    }

    @Keep
    @Tool(description = "Save a detailed visual description of the user's checked luggage.")
    fun saveBag(
        @ToolParam(description = "A detailed description of the bag.")
        description: String
    ): String = bridge {
        android.util.Log.d("SkyBuddy", "Tool: saveBag")
        notifyTool("🧳 Saving bag description...")
        didQueryLuggage = true
        luggageRepository.save(description, activeFlightNumber)
        if (activeFlightNumber != null) "SUCCESS: Bag saved for flight $activeFlightNumber."
        else "SUCCESS: Bag saved."
    }

    @Keep
    @Tool(description = "Retrieve the saved visual description of the user's checked luggage.")
    fun getBagDescription(): Map<String, String> = bridge {
        android.util.Log.d("SkyBuddy", "Tool: getBagDescription")
        notifyTool("🧳 Looking up bag description...")
        didQueryLuggage = true
        val bag = activeFlightNumber?.let { luggageRepository.latestForFlight(it) }
            ?: luggageRepository.latest()
        if (bag != null) mapOf("description" to bag.description)
        else mapOf("error" to "No bag saved.")
    }

    @Keep
    @Tool(description = "Retrieve the saved expense receipts for the active flight.")
    fun getReceipts(): List<Map<String, String>> = bridge {
        android.util.Log.d("SkyBuddy", "Tool: getReceipts")
        notifyTool("🧾 Fetching expense receipts...")
        didQueryReceipts = true
        val rows = activeFlightNumber?.let { receiptRepository.allForFlight(it) }
            ?: receiptRepository.all()
        rows.map {
            mapOf(
                "vendor" to it.vendor,
                "amount" to it.amount,
                "currency" to it.currency
            )
        }
    }

    @Keep
    @Tool(description = "Get real-time status, gate, terminal, and seat for the active flight.")
    fun getFlightStatus(): Map<String, Any> = bridge {
        android.util.Log.d("SkyBuddy", "Tool: getFlightStatus")
        notifyTool("✈️ Checking flight status...")
        val number = activeFlightNumber ?: return@bridge mapOf("error" to "No active flight.")
        didTouchFlight = true
        val entity = flightRepository.getFlight(number)
        if (entity != null) mapOf(
            "flightNumber" to entity.flightNumber,
            "status" to entity.status,
            "gate" to entity.gate,
            "terminal" to entity.terminal,
            "time" to entity.time,
            "airline" to entity.airline,
            "seat" to entity.seat
        ) else mapOf("error" to "Flight not found.")
    }

    @Keep
    @Tool(description = "Check if the user has lounge access based on their credit card.")
    fun checkLoungeAccess(
        @ToolParam(description = "The credit card name.")
        creditCard: String
    ): String = bridge {
        android.util.Log.d("SkyBuddy", "Tool: checkLoungeAccess($creditCard)")
        notifyTool("🏙️ Checking lounge access...")
        val terminal = activeFlightNumber
            ?.let { flightRepository.getFlight(it) }
            ?.terminal
            ?: "your terminal"
        when {
            creditCard.contains("Amex", ignoreCase = true) ||
                creditCard.contains("Platinum", ignoreCase = true) ||
                creditCard.contains("Centurion", ignoreCase = true) ->
                "Yes, you have access to the Centurion Lounge in $terminal."
            creditCard.contains("Chase", ignoreCase = true) ||
                creditCard.contains("Sapphire", ignoreCase = true) ||
                creditCard.contains("Priority Pass", ignoreCase = true) ->
                "Yes, you have access to the Sapphire Lounge in $terminal."
            else -> "No, $creditCard does not typically provide access in $terminal."
        }
    }

    @Keep
    @Tool(description = "Check seat details and comfort information for the user's seat.")
    fun checkSeatDetails(): String = bridge {
        android.util.Log.d("SkyBuddy", "Tool: checkSeatDetails")
        notifyTool("💺 Looking up seat details...")
        val number = activeFlightNumber ?: return@bridge "No active flight."
        val seat = flightRepository.getFlight(number)?.seat
        if (seat.isNullOrBlank() || seat.equals("Unknown", true)) {
            "Seat is not yet known. Call setMySeat to save it."
        } else describeSeat(seat)
    }

    @Keep
    @Tool(description = "Save the user's seat number on the active flight.")
    fun setMySeat(
        @ToolParam(description = "The seat number, e.g. 12A.")
        seatNumber: String
    ): String = bridge {
        android.util.Log.d("SkyBuddy", "Tool: setMySeat($seatNumber)")
        notifyTool("💺 Saving seat number...")
        val number = activeFlightNumber ?: return@bridge "No active flight."
        val seat = seatNumber.trim().uppercase()
        flightRepository.updateSeat(number, seat)
        didTouchFlight = true
        "SUCCESS: Seat saved."
    }

    @Keep
    @Tool(description = "Save an expense receipt for the active flight.")
    fun saveReceipt(
        @ToolParam(description = "The vendor name.") vendor: String,
        @ToolParam(description = "The amount.") amount: String,
        @ToolParam(description = "The currency.") currency: String
    ): String = bridge {
        android.util.Log.d("SkyBuddy", "Tool: saveReceipt($vendor, $amount, $currency)")
        notifyTool("🧾 Saving receipt from $vendor...")
        didQueryReceipts = true
        receiptRepository.save(vendor, amount, currency, activeFlightNumber)
        "SUCCESS: Receipt saved."
    }

    private fun describeSeat(seat: String): String = when {
        seat.endsWith("A") || seat.endsWith("F") ->
            "Seat $seat is a window seat."
        seat.endsWith("C") || seat.endsWith("D") ->
            "Seat $seat is an aisle seat."
        else -> "Seat $seat is a middle seat."
    }

    private fun <T> bridge(block: suspend () -> T): T = runBlocking {
        android.util.Log.d("SkyBuddy", "Tool bridge: entering")
        try {
            withTimeout(TOOL_TIMEOUT_MS) {
                withContext(Dispatchers.IO) { block() }
            }
        } finally {
            android.util.Log.d("SkyBuddy", "Tool bridge: exiting")
        }
    }

    companion object {
        private const val TOOL_TIMEOUT_MS = 15_000L
    }
}
