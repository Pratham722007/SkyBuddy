package com.example.skybuddy.domain.usecase

import android.graphics.Bitmap
import com.example.skybuddy.vision.MlKitTextRecognizer
import javax.inject.Inject

class RecognizeReceiptUseCase @Inject constructor(
    private val recognizer: MlKitTextRecognizer
) {
    suspend operator fun invoke(bitmap: Bitmap): String = recognizer.recognize(bitmap)
}
