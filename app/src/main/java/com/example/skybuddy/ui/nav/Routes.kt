package com.example.skybuddy.ui.nav

object Routes {
    const val ONBOARDING = "onboarding"
    const val MODEL_LOAD = "model_load"
    const val HOME = "home"
    
    const val JOURNEY_SELECTION_PATTERN = "journey_selection/{flightNumber}"
    fun journeySelection(flightNumber: String): String = "journey_selection/$flightNumber"

    const val HOME_PHASE_PATTERN = "home_phase/{flightNumber}"
    fun homePhase(flightNumber: String): String = "home_phase/$flightNumber"

    const val INDOOR_MAP_PATTERN = "indoor_map/{flightNumber}"
    fun indoorMap(flightNumber: String): String = "indoor_map/$flightNumber"
    
    const val CHAT_PATTERN = "chat/{flightNumber}"
    fun chat(flightNumber: String): String = "chat/$flightNumber"
}
