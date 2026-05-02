package com.example.skybuddy.location

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.util.Log
import com.example.skybuddy.domain.usecase.EvaluateAmbientBeaconUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DynamicBeaconReceiver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val evaluateAmbientBeacon: EvaluateAmbientBeaconUseCase,
    private val indoorLocationManager: IndoorLocationManager
) {
    private var isScanning = false
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val processedBeacons = mutableSetOf<String>()

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.scanRecord?.let { scanRecord ->
                // Example simplified BLE beacon parser (e.g. from service data or device name)
                val deviceName = result.device?.name ?: scanRecord.deviceName
                if (deviceName != null && deviceName.startsWith("SkyBeacon:")) {
                    val payload = deviceName.removePrefix("SkyBeacon:")
                    val parts = payload.split("|")
                    if (parts.size >= 3) {
                        val locationType = parts[0].trim()
                        val locationName = parts[1].trim()
                        val offer = parts[2].trim()

                        val uniqueKey = "$locationName-$offer"
                        if (!processedBeacons.contains(uniqueKey)) {
                            processedBeacons.add(uniqueKey)
                            Log.d("DynamicBeaconReceiver", "Intercepted: $locationName - $offer")

                            // Simulate Absolute Anchoring: Snap PDR coordinates based on known beacon name
                            when (locationName) {
                                "Costa Coffee" -> indoorLocationManager.calibratePosition(700f, 700f) // STV_F0_COFFEE
                                "Duty Free" -> indoorLocationManager.calibratePosition(500f, 600f) // DUTY_FREE
                            }

                            // Trigger JIT AI Generation
                            coroutineScope.launch {
                                evaluateAmbientBeacon(offer, locationName)
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startScanning() {
        if (isScanning) return
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val bluetoothAdapter = bluetoothManager?.adapter
        
        if (bluetoothAdapter?.isEnabled == true) {
            val scanner = bluetoothAdapter.bluetoothLeScanner
            scanner?.startScan(scanCallback)
            isScanning = true
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScanning() {
        if (!isScanning) return
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val bluetoothAdapter = bluetoothManager?.adapter
        
        if (bluetoothAdapter?.isEnabled == true) {
            val scanner = bluetoothAdapter.bluetoothLeScanner
            scanner?.stopScan(scanCallback)
            isScanning = false
        }
    }
}
