package com.example.skybuddy.domain.usecase

import android.graphics.Bitmap
import com.example.skybuddy.ai.LlmEngine
import com.example.skybuddy.data.repository.LuggageRepository
import javax.inject.Inject

class DescribeLuggageUseCase @Inject constructor(
    private val llmEngine: LlmEngine,
    private val luggageRepository: LuggageRepository
) {
    suspend operator fun invoke(bitmap: Bitmap, persist: Boolean = true): String {
        val description = llmEngine.generateMultimodal("Describe this luggage concisely.", bitmap)
        if (persist) luggageRepository.save(description)
        return description
    }
}