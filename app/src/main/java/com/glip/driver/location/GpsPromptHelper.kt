package com.glip.driver.location

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority

object GpsPromptHelper {

    /**
     * Shows Google Play Services Location Accuracy system popup dialog:
     * "To continue, your device will need to use Location Accuracy... [Turn on]"
     * Tapping "Turn on" immediately enables device GPS without opening Android Settings.
     */
    fun requestEnableGps(
        activity: Activity,
        launcher: ActivityResultLauncher<IntentSenderRequest>? = null,
        onAlreadyEnabled: (() -> Unit)? = null,
        onFailed: (() -> Unit)? = null
    ) {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L).build()
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true) // Triggers the direct system popup

        val client = LocationServices.getSettingsClient(activity)
        client.checkLocationSettings(builder.build())
            .addOnSuccessListener {
                onAlreadyEnabled?.invoke()
            }
            .addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    try {
                        if (launcher != null) {
                            val request = IntentSenderRequest.Builder(exception.resolution.intentSender).build()
                            launcher.launch(request)
                        } else {
                            exception.startResolutionForResult(activity, 9001)
                        }
                    } catch (e: Exception) {
                        openSettingsFallback(activity)
                        onFailed?.invoke()
                    }
                } else {
                    openSettingsFallback(activity)
                    onFailed?.invoke()
                }
            }
    }

    fun openSettingsFallback(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
            } catch (_: Exception) {}
        }
    }
}
