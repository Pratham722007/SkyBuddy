package com.example.skybuddy.domain.state

import com.example.skybuddy.ui.journey.JourneyPhase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JourneyManager @Inject constructor() {
    private val _currentPhase = MutableStateFlow(JourneyPhase.HOME)
    val currentPhase: StateFlow<JourneyPhase> = _currentPhase.asStateFlow()

    fun setPhase(phase: JourneyPhase) {
        _currentPhase.value = phase
    }
}
