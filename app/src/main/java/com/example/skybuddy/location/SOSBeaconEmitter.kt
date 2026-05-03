package com.example.skybuddy.location

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SOSBeaconEmitter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var advertiser: BluetoothLeAdvertiser? = null
    private var isAdvertising = false
    private var stopJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            isAdvertising = true
            Log.d("SOSBeaconEmitter", "SOS advertising started")
        }
        override fun onStartFailure(errorCode: Int) {
            isAdvertising = false
            Log.e("SOSBeaconEmitter", "SOS advertising failed: $errorCode")
        }
    }

    /**
     * Emit an SOS beacon with the given type and optional location.
     * Format: SBSOS:<TYPE>|X:<x>|Y:<y>
     * Auto-stops after [durationMs].
     */
    @SuppressLint("MissingPermission")
    fun emitSOS(type: String, x: Float? = null, y: Float? = null, durationMs: Long = 30_000) {
        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = btManager?.adapter ?: return
        if (!adapter.isEnabled) return
        advertiser = adapter.bluetoothLeAdvertiser ?: return

        if (isAdvertising) stopSOS()

        // Build payload within 61-char BLE name limit
        var payload = "SBSOS:$type"
        if (x != null && y != null) {
            val locSuffix = "|${x.toInt()},${y.toInt()}"
            if (payload.length + locSuffix.length <= 61) {
                payload += locSuffix
            }
        }

        try {
            adapter.name = payload.take(61)
        } catch (e: SecurityException) {
            Log.e("SOSBeaconEmitter", "Need BLUETOOTH_CONNECT", e)
            return
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .build()

        val advertiseData = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .build()

        val scanResponse = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .build()

        try {
            advertiser?.startAdvertising(settings, advertiseData, scanResponse, advertiseCallback)
        } catch (e: SecurityException) {
            Log.e("SOSBeaconEmitter", "Need BLUETOOTH_ADVERTISE", e)
            return
        }

        // Auto-stop after duration
        stopJob?.cancel()
        stopJob = scope.launch {
            delay(durationMs)
            stopSOS()
        }
    }

    @SuppressLint("MissingPermission")
    fun stopSOS() {
        stopJob?.cancel()
        if (!isAdvertising) return
        try {
            advertiser?.stopAdvertising(advertiseCallback)
            val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            btManager?.adapter?.name = "Android"
        } catch (_: SecurityException) {}
        isAdvertising = false
        Log.d("SOSBeaconEmitter", "SOS advertising stopped")
    }
}
