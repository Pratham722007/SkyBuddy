package com.example.skybuddy.domain.usecase

import android.graphics.Bitmap
import com.example.skybuddy.data.repository.LuggageRepository
import com.example.skybuddy.vision.MlKitImageLabeler
import javax.inject.Inject

class DescribeLuggageUseCase @Inject constructor(
    private val labeler: MlKitImageLabeler,
    private val luggageRepository: LuggageRepository
) {
    suspend operator fun invoke(bitmap: Bitmap, persist: Boolean = true): String {
        val labels = labeler.label(bitmap)
        val description = if (labels.isEmpty()) "Unidentified item" else labels.joinToString(", ")
        if (persist) luggageRepository.save(description)
        return description
    }
}
