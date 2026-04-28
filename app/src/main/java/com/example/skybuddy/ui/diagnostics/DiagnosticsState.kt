package com.example.skybuddy.ui.diagnostics

sealed interface ComponentStatus {
    data object Initializing : ComponentStatus
    data object Ready : ComponentStatus
    data class Error(val reason: String) : ComponentStatus
}

data class DiagnosticsState(
    val llm: ComponentStatus = ComponentStatus.Initializing,
    val tts: ComponentStatus = ComponentStatus.Initializing,
    val stt: ComponentStatus = ComponentStatus.Initializing,
    val database: ComponentStatus = ComponentStatus.Ready,
    val network: ComponentStatus = ComponentStatus.Ready
)
