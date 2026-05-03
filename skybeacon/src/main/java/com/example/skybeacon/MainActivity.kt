package com.example.skybeacon

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.appcompat.app.AlertDialog
import android.view.View
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private var advertiser: BluetoothLeAdvertiser? = null
    private var isAdvertising = false
    private lateinit var statusText: TextView

    // Mock UUID for SkyBeacon
    private val pUuid = ParcelUuid(UUID.fromString("0000FEAA-0000-1000-8000-00805F9B34FB"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.status_text)
        
        checkPermissions()

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter
        
        if (adapter == null || !adapter.isMultipleAdvertisementSupported) {
            statusText.text = "Advertising not supported on this device."
            return
        }

        advertiser = adapter.bluetoothLeAdvertiser

        setupNavigation()
        setupSOS()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setupWithNavController(navController)

        // Hide bottom nav on login screen
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.loginFragment) {
                bottomNav.visibility = View.GONE
            } else {
                bottomNav.visibility = View.VISIBLE
            }
        }
    }

    private fun setupSOS() {
        val fabSos = findViewById<FloatingActionButton>(R.id.fab_sos)
        fabSos.setOnClickListener {
            val prefs = getSharedPreferences("skybeacon_prefs", Context.MODE_PRIVATE)
            val sosMsg = prefs.getString("custom_sos_message", "SB:SOS|EMERGENCY ALERT - Please contact airport staff immediately") ?: "SB:SOS|EMERGENCY ALERT - Please contact airport staff immediately"

            AlertDialog.Builder(this)
                .setTitle("Emergency SOS")
                .setMessage("Send Emergency Alert to all users?")
                .setPositiveButton("Confirm") { _, _ ->
                    triggerBroadcast(sosMsg.take(61))
                    
                    lifecycleScope.launch(Dispatchers.IO) {
                        val db = com.example.skybeacon.data.AppDatabase.getDatabase(this@MainActivity)
                        db.broadcastLogDao().insertLog(
                            com.example.skybeacon.data.BroadcastLog(
                                shopName = "Global",
                                broadcastType = "SOS",
                                content = sosMsg
                            )
                        )
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    fun triggerBroadcast(payload: String) {
        startAdvertising(payload)
    }

    fun haltBroadcast() {
        stopAdvertising()
    }

    private fun checkPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 101)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startAdvertising(payload: String) {
        if (advertiser == null) return
        if (isAdvertising) stopAdvertising()

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .build()

        // adapter.name setter requires BLUETOOTH_CONNECT at runtime.
        val adapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        try {
            adapter.name = payload
        } catch (e: SecurityException) {
            statusText.text = "Need BLUETOOTH_CONNECT permission. Tap again after granting."
            return
        }

        // The 31-byte advertisement is too small for our payload, so we put the
        // device name in the scan response (its own 31-byte budget) and keep the
        // main advertisement empty. The scanner merges both into ScanRecord.
        val advertiseData = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .build()

        val scanResponse = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .build()

        try {
            advertiser?.startAdvertising(settings, advertiseData, scanResponse, advertiseCallback)
        } catch (e: SecurityException) {
            statusText.text = "Need BLUETOOTH_ADVERTISE permission. Tap again after granting."
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopAdvertising() {
        if (!isAdvertising || advertiser == null) return
        try {
            advertiser?.stopAdvertising(advertiseCallback)
        } catch (e: SecurityException) {
            // Permission revoked between start and stop; nothing to do.
        }
        isAdvertising = false
        statusText.text = "Stopped Advertising"

        val adapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        try {
            adapter.name = "Android"
        } catch (e: Exception) {}
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            isAdvertising = true
            statusText.text = "Advertising Started Successfully!"
        }

        override fun onStartFailure(errorCode: Int) {
            isAdvertising = false
            statusText.text = "Advertising Failed: $errorCode"
        }
    }
}
