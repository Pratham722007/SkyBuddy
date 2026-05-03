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
            Log.d("BlockedBroadcaster", "Blocked region broadcast started")
        }
        override fun onStartFailure(errorCode: Int) {
            isAdvertising = false
            Log.e("BlockedBroadcaster", "Blocked region broadcast failed: $errorCode")
        }
    }

    @SuppressLint("MissingPermission")
    fun broadcastBlockedNodes(nodeIds: Set<String>) {
        _broadcastingNodeIds.value = nodeIds
        if (nodeIds.isEmpty()) {
            stopBroadcast()
            return
        }

        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = btManager?.adapter ?: return
        if (!adapter.isEnabled) return
        advertiser = adapter.bluetoothLeAdvertiser ?: return

        if (isAdvertising) stopBroadcast()

        // Build payload: SBBLK:<id1>,<id2>,...
        val payload = "SBBLK:${nodeIds.joinToString(",")}".take(61)

        try {
            adapter.name = payload
        } catch (e: SecurityException) {
            Log.e("BlockedBroadcaster", "Need BLUETOOTH_CONNECT", e)
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
            Log.e("BlockedBroadcaster", "Need BLUETOOTH_ADVERTISE", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun stopBroadcast() {
        if (!isAdvertising) return
        try {
            advertiser?.stopAdvertising(advertiseCallback)
            val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            btManager?.adapter?.name = "Android"
        } catch (_: SecurityException) {}
        isAdvertising = false
        _broadcastingNodeIds.value = emptySet()
    }
}
