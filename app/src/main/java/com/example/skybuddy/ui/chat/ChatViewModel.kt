package com.example.skybuddy.ui.chat

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skybuddy.data.db.FlightEntity
import com.example.skybuddy.data.db.LuggageEntity
import com.example.skybuddy.data.db.ReceiptEntity
import com.example.skybuddy.data.repository.FlightRepository
import com.example.skybuddy.domain.usecase.ChatTurnUseCase
import com.example.skybuddy.domain.usecase.DescribeLuggageUseCase
import com.example.skybuddy.domain.usecase.RecognizeReceiptUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ChatRole { USER, ASSISTANT }

sealed interface ChatItem {
    val id: Long

    data class Message(
        override val id: Long,
        val role: ChatRole,
        val text: String
    ) : ChatItem

    data class FlightCard(override val id: Long, val flight: FlightEntity) : ChatItem
    data class LuggageCard(override val id: Long, val luggage: LuggageEntity) : ChatItem
    data class ReceiptListCard(override val id: Long, val receipts: List<ReceiptEntity>) : ChatItem
}

data class ChatUiState(
    val items: List<ChatItem> = emptyList(),
    val input: String = "",
    val isThinking: Boolean = false,
    val isIntercomMode: Boolean = false
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val flightRepository: FlightRepository,
    private val chatTurn: ChatTurnUseCase,
    private val recognizeReceipt: RecognizeReceiptUseCase,
    private val describeLuggage: DescribeLuggageUseCase,
    val voiceController: VoiceController
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var nextId = 1L
    private var pinnedFlightNumber: String? = null

    private val _pinnedFlight = MutableStateFlow<FlightEntity?>(null)
    val pinnedFlight: StateFlow<FlightEntity?> = _pinnedFlight.asStateFlow()

    fun setFlightContext(flightNumber: String?) {
        if (flightNumber.isNullOrBlank()) return
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

    fun sendText(): String? {
        val prompt = _state.value.input.trim()
        if (prompt.isBlank() || _state.value.isThinking) return null
        addUser(prompt)
        _state.update { it.copy(input = "", isThinking = true) }
        viewModelScope.launch {
            val result = chatTurn.text(prompt, pinnedFlightNumber)
            applyTurn(result.response, result.flight, result.luggage, result.receipts)
        }
        return prompt
    }

    fun sendImage(prompt: String, bitmap: Bitmap) {
        if (_state.value.isThinking) return
        addUser(prompt.ifBlank { "Image" })
        _state.update { it.copy(isThinking = true) }
        viewModelScope.launch {
            val result = chatTurn.image(prompt, bitmap, pinnedFlightNumber)
            applyTurn(result.response, result.flight, result.luggage, result.receipts)
        }
    }

    fun captureReceipt(bitmap: Bitmap) {
        viewModelScope.launch {
            val text = recognizeReceipt(bitmap).ifBlank { "No text detected on receipt" }
            _state.update { it.copy(items = it.items + ChatItem.Message(nextId(), ChatRole.ASSISTANT, "Receipt: $text")) }
        }
    }

    fun captureLuggage(bitmap: Bitmap) {
        viewModelScope.launch {
            val description = describeLuggage(bitmap)
            _state.update {
                it.copy(items = it.items + ChatItem.Message(nextId(), ChatRole.ASSISTANT, "Saved bag: $description"))
            }
        }
    }

    private fun addUser(text: String) {
        _state.update { it.copy(items = it.items + ChatItem.Message(nextId(), ChatRole.USER, text)) }
    }

    private fun applyTurn(
        response: String,
        flight: FlightEntity?,
        luggage: LuggageEntity?,
        receipts: List<ReceiptEntity>?
    ) {
        _state.update { current ->
            val additions = buildList {
                add(ChatItem.Message(nextId(), ChatRole.ASSISTANT, response))
                flight?.let { add(ChatItem.FlightCard(nextId(), it)) }
                luggage?.let { add(ChatItem.LuggageCard(nextId(), it)) }
                receipts?.let { add(ChatItem.ReceiptListCard(nextId(), it)) }
            }
            current.copy(items = current.items + additions, isThinking = false)
        }
    }

    private fun nextId(): Long = nextId++
}
