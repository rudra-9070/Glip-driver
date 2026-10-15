package com.glip.driver.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DriverLocationManager(private val context: Context) : LocationListener {

    private val locationManager: LocationManager? =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private val _isGpsEnabled = MutableStateFlow(isGpsProviderEnabled(context))
    val isGpsEnabled: StateFlow<Boolean> = _isGpsEnabled.asStateFlow()

    private var isListening = false

    companion object {
        fun hasLocationPermission(context: Context): Boolean {
            val fine = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val coarse = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            return fine || coarse
        }

        fun isGpsProviderEnabled(context: Context): Boolean {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
            val gps = try { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) } catch (e: Exception) { false }
            val net = try { lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) } catch (e: Exception) { false }
            return gps || net
        }

        fun openLocationSettings(context: Context) {
            try {
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                // Fallback to general settings if specific action fails
                try {
                    val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(fallback)
                } catch (ignored: Exception) {}
            }
        }
    }

    fun refreshGpsStatus(): Boolean {
        val enabled = isGpsProviderEnabled(context)
        _isGpsEnabled.value = enabled
        return enabled
    }

    @SuppressLint("MissingPermission")
    fun startTracking() {
        refreshGpsStatus()
        if (!hasLocationPermission(context)) {
            return
        }

        val lm = locationManager ?: return

        try {
            // Find best last known location across all providers
            var bestLastKnown: Location? = _currentLocation.value
            val providers = lm.getProviders(true)
            for (p in providers) {
                try {
                    val loc = lm.getLastKnownLocation(p)
                    if (loc != null && (bestLastKnown == null || loc.time > bestLastKnown.time)) {
                        bestLastKnown = loc
                    }
                } catch (ignored: Exception) {}
            }
            if (bestLastKnown != null) {
                _currentLocation.value = bestLastKnown
            }

            // On Android 11+ (API 30+), request immediate one-shot location
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val executor = ContextCompat.getMainExecutor(context)
                if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    try {
                        lm.getCurrentLocation(LocationManager.GPS_PROVIDER, null, executor) { loc ->
                            if (loc != null) _currentLocation.value = loc
                        }
                    } catch (ignored: Exception) {}
                }
                if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    try {
                        lm.getCurrentLocation(LocationManager.NETWORK_PROVIDER, null, executor) { loc ->
                            if (loc != null) _currentLocation.value = loc
                        }
                    } catch (ignored: Exception) {}
                }
            }

            // Remove previous updates before re-registering
            try {
                lm.removeUpdates(this)
            } catch (ignored: Exception) {}
            isListening = false

            // Register continuous live updates with minDistance = 0f (updates even while stationary)
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                try {
                    lm.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        1000L,
                        0f,
                        this,
                        Looper.getMainLooper()
                    )
                    isListening = true
                } catch (ignored: Exception) {}
            }

            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                try {
                    lm.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        1000L,
                        0f,
                        this,
                        Looper.getMainLooper()
                    )
                    isListening = true
                } catch (ignored: Exception) {}
            }

            if (lm.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
                try {
                    lm.requestLocationUpdates(
                        LocationManager.PASSIVE_PROVIDER,
                        1000L,
                        0f,
                        this,
                        Looper.getMainLooper()
                    )
                    isListening = true
                } catch (ignored: Exception) {}
            }
        } catch (e: Exception) {
            // Permission or hardware exception handled safely
        }
    }

    fun stopTracking() {
        if (!isListening) return
        try {
            locationManager?.removeUpdates(this)
            isListening = false
        } catch (e: Exception) {
            // Safe teardown
        }
    }

    override fun onLocationChanged(location: Location) {
        _currentLocation.value = location
    }

    override fun onProviderEnabled(provider: String) {
        refreshGpsStatus()
        startTracking()
    }

    override fun onProviderDisabled(provider: String) {
        refreshGpsStatus()
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        refreshGpsStatus()
    }
}
