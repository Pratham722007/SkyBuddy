package com.example.skybuddy.domain.usecase

import com.example.skybuddy.core.result.AppResult
import com.example.skybuddy.data.db.FlightEntity
import com.example.skybuddy.data.repository.FlightRepository
import javax.inject.Inject

class AddTrackedFlightUseCase @Inject constructor(
    private val repository: FlightRepository
) {
    suspend operator fun invoke(flightNumber: String): AppResult<FlightEntity> =
        repository.refresh(flightNumber)
}
