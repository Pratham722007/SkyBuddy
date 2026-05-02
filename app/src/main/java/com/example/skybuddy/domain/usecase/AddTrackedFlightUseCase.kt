package com.example.skybuddy.domain.usecase

import com.example.skybuddy.core.result.AppResult
import com.example.skybuddy.data.db.FlightEntity
import com.example.skybuddy.data.repository.FlightRepository
import com.example.skybuddy.work.AlarmScheduler
import javax.inject.Inject

class AddTrackedFlightUseCase @Inject constructor(
    private val repository: FlightRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke(flightNumber: String): AppResult<FlightEntity> {
        val result = repository.refresh(flightNumber)
        if (result is AppResult.Success) {
            alarmScheduler.schedulePreflightAlarm(
                result.value.flightNumber, 
                result.value.departureTimeEpoch
            )
        }
        return result
    }
}
