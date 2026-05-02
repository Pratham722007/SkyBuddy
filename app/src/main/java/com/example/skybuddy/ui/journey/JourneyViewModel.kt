package com.example.skybuddy.ui.journey

import androidx.lifecycle.ViewModel
import com.example.skybuddy.domain.state.JourneyManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

enum class JourneyPhase(val displayName: String) {
    HOME("Home"),
    AIRPORT_ENTRANCE("Airport Entrance"),
    BAGGAGE_DROP("Baggage Drop"),
    SECURITY_CHECKPOINT("Security Checkpoint"),
    GATE("Gate")
}

@HiltViewModel
class JourneyViewModel @Inject constructor(
    private val journeyManager: JourneyManager
) : ViewModel() {
    val currentPhase: StateFlow<JourneyPhase> = journeyManager.currentPhase

    fun setPhase(phase: JourneyPhase) {
        journeyManager.setPhase(phase)
    }
}
