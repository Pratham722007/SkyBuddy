package com.example.skybuddy.location

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log
import com.example.skybuddy.domain.usecase.EvaluateAmbientBeaconUseCase
import com.example.skybuddy.shared.location.BlockedRegionManager
import com.example.skybuddy.shared.location.IndoorLocationManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DynamicBeaconReceiver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val evaluateAmbientBeacon: EvaluateAmbientBeaconUseCase,
    private val indoorLocationManager: IndoorLocationManager,
    private val blockedRegionManager: BlockedRegionManager
) {
    private var isScanning = false
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val processedBeacons: MutableSet<String> =
        Collections.synchronizedSet(mutableSetOf())

    // Track last-seen blocked set to avoid redundant StateFlow emissions
    @Volatile
    private var lastBlockedSet: Set<String> = emptySet()

    // User-facing error/status events — collect in the UI to show Snackbar
    private val _beaconEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val beaconEvents: SharedFlow<String> = _beaconEvents.asSharedFlow()


    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            try {
                result?.scanRecord?.let { scanRecord ->
                    // Use the advertised device name from the scan record only.
                    // BluetoothDevice.getName() requires BLUETOOTH_CONNECT, which we do not hold.
                    val deviceName = scanRecord.deviceName
                    if (deviceName.isNullOrEmpty()) return

                    // ─── Handle blocked-region beacons from security ───
                    if (deviceName.startsWith("SBBLK:")) {
                        val nodesCsv = deviceName.removePrefix("SBBLK:")
                        val nodeIds = nodesCsv.split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .toSet()
                        // Only update if the blocked set actually changed —
                        // avoids redundant StateFlow emissions and pathfinding recalcs.
                        if (nodeIds != lastBlockedSet) {
                            lastBlockedSet = nodeIds
                            Log.d(TAG, "Blocked regions updated: $nodeIds")
                            blockedRegionManager.setBlockedNodes(nodeIds)
                        }
                        return
                    }

                    // ─── Handle SOS beacons (just log, main app doesn't act on its own SOS) ───
                    if (deviceName.startsWith("SBSOS:")) {
                        Log.d(TAG, "SOS beacon seen: $deviceName")
                        return
                    }

                    // ─── Handle shop/offer beacons (existing logic) ───
                    if (deviceName.startsWith("SB:")) {
                        val payload = deviceName.removePrefix("SB:")
                        val parts = payload.split("|")
                        if (parts.size >= 2) {
                            val locationName = parts[0].trim()
                            val offer = parts[1].trim()

                            val uniqueKey = "$locationName-$offer"
                            if (processedBeacons.add(uniqueKey)) {
                                Log.d(TAG, "Intercepted: $locationName - $offer")

                                // Simulate Absolute Anchoring: Snap PDR coordinates based on known beacon name
                                when (locationName) {
                                    "Costa" -> indoorLocationManager.calibratePosition(700f, 700f)
                                    "DutyFree" -> indoorLocationManager.calibratePosition(500f, 600f)
                                }

                                // Trigger JIT AI Generation
                                coroutineScope.launch {
                                    evaluateAmbientBeacon(offer, locationName)
                                }
                            }
                        }
                    }
                }
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
            Log.e(TAG, "BLE scan failed: $reason (code=$errorCode)")
            _beaconEvents.tryEmit("Beacon scan failed: $reason")
        }
    }

    @SuppressLint("MissingPermission")
    fun startScanning() {
        if (isScanning) return
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE)
            as? BluetoothManager
        val bluetoothAdapter = bluetoothManager?.adapter

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            val msg = "Beacon scanning unavailable: Bluetooth is ${if (bluetoothAdapter == null) "not available" else "turned off"}"
            Log.w(TAG, msg)
            _beaconEvents.tryEmit(msg)
            return
        }

        val scanner = bluetoothAdapter.bluetoothLeScanner
        if (scanner == null) {
            Log.e(TAG, "Cannot start scanning: BluetoothLeScanner is null (BT may be turning off)")
            _beaconEvents.tryEmit("Beacon scanning unavailable: Bluetooth is turning off")
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
        Log.d(TAG, "BLE scanning started (low-latency / aggressive)")
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
            processedBeacons.clear()
            lastBlockedSet = emptySet()
            Log.d(TAG, "BLE scanning stopped")
        }
    }

    companion object {
        private const val TAG = "DynamicBeaconReceiver"
    }
}
