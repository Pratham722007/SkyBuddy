package com.example.skysecurity.location

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val seenPayloads = mutableSetOf<String>()

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            val deviceName = result?.scanRecord?.deviceName ?: return
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
            Log.d("SOSScanner", "SOS received: $type at ($locX, $locY)")
            _alerts.value = listOf(alert) + _alerts.value
        }
    }

    @SuppressLint("MissingPermission")
    fun startScanning() {
        if (isScanning) return
        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = btManager?.adapter
        if (adapter?.isEnabled == true) {
            adapter.bluetoothLeScanner?.startScan(scanCallback)
            isScanning = true
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScanning() {
        if (!isScanning) return
        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = btManager?.adapter
        if (adapter?.isEnabled == true) {
            adapter.bluetoothLeScanner?.stopScan(scanCallback)
            isScanning = false
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
}
