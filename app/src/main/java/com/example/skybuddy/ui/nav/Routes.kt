package com.example.skybuddy.ui.nav

object Routes {
    const val ONBOARDING = "onboarding"
    const val MODEL_LOAD = "model_load"
    const val MAIN_SHELL = "main_shell"
    const val HOME = "home"
    const val CHAT_PATTERN = "chat/{flightNumber}"
    const val FLIGHT_INFO_PATTERN = "flight_info/{flightNumber}"
    const val FLIGHT_DETAIL_PATTERN = "flight_detail/{flightNumber}"

    fun chat(flightNumber: String): String = "chat/$flightNumber"
    fun flightInfo(flightNumber: String): String = "flight_info/$flightNumber"
    fun flightDetail(flightNumber: String): String = "flight_detail/$flightNumber"
}
