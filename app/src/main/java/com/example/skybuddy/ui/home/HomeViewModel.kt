package com.example.skybuddy.ui.home

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skybuddy.core.result.AppResult
import com.example.skybuddy.core.result.ErrorReason
import com.example.skybuddy.data.db.FlightEntity
import com.example.skybuddy.data.repository.FlightRepository
import com.example.skybuddy.domain.usecase.AddTrackedFlightUseCase
import com.example.skybuddy.domain.usecase.IngestFlightUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val input: String = "",
    val isAdding: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val flightRepository: FlightRepository,
    private val addTrackedFlight: AddTrackedFlightUseCase,
    private val ingestFlightUseCase: IngestFlightUseCase
) : ViewModel() {

    private val _ui = MutableStateFlow(HomeUiState())
    val ui: StateFlow<HomeUiState> = _ui.asStateFlow()

    val upcoming: StateFlow<List<FlightEntity>> = flightRepository.observeUpcoming()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val past: StateFlow<List<FlightEntity>> = flightRepository.observePast()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onInputChanged(value: String) = _ui.update { it.copy(input = value) }

    suspend fun ingestFlight(context: Context, uri: Uri): Result<FlightEntity> {
        _ui.update { it.copy(isAdding = true, message = "Ingesting boarding pass...") }
        val result = ingestFlightUseCase(context, uri)
        _ui.update { 
            it.copy(
                isAdding = false,
                message = if (result.isSuccess) "Flight ${result.getOrNull()?.flightNumber} tracked!" 
                          else "Failed to ingest boarding pass: ${result.exceptionOrNull()?.message}"
            ) 
        }
        return result
    }

    suspend fun ingestFlightBitmap(bitmap: Bitmap): Result<FlightEntity> {
        _ui.update { it.copy(isAdding = true, message = "Scanning boarding pass...") }
        val result = ingestFlightUseCase(bitmap)
        _ui.update { 
            it.copy(
                isAdding = false,
                message = if (result.isSuccess) "Flight ${result.getOrNull()?.flightNumber} tracked!" 
                          else "Failed to ingest boarding pass: ${result.exceptionOrNull()?.message}"
            ) 
        }
        return result
    }

    fun addFlight() {
        val number = _ui.value.input.trim()
        if (number.isBlank()) return
        _ui.update { it.copy(isAdding = true, message = null) }
        viewModelScope.launch {
            val result = addTrackedFlight(number)
            val message = when (result) {
                is AppResult.Success -> "Tracking ${result.value.flightNumber}"
                is AppResult.Error -> when (val r = result.reason) {
                    ErrorReason.MissingApiKey -> "AirLabs key not configured — running offline"
                    ErrorReason.Offline -> "Offline — using cached data if available"
                    ErrorReason.NotFound -> "Flight not found"
                    is ErrorReason.Network -> "Network error: ${r.message}"
                    is ErrorReason.Unexpected -> "Unexpected: ${r.message}"
                }
            }
            _ui.update { it.copy(isAdding = false, input = "", message = message) }
        }
    }

    fun dismissMessage() = _ui.update { it.copy(message = null) }
}
