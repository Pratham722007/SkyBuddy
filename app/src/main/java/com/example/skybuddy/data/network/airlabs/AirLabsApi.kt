package com.example.skybuddy.data.network.airlabs

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class AirLabsScheduleResponse(
    val response: List<AirLabsSchedule>?
)

@JsonClass(generateAdapter = true)
data class AirLabsSchedule(
    @Json(name = "airline_iata") val airlineIata: String?,
    @Json(name = "flight_iata") val flightIata: String?,
    @Json(name = "dep_iata") val depIata: String?,
    @Json(name = "dep_terminal") val depTerminal: String?,
    @Json(name = "dep_gate") val depGate: String?,
    @Json(name = "dep_time") val depTime: String?,
    @Json(name = "arr_iata") val arrIata: String?,
    @Json(name = "arr_terminal") val arrTerminal: String?,
    @Json(name = "arr_gate") val arrGate: String?,
    @Json(name = "arr_time") val arrTime: String?,
    val status: String?
)

interface AirLabsApi {
    @GET("api/v9/schedules")
    suspend fun getFlightSchedule(
        @Query("api_key") apiKey: String,
        @Query("flight_iata") flightIata: String
    ): AirLabsScheduleResponse
}
