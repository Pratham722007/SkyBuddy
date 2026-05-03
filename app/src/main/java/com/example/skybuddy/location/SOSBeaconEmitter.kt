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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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

    // User-facing error/status events
    private val _sosEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val sosEvents: SharedFlow<String> = _sosEvents.asSharedFlow()

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            isAdvertising = true
            Log.d("SOSBeaconEmitter", "SOS advertising started")
        }
        override fun onStartFailure(errorCode: Int) {
            isAdvertising = false
            val msg = "SOS broadcast failed (error $errorCode)"
            Log.e("SOSBeaconEmitter", msg)
            _sosEvents.tryEmit(msg)
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
        val adapter = btManager?.adapter
        if (adapter == null || !adapter.isEnabled) {
            val msg = "Cannot send SOS: Bluetooth is ${if (adapter == null) "not available" else "turned off"}"
            Log.w("SOSBeaconEmitter", msg)
            _sosEvents.tryEmit(msg)
            return
        }
        advertiser = adapter.bluetoothLeAdvertiser
        if (advertiser == null) {
            val msg = "Cannot send SOS: BLE advertiser unavailable"
            Log.e("SOSBeaconEmitter", msg)
            _sosEvents.tryEmit(msg)
            return
        }

        // Always stop first — isAdvertising is set asynchronously by the
        // callback, so checking it can miss in-progress advertisements.
        try { advertiser?.stopAdvertising(advertiseCallback) } catch (_: SecurityException) {}
        isAdvertising = false

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
            val msg = "SOS broadcast failed: missing Bluetooth permission"
            Log.e("SOSBeaconEmitter", msg, e)
            _sosEvents.tryEmit(msg)
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
            val msg = "SOS broadcast failed: missing advertise permission"
            Log.e("SOSBeaconEmitter", msg, e)
            _sosEvents.tryEmit(msg)
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
        // Always try to stop — don't rely on isAdvertising flag (set async)
        try {
            advertiser?.stopAdvertising(advertiseCallback)
            val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            btManager?.adapter?.name = "Android"
        } catch (e: SecurityException) {
            Log.w("SOSBeaconEmitter", "Failed to stop SOS advertising", e)
        }
        isAdvertising = false
        Log.d("SOSBeaconEmitter", "SOS advertising stopped")
    }
}
