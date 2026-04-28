package com.example.skybuddy.core.result

sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>
    data class Error(val reason: ErrorReason, val cause: Throwable? = null) : AppResult<Nothing>
}

sealed interface ErrorReason {
    data object MissingApiKey : ErrorReason
    data object Offline : ErrorReason
    data object NotFound : ErrorReason
    data class Network(val message: String) : ErrorReason
    data class Unexpected(val message: String) : ErrorReason
}
