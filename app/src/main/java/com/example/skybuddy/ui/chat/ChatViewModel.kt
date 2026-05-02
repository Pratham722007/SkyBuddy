package com.example.skybuddy.ui.chat

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skybuddy.data.db.FlightEntity
import com.example.skybuddy.data.db.LuggageEntity
import com.example.skybuddy.data.db.ReceiptEntity
import com.example.skybuddy.data.db.TimelineEventDao
import com.example.skybuddy.data.db.TimelineEventEntity
import com.example.skybuddy.data.repository.FlightRepository
import com.example.skybuddy.domain.state.JourneyManager
import com.example.skybuddy.domain.usecase.ChatTurnUseCase
import com.example.skybuddy.domain.usecase.DescribeLuggageUseCase
import com.example.skybuddy.domain.usecase.RecognizeReceiptUseCase
import com.example.skybuddy.location.IndoorLocationManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val input: String = "",
    val isThinking: Boolean = false,
    val isIntercomMode: Boolean = false
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val flightRepository: FlightRepository,
    private val timelineEventDao: TimelineEventDao,
    private val chatTurn: ChatTurnUseCase,
    private val recognizeReceipt: RecognizeReceiptUseCase,
    private val describeLuggage: DescribeLuggageUseCase,
    private val journeyManager: JourneyManager,
    private val indoorLocationManager: IndoorLocationManager,
    val voiceController: VoiceController
) : ViewModel() {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    val timelineEvents: StateFlow<List<TimelineEventEntity>> = timelineEventDao.getAllEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var pinnedFlightNumber: String? = null

    private val _pinnedFlight = MutableStateFlow<FlightEntity?>(null)
    val pinnedFlight: StateFlow<FlightEntity?> = _pinnedFlight.asStateFlow()

    fun setFlightContext(flightNumber: String?) {
        if (flightNumber.isNullOrBlank() || flightNumber == "help" || flightNumber == "timeline") return
        if (flightNumber == pinnedFlightNumber) return
        pinnedFlightNumber = flightNumber
        viewModelScope.launch {
            flightRepository.observeFlight(flightNumber).collect { flight ->
                _pinnedFlight.value = flight
            }
        }
    }

    fun onInputChanged(value: String) = _state.update { it.copy(input = value) }

    fun toggleIntercom() = _state.update { it.copy(isIntercomMode = !it.isIntercomMode) }
    
    private fun getSpatialContext(): String {
        val x = indoorLocationManager.currentX.value
        val y = indoorLocationManager.currentY.value
        val phase = journeyManager.currentPhase.value.displayName
        return "[SYSTEM CONTEXT: User is at X:$x, Y:$y. State is $phase.]"
    }

    fun sendText(hiddenContext: String? = null): String? {
        val prompt = _state.value.input.trim()
        if (prompt.isBlank() || _state.value.isThinking) return null
        
        viewModelScope.launch {
            timelineEventDao.insert(TimelineEventEntity(
                timestamp = System.currentTimeMillis(),
                role = "USER",
                uiComponentType = "TEXT",
                content = prompt
            ))
            
            _state.update { it.copy(input = "", isThinking = true) }
            
            val spatialContext = getSpatialContext()
            val finalContext = if (hiddenContext != null) "$spatialContext\n$hiddenContext" else spatialContext
            val query = "$finalContext\n$prompt"
            val result = chatTurn.text(query, pinnedFlightNumber)
            
            applyTurn(result.response, result.flight, result.luggage, result.receipts)
        }
        return prompt
    }

    fun sendImage(prompt: String, bitmap: Bitmap, hiddenContext: String? = null) {
        if (_state.value.isThinking) return
        
        val actualPrompt = prompt.ifBlank { "Image" }
        
        viewModelScope.launch {
            timelineEventDao.insert(TimelineEventEntity(
                timestamp = System.currentTimeMillis(),
                role = "USER",
                uiComponentType = "TEXT",
                content = actualPrompt
            ))
            
            _state.update { it.copy(input = "", isThinking = true) }
            
            val spatialContext = getSpatialContext()
            val finalContext = if (hiddenContext != null) "$spatialContext\n$hiddenContext" else spatialContext
            val query = "$finalContext\n$actualPrompt"
            val result = chatTurn.image(query, bitmap, pinnedFlightNumber)
            
            applyTurn(result.response, result.flight, result.luggage, result.receipts)
        }
    }

    fun captureReceipt(bitmap: Bitmap) {
        viewModelScope.launch {
            val text = recognizeReceipt(bitmap).ifBlank { "No text detected on receipt" }
            timelineEventDao.insert(TimelineEventEntity(
                timestamp = System.currentTimeMillis(),
                role = "GEMMA",
                uiComponentType = "TEXT",
                content = "Receipt: $text"
            ))
        }
    }

    fun captureLuggage(bitmap: Bitmap) {
        viewModelScope.launch {
            val description = describeLuggage(bitmap)
            timelineEventDao.insert(TimelineEventEntity(
                timestamp = System.currentTimeMillis(),
                role = "GEMMA",
                uiComponentType = "TEXT",
                content = "Saved bag: $description"
            ))
        }
    }

    private suspend fun applyTurn(
        response: String,
        flight: FlightEntity?,
        luggage: LuggageEntity?,
        receipts: List<ReceiptEntity>?
    ) {
        timelineEventDao.insert(TimelineEventEntity(
            timestamp = System.currentTimeMillis(),
            role = "GEMMA",
            uiComponentType = "TEXT",
            content = response
        ))

        flight?.let {
            timelineEventDao.insert(TimelineEventEntity(
                timestamp = System.currentTimeMillis() + 1,
                role = "GEMMA",
                uiComponentType = "FLIGHT_CARD",
                content = moshi.adapter(FlightEntity::class.java).toJson(it)
            ))
        }

        luggage?.let {
            timelineEventDao.insert(TimelineEventEntity(
                timestamp = System.currentTimeMillis() + 2,
                role = "GEMMA",
                uiComponentType = "LUGGAGE_CARD",
                content = moshi.adapter(LuggageEntity::class.java).toJson(it)
            ))
        }

        if (!receipts.isNullOrEmpty()) {
            val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, ReceiptEntity::class.java)
            val adapter: com.squareup.moshi.JsonAdapter<List<ReceiptEntity>> = moshi.adapter(type)
            timelineEventDao.insert(TimelineEventEntity(
                timestamp = System.currentTimeMillis() + 3,
                role = "GEMMA",
                uiComponentType = "RECEIPT_CARD",
                content = adapter.toJson(receipts)
            ))
        }

        _state.update { it.copy(isThinking = false) }
    }
}
