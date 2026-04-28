package com.example.skybuddy.domain.model

@JvmInline value class FlightNumber(val raw: String) {
    init { require(raw.isNotBlank()) { "Flight number cannot be blank" } }
    val normalized: String get() = raw.trim().uppercase()
}

@JvmInline value class Gate(val value: String)
@JvmInline value class Terminal(val value: String)
@JvmInline value class Seat(val value: String)
