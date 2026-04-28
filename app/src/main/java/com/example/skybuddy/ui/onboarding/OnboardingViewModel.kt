package com.example.skybuddy.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface OnboardingPhase {
    data object Welcome : OnboardingPhase
    data class TokenEntry(val showError: Boolean = false) : OnboardingPhase
    data class Downloading(val bytesRead: Long, val total: Long) : OnboardingPhase
    data class Failed(val message: String) : OnboardingPhase
    data object Done : OnboardingPhase
}

data class OnboardingUiState(
    val phase: OnboardingPhase = OnboardingPhase.Welcome,
    val token: String = ""
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val downloader: ModelDownloader
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    init {
        if (downloader.isDownloaded()) _state.update { it.copy(phase = OnboardingPhase.Done) }
    }

    fun onContinueFromWelcome() {
        _state.update { it.copy(phase = OnboardingPhase.TokenEntry()) }
    }

    fun onTokenChanged(token: String) {
        _state.update { it.copy(token = token) }
    }

    fun startDownload() {
        val token = _state.value.token.trim()
        if (token.isBlank()) {
            _state.update { it.copy(phase = OnboardingPhase.TokenEntry(showError = true)) }
            return
        }
        _state.update { it.copy(phase = OnboardingPhase.Downloading(0, -1)) }
        viewModelScope.launch {
            downloader.download(ModelDownloader.DEFAULT_MODEL_URL, token).collect { progress ->
                when (progress) {
                    is DownloadProgress.Active ->
                        _state.update { it.copy(phase = OnboardingPhase.Downloading(progress.bytesRead, progress.total)) }
                    is DownloadProgress.Complete ->
                        _state.update { it.copy(phase = OnboardingPhase.Done) }
                    is DownloadProgress.Failed ->
                        _state.update { it.copy(phase = OnboardingPhase.Failed(progress.message)) }
                }
            }
        }
    }

    fun retry() {
        _state.update { it.copy(phase = OnboardingPhase.TokenEntry()) }
    }
}
