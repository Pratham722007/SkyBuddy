package com.example.skybuddy.ui.nav

object Routes {
    const val ONBOARDING = "onboarding"
    const val MODEL_LOAD = "model_load"
    const val HOME = "home"
    const val JOURNEY_SELECT_PATTERN = "journey_select/{flightNumber}"
    const val HOME_PHASE_PATTERN = "home_phase/{flightNumber}/{departureEpoch}"
    const val INDOOR_MAP_PATTERN = "indoor_map/{flightNumber}"
    const val CHAT_PATTERN = "chat/{flightNumber}"
    const val FLIGHT_INFO_PATTERN = "flight_info/{flightNumber}"

    fun journeySelect(flightNumber: String): String = "journey_select/$flightNumber"
    fun homePhase(flightNumber: String, departureEpoch: Long): String = "home_phase/$flightNumber/$departureEpoch"
    fun indoorMap(flightNumber: String): String = "indoor_map/$flightNumber"
    fun chat(flightNumber: String): String = "chat/$flightNumber"
    fun flightInfo(flightNumber: String): String = "flight_info/$flightNumber"
}
