package com.glip.driver.data.local

import android.content.Context
import android.content.SharedPreferences
import com.glip.driver.domain.model.DriverProfile
import com.glip.driver.domain.model.DriverStatus

/**
 * Lightweight local demo persistence using standard Android SharedPreferences.
 * Stores only minimal demo state (logged in profile name, mobile, etc.).
 * Does NOT store unencrypted passwords or production credentials.
 */
class LocalDemoSessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("glip_driver_demo_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_DRIVER_ID = "driver_id"
        private const val KEY_FULL_NAME = "driver_full_name"
        private const val KEY_MOBILE = "driver_mobile"
        private const val KEY_EMAIL = "driver_email"
        private const val KEY_IS_AVAILABLE = "is_available"
    }

    fun saveProfile(profile: DriverProfile) {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_DRIVER_ID, profile.id)
            .putString(KEY_FULL_NAME, profile.fullName)
            .putString(KEY_MOBILE, profile.mobileNumber)
            .putString(KEY_EMAIL, profile.email)
            .apply()
    }

    fun getProfile(): DriverProfile? {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        if (!isLoggedIn) return null

        val name = prefs.getString(KEY_FULL_NAME, null) ?: return null
        val mobile = prefs.getString(KEY_MOBILE, "") ?: ""
        val email = prefs.getString(KEY_EMAIL, null)
        val id = prefs.getString(KEY_DRIVER_ID, "drv_demo_001") ?: "drv_demo_001"

        return DriverProfile(
            id = id,
            fullName = name,
            mobileNumber = mobile,
            email = email,
            status = DriverStatus.DEMO_ACTIVE
        )
    }

    fun isDriverAvailable(): Boolean {
        return prefs.getBoolean(KEY_IS_AVAILABLE, true)
    }

    fun setDriverAvailable(available: Boolean) {
        prefs.edit().putBoolean(KEY_IS_AVAILABLE, available).apply()
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
