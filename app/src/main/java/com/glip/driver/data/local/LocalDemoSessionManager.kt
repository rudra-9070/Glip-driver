package com.glip.driver.data.local

import android.content.Context
import android.content.SharedPreferences
import com.glip.driver.domain.model.DriverProfile
import com.glip.driver.domain.model.DriverStatus
import com.glip.driver.domain.model.Vehicle
import com.glip.driver.domain.model.VehicleCategory

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
        private const val KEY_VEHICLE_ID = "vehicle_id"
        private const val KEY_VEHICLE_NAME = "vehicle_name"
        private const val KEY_VEHICLE_CATEGORY = "vehicle_category"
        private const val KEY_VEHICLE_PLATE = "vehicle_plate"
        private const val KEY_TOTAL_CAPACITY = "total_capacity"
        private const val KEY_AVAILABLE_SEATS = "available_seats"
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

    fun saveVehicle(vehicle: Vehicle) {
        prefs.edit()
            .putString(KEY_VEHICLE_ID, vehicle.id)
            .putString(KEY_VEHICLE_NAME, vehicle.displayName)
            .putString(KEY_VEHICLE_CATEGORY, vehicle.category.name)
            .putString(KEY_VEHICLE_PLATE, vehicle.registrationNumberDisplay)
            .putInt(KEY_TOTAL_CAPACITY, vehicle.totalCapacity)
            .putInt(KEY_AVAILABLE_SEATS, vehicle.availableSeats)
            .apply()
    }

    fun getVehicle(): Vehicle? {
        val catName = prefs.getString(KEY_VEHICLE_CATEGORY, null) ?: return null
        val cat = try { VehicleCategory.valueOf(catName) } catch (_: Exception) { VehicleCategory.CAR }
        val name = prefs.getString(KEY_VEHICLE_NAME, "My Vehicle") ?: "My Vehicle"
        val plate = prefs.getString(KEY_VEHICLE_PLATE, "CG 08 0000") ?: "CG 08 0000"
        val totalCap = prefs.getInt(KEY_TOTAL_CAPACITY, cat.defaultCapacity)
        val avail = prefs.getInt(KEY_AVAILABLE_SEATS, totalCap)
        val id = prefs.getString(KEY_VEHICLE_ID, "veh_local") ?: "veh_local"

        return Vehicle(
            id = id,
            displayName = name,
            category = cat,
            registrationNumberDisplay = plate,
            totalCapacity = totalCap,
            occupiedSeats = (totalCap - avail).coerceAtLeast(0),
            availableSeats = avail
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
