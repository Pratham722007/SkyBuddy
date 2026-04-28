package com.example.skybuddy.ui.modelload

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skybuddy.ai.Backend
import com.example.skybuddy.ai.InitStage
import com.example.skybuddy.ai.InitState
import com.example.skybuddy.ai.LlmEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ModelLoadUi {
    data class Loading(val backend: Backend, val stage: InitStage) : ModelLoadUi
    data class Ready(val backend: Backend) : ModelLoadUi
    data class Failed(val backend: Backend, val message: String) : ModelLoadUi
}

@HiltViewModel
class ModelLoadViewModel @Inject constructor(
    private val llm: LlmEngine
) : ViewModel() {

    private val _state = MutableStateFlow<ModelLoadUi>(
        ModelLoadUi.Loading(Backend.GPU, InitStage.ProbingDevice)
    )
    val state: StateFlow<ModelLoadUi> = _state.asStateFlow()

    private var job: Job? = null

    fun startIfNeeded() {
        if (llm.isReady) {
            _state.value = ModelLoadUi.Ready(Backend.CPU)
            return
        }
        if (job?.isActive == true) return
        job = viewModelScope.launch {
            llm.initialize(null).collect { init ->
                _state.value = when (init) {
                    is InitState.Loading -> ModelLoadUi.Loading(init.backend, init.stage)
                    is InitState.Ready -> ModelLoadUi.Ready(init.backend)
                    is InitState.Failed -> ModelLoadUi.Failed(init.backend, init.message)
                }
            }
        }
    }

    fun retry() {
        job?.cancel()
        job = null
        startIfNeeded()
    }
}
