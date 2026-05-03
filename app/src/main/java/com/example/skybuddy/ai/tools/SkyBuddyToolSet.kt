package com.example.skybuddy.ai.tools

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

    @Tool(description = "Save a detailed visual description of the user's checked luggage. The active flight is known automatically. Use this when the user shows a picture of their bag and wants you to remember it.")
    fun saveBag(
        @ToolParam(description = "A detailed 1-2 sentence description of the bag based on the image provided.")
        description: String
    ): String = bridge {
        notifyTool("🧳 Saving bag description...")
        didQueryLuggage = true
        luggageRepository.save(description, activeFlightNumber)
        if (activeFlightNumber != null) "SUCCESS: Bag saved for flight $activeFlightNumber."
        else "SUCCESS: Bag saved."
    }

    @Tool(description = "Retrieve the saved visual description of the user's checked luggage for the active flight. Use this when the user asks what their bag looks like or says they lost their bag.")
    fun getBagDescription(): Map<String, String> = bridge {
        notifyTool("🧳 Looking up bag description...")
        didQueryLuggage = true
        val bag = activeFlightNumber?.let { luggageRepository.latestForFlight(it) }
            ?: luggageRepository.latest()
        if (bag != null) mapOf("description" to bag.description)
        else mapOf("error" to "No bag saved for this flight.")
    }

    @Tool(description = "Retrieve the saved expense receipts for the active flight. Use this when the user asks about their expenses for this trip.")
    fun getReceipts(): List<Map<String, String>> = bridge {
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

    @Tool(description = "Get real-time status, gate, terminal, time and seat for the active flight. The flight is known from the chat context.")
    fun getFlightStatus(): Map<String, Any> = bridge {
        notifyTool("✈️ Checking flight status...")
        val number = activeFlightNumber ?: return@bridge mapOf("error" to "No active flight in this chat.")
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
        ) else mapOf("error" to "Flight $number not found in database.")
    }

    @Tool(description = "Check if the user has lounge access in the active flight's terminal based on their credit card.")
    fun checkLoungeAccess(
        @ToolParam(description = "The credit card name, e.g. Amex Platinum or Chase Sapphire")
        creditCard: String
    ): String = bridge {
        notifyTool("🏙️ Checking lounge access...")
        val terminal = activeFlightNumber
            ?.let { flightRepository.getFlight(it) }
            ?.terminal
            ?: "your terminal"
        when {
            creditCard.contains("Amex", ignoreCase = true) ||
                creditCard.contains("Platinum", ignoreCase = true) ||
                creditCard.contains("Centurion", ignoreCase = true) ->
                "Yes, you have access to the Centurion Lounge in $terminal. It is a 5-minute walk from the central concourse."
            creditCard.contains("Chase", ignoreCase = true) ||
                creditCard.contains("Sapphire", ignoreCase = true) ||
                creditCard.contains("Priority Pass", ignoreCase = true) ->
                "Yes, you have access to the Sapphire Lounge in $terminal. It is located near the food court."
            else -> "No, your $creditCard does not typically provide lounge access in $terminal."
        }
    }

    @Tool(description = "Check seat details and comfort information for the user's seat on the active flight. Reads the seat from the active flight context.")
    fun checkSeatDetails(): String = bridge {
        notifyTool("💺 Looking up seat details...")
        val number = activeFlightNumber ?: return@bridge "No active flight."
        val seat = flightRepository.getFlight(number)?.seat
        if (seat.isNullOrBlank() || seat.equals("Unknown", true)) {
            "Seat is not yet known. Ask the user to upload a boarding pass photo or tell their seat number, then call setMySeat."
        } else describeSeat(seat)
    }

    @Tool(description = "Save the user's seat number on the active flight. Use this after the user tells you their seat or after parsing it from a boarding pass image.")
    fun setMySeat(
        @ToolParam(description = "The seat number, e.g. 12A, 4C, 27F.")
        seatNumber: String
    ): String = bridge {
        notifyTool("💺 Saving seat number...")
        val number = activeFlightNumber ?: return@bridge "No active flight to update."
        val seat = seatNumber.trim().uppercase()
        if (!seat.matches(Regex("^\\d{1,3}[A-K]$"))) return@bridge "Seat '$seatNumber' does not look like a valid seat number."
        flightRepository.updateSeat(number, seat)
        didTouchFlight = true
        "SUCCESS: Seat $seat saved for flight $number."
    }

    @Tool(description = "Save an expense receipt for the active flight. Use this when the user provides an image or details of a receipt.")
    fun saveReceipt(
        @ToolParam(description = "The vendor or restaurant name on the receipt.") vendor: String,
        @ToolParam(description = "The total amount paid.") amount: String,
        @ToolParam(description = "The currency of the transaction, e.g. USD or EUR.") currency: String
    ): String = bridge {
        notifyTool("🧾 Saving receipt from $vendor...")
        didQueryReceipts = true
        receiptRepository.save(vendor, amount, currency, activeFlightNumber)
        if (activeFlightNumber != null) "SUCCESS: Receipt saved for flight $activeFlightNumber."
        else "SUCCESS: Receipt saved."
    }

    @Tool(
        description = "Fuzzy-searches the Kempegowda International Airport (BLR) knowledge base. " +
            "Returns restaurants, cafes, shops, lounges, services, menus, timings, gate proximity, and location data. " +
            "Call this whenever the user asks ANYTHING about food, drinks, shopping, facilities, " +
            "gate proximity, open hours, specific menu items, or prices at Bangalore airport."
    )
    fun searchAirportKb(
        @ToolParam(description = "Natural-language query, e.g. 'South Indian breakfast near Gate 12', 'open coffee shop', 'veg food after security T2'")
        query: String
    ): String = bridge {
        notifyTool("🔍 Searching airport knowledge base...")
        try {
            airportKb.search(query = query, topK = 5)
        } catch (e: Exception) {
            android.util.Log.e("SkyBuddy", "Error in searchAirportKb: ", e)
            "{\"error\": \"Airport KB search failed: ${e.message}\", \"query\": \"$query\", \"pois\": [], \"services\": [], \"totalMatches\": 0}"
        }
    }


    private fun describeSeat(seat: String): String = when {
        seat.endsWith("A") || seat.endsWith("F") ->
            "Seat $seat is a window seat."
        seat.endsWith("C") || seat.endsWith("D") ->
            "Seat $seat is an aisle seat."
        else -> "Seat $seat is a middle seat."
    }

    private fun <T> bridge(block: suspend () -> T): T = runBlocking {
        withTimeout(TOOL_TIMEOUT_MS) {
            withContext(Dispatchers.IO) { block() }
        }
    }

    companion object {
        private const val TOOL_TIMEOUT_MS = 5_000L
    }
}
