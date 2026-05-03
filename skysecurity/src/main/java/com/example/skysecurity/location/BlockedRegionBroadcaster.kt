package com.example.skysecurity.location

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockedRegionBroadcaster @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var advertiser: BluetoothLeAdvertiser? = null
    private var isAdvertising = false
    private val _broadcastingNodeIds = MutableStateFlow<Set<String>>(emptySet())
    val broadcastingNodeIds: StateFlow<Set<String>> = _broadcastingNodeIds.asStateFlow()

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            isAdvertising = true
            Log.d(TAG, "Blocked region broadcast started")
        }
        override fun onStartFailure(errorCode: Int) {
            isAdvertising = false
            Log.e(TAG, "Blocked region broadcast failed: $errorCode")
        }
    }

    /**
     * Start (or restart) advertising the current set of blocked node IDs.
     *
     * When [nodeIds] is empty, we still broadcast "SBBLK:" (the clear signal)
     * and keep advertising indefinitely. The previous implementation only
     * advertised the clear for 2 seconds via a Handler.postDelayed — if the
     * main app's scanner didn't happen to catch that narrow window the clear
     * was silently lost. Now the clear stays advertised until the security
     * officer explicitly blocks something new or stops the broadcast.
     */
    @SuppressLint("MissingPermission")
    fun broadcastBlockedNodes(nodeIds: Set<String>) {
        _broadcastingNodeIds.value = nodeIds

        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = btManager?.adapter ?: return
        if (!adapter.isEnabled) return
        advertiser = adapter.bluetoothLeAdvertiser ?: return

        // Stop any current advertisement before restarting
        if (isAdvertising) {
            try { advertiser?.stopAdvertising(advertiseCallback) } catch (_: SecurityException) {}
            isAdvertising = false
        }

        // Build payload — empty string after prefix = clear signal for the main app
        val payload = if (nodeIds.isEmpty()) {
            "SBBLK:"
        } else {
            "SBBLK:${nodeIds.joinToString(",")}".take(61)
        }

        try {
            adapter.name = payload
        } catch (e: SecurityException) {
            Log.e(TAG, "Need BLUETOOTH_CONNECT", e)
            return
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .build()

        val advertiseData = AdvertiseData.Builder().setIncludeDeviceName(false).build()
        val scanResponse = AdvertiseData.Builder().setIncludeDeviceName(true).build()

        try {
            advertiser?.startAdvertising(settings, advertiseData, scanResponse, advertiseCallback)
        } catch (e: SecurityException) {
            Log.e(TAG, "Need BLUETOOTH_ADVERTISE", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun stopBroadcast() {
        _broadcastingNodeIds.value = emptySet()

        if (!isAdvertising) return
        try {
            advertiser?.stopAdvertising(advertiseCallback)
            val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            btManager?.adapter?.name = "Android"
        } catch (_: SecurityException) {}
        isAdvertising = false
        Log.d(TAG, "Blocked region broadcast stopped")
    }

    companion object {
        private const val TAG = "BlockedBroadcaster"
    }
}
