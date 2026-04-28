package com.example.skybuddy.ai

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ConfigurationInfo
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccelerationCompat @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun isGpuAvailable(): Boolean = try {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info: ConfigurationInfo = am.deviceConfigurationInfo
        val supportsGles3 = info.reqGlEsVersion >= 0x30000
        val recentEnough = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
        supportsGles3 && recentEnough
    } catch (t: Throwable) {
        Log.w(TAG, "GPU probe failed", t)
        false
    }

    companion object { private const val TAG = "AccelerationCompat" }
}
