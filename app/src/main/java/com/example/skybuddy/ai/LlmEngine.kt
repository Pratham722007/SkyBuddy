package com.example.skybuddy.ai

import android.graphics.Bitmap
import kotlinx.coroutines.flow.Flow

enum class Backend { GPU, CPU }

enum class InitStage { ProbingDevice, OpeningModel, AllocatingTensors, Warmup }

sealed interface InitState {
    data class Loading(val backend: Backend, val stage: InitStage) : InitState
    data class Ready(val backend: Backend) : InitState
    data class Failed(val backend: Backend, val message: String, val cause: Throwable? = null) : InitState
}

interface LlmEngine {
    val isReady: Boolean
    fun initialize(preferred: Backend? = null): Flow<InitState>
    suspend fun generateText(prompt: String): String
    fun generateTextStreaming(prompt: String): Flow<String>
    suspend fun generateOneOffText(prompt: String): String
    suspend fun generateMultimodal(prompt: String, bitmap: Bitmap): String
    fun close()
}
