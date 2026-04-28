package com.example.skybuddy.ui.nav

object Routes {
    const val ONBOARDING = "onboarding"
    const val MODEL_LOAD = "model_load"
    const val HOME = "home"
    const val CHAT_PATTERN = "chat/{flightNumber}"
    fun chat(flightNumber: String): String = "chat/$flightNumber"
}
