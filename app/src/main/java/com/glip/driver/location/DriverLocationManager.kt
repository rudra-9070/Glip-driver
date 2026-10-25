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
            val activity = context as? android.app.Activity
            if (activity != null) {
                GpsPromptHelper.requestEnableGps(activity)
            } else {
                GpsPromptHelper.openSettingsFallback(context)
            }
        }

        fun isBatteryOptimizationIgnored(context: Context): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val pm = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
                return pm?.isIgnoringBatteryOptimizations(context.packageName) ?: true
            }
            return true
        }

        @SuppressLint("BatteryLife")
        fun requestIgnoreBatteryOptimizations(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = android.net.Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {
                    try {
                        val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(fallback)
                    } catch (_: Exception) {}
                }
            }
        }
    }

    fun refreshGpsStatus(): Boolean {
        val enabled = isGpsProviderEnabled(context)
        _isGpsEnabled.value = enabled
        return enabled
    }

    @SuppressLint("MissingPermission")
    fun getBestLastKnownLocation(): Location? {
        val lm = locationManager ?: return _currentLocation.value
        if (!hasLocationPermission(context)) return _currentLocation.value
        try {
            var best: Location? = _currentLocation.value
            val providers = lm.getProviders(true)
            for (p in providers) {
                try {
                    val loc = lm.getLastKnownLocation(p)
                    if (loc != null && (best == null || loc.time > best.time)) {
                        best = loc
                    }
                } catch (ignored: Exception) {}
            }
            if (best != null) {
                _currentLocation.value = best
            }
            return best
        } catch (e: Exception) {
            return _currentLocation.value
        }
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

            // Production Battery Optimization:
            // High-power GPS is requested with 3000ms and 5m distance filter to avoid 100% continuous RF drain.
            // Redundant simultaneous Network + Passive polling is eliminated.
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                try {
                    lm.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        3000L,
                        5f,
                        this,
                        Looper.getMainLooper()
                    )
                    isListening = true
                } catch (ignored: Exception) {}
            } else if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                // Low-power Network Provider fallback only when GPS is off
                try {
                    lm.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        4000L,
                        10f,
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
