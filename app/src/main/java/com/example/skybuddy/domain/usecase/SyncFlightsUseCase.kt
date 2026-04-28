package com.example.skybuddy.domain.usecase

import com.example.skybuddy.data.db.FlightEntity
import com.example.skybuddy.data.repository.FlightRepository
import javax.inject.Inject

class SyncFlightsUseCase @Inject constructor(
    private val repository: FlightRepository
) {
    suspend operator fun invoke(): List<Pair<FlightEntity, FlightEntity?>> =
        repository.refreshAllTracked()
}
