package com.example.skybuddy.ui.flight

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skybuddy.data.db.FlightEntity
import com.example.skybuddy.data.repository.FlightRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FlightInfoViewModel @Inject constructor(
    private val flightRepository: FlightRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _flight = MutableStateFlow<FlightEntity?>(null)
    val flight: StateFlow<FlightEntity?> = _flight.asStateFlow()

    init {
        val flightNumber = savedStateHandle.get<String>("flightNumber") ?: ""
        if (flightNumber.isNotBlank()) {
            viewModelScope.launch {
                flightRepository.observeFlight(flightNumber).collect { entity ->
                    _flight.value = entity
                }
            }
        }
    }
}
