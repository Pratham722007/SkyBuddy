package com.example.skybuddy.domain.usecase

import com.example.skybuddy.ai.LlmEngine
import com.example.skybuddy.data.db.TimelineEventDao
import com.example.skybuddy.data.db.TimelineEventEntity
import com.example.skybuddy.work.NotificationHelper
import javax.inject.Inject

class EvaluateAmbientBeaconUseCase @Inject constructor(
    private val llmEngine: LlmEngine,
    private val timelineEventDao: TimelineEventDao,
    private val notificationHelper: NotificationHelper
) {
    suspend operator fun invoke(beaconPayload: String, locationName: String) {
        val prompt = "User is walking past a physical beacon at $locationName broadcasting: '$beaconPayload'. Write a 1-sentence, engaging, helpful tip as an offline AI companion for the user."
        val response = llmEngine.generateText(prompt)
        
        timelineEventDao.insert(
            TimelineEventEntity(
                timestamp = System.currentTimeMillis(),
                role = "SYSTEM_SPATIAL",
                uiComponentType = "MAP_TOAST",
                content = response.trim()
            )
        )
        
        notificationHelper.notifyFlightUpdate(
            flightNumber = "BEACON", // Reusing the notification method for debugging
            title = "📍 Beacon Intercepted: $locationName",
            message = response.trim()
        )
    }
}
