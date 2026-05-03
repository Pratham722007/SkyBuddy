package com.example.skysecurity.location

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

data class SOSAlert(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: String,
    val locationX: Int? = null,
    val locationY: Int? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val acknowledged: Boolean = false
)

@Singleton
class SOSBeaconScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var isScanning = false
    private val _alerts = MutableStateFlow<List<SOSAlert>>(emptyList())
    val alerts: StateFlow<List<SOSAlert>> = _alerts.asStateFlow()
    private val seenPayloads: MutableSet<String> =
        Collections.synchronizedSet(mutableSetOf())

    // User-facing error/status events — collect in the UI to show Snackbar
    private val _scanEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val scanEvents: SharedFlow<String> = _scanEvents.asSharedFlow()

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            try {
                val deviceName = result?.scanRecord?.deviceName
                if (deviceName.isNullOrEmpty()) return
                if (!deviceName.startsWith("SBSOS:")) return

                // Deduplicate
                if (!seenPayloads.add(deviceName)) return

                val payload = deviceName.removePrefix("SBSOS:")
                val parts = payload.split("|")
                val type = parts[0]
                var locX: Int? = null
                var locY: Int? = null
                if (parts.size >= 2) {
                    val coords = parts[1].split(",")
                    if (coords.size == 2) {
                        locX = coords[0].toIntOrNull()
                        locY = coords[1].toIntOrNull()
                    }
                }

                val alert = SOSAlert(type = type, locationX = locX, locationY = locY)
                Log.d(TAG, "SOS received: $type at ($locX, $locY)")
                _alerts.value = listOf(alert) + _alerts.value
            } catch (e: Exception) {
                Log.e(TAG, "Error processing scan result", e)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            isScanning = false
            val reason = when (errorCode) {
                SCAN_FAILED_ALREADY_STARTED -> "ALREADY_STARTED"
                SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "APP_REGISTRATION_FAILED"
                SCAN_FAILED_INTERNAL_ERROR -> "INTERNAL_ERROR"
                SCAN_FAILED_FEATURE_UNSUPPORTED -> "FEATURE_UNSUPPORTED"
                else -> "UNKNOWN($errorCode)"
            }
            Log.e(TAG, "SOS scan failed: $reason (code=$errorCode)")
            _scanEvents.tryEmit("SOS scan failed: $reason")
        }
    }

    @SuppressLint("MissingPermission")
    fun startScanning() {
        if (isScanning) return
        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = btManager?.adapter

        if (adapter == null || !adapter.isEnabled) {
            val msg = "SOS scanning unavailable: Bluetooth is ${if (adapter == null) "not available" else "turned off"}"
            Log.w(TAG, msg)
            _scanEvents.tryEmit(msg)
            return
        }

        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            Log.e(TAG, "Cannot start SOS scanning: BluetoothLeScanner is null (BT may be turning off)")
            _scanEvents.tryEmit("SOS scanning unavailable: Bluetooth is turning off")
            return
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
            .setReportDelay(0)
            .build()
        scanner.startScan(emptyList<ScanFilter>(), settings, scanCallback)
        isScanning = true
        Log.d(TAG, "SOS scanning started (low-latency / aggressive)")
    }

    @SuppressLint("MissingPermission")
    fun stopScanning() {
        if (!isScanning) return
        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = btManager?.adapter
        if (adapter?.isEnabled == true) {
            adapter.bluetoothLeScanner?.stopScan(scanCallback)
            isScanning = false
            Log.d(TAG, "SOS scanning stopped")
        }
    }

    fun acknowledgeAlert(alertId: String) {
        _alerts.value = _alerts.value.map {
            if (it.id == alertId) it.copy(acknowledged = true) else it
        }
    }

    fun clearAlerts() {
        _alerts.value = emptyList()
        seenPayloads.clear()
    }

    companion object {
        private const val TAG = "SOSBeaconScanner"
    }
}
