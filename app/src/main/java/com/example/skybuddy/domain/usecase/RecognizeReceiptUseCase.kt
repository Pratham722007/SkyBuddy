package com.example.skybuddy.domain.usecase

import android.graphics.Bitmap
import com.example.skybuddy.ai.LlmEngine
import javax.inject.Inject

class RecognizeReceiptUseCase @Inject constructor(
    private val llmEngine: LlmEngine
) {
    suspend operator fun invoke(bitmap: Bitmap): String {
        return llmEngine.generateMultimodal("Extract all text and amounts from this receipt.", bitmap)
    }
}