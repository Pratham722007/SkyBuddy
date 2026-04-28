package com.example.skybuddy.ui.chat

import com.example.skybuddy.audio.SpeechCallback
import com.example.skybuddy.audio.SpeechError
import com.example.skybuddy.audio.SpeechRecognizer
import com.example.skybuddy.audio.TextToSpeechService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed interface VoiceEvent {
    data class Heard(val text: String) : VoiceEvent
    data class Error(val reason: SpeechError) : VoiceEvent
}

@Singleton
class VoiceController @Inject constructor(
    private val recognizer: SpeechRecognizer,
    private val tts: TextToSpeechService
) {
    private val _events = MutableStateFlow<VoiceEvent?>(null)
    val events: StateFlow<VoiceEvent?> = _events.asStateFlow()

    val ttsStatus get() = tts.status

    fun startListening() {
        recognizer.start(object : SpeechCallback {
            override fun onResult(text: String) { _events.value = VoiceEvent.Heard(text) }
            override fun onError(reason: SpeechError) { _events.value = VoiceEvent.Error(reason) }
        })
    }

    fun stopListening() = recognizer.stop()

    fun speak(text: String) = tts.speak(text)

    fun consume() { _events.value = null }

    fun shutdown() {
        recognizer.destroy()
    }
}
