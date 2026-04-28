package com.example.skybuddy.core.time

import javax.inject.Inject
import javax.inject.Singleton

fun interface Clock {
    fun nowMillis(): Long
}

@Singleton
class SystemClock @Inject constructor() : Clock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}
