package com.glip.driver.data.remote

import android.util.Log
import com.glip.driver.domain.model.DriverProfile
import com.glip.driver.domain.model.DriverStatus
import com.glip.driver.domain.model.Vehicle
import com.glip.driver.domain.model.PassengerRequest
import com.glip.driver.domain.model.RouteMatchResult
import com.glip.driver.domain.model.CompatibilityLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Native, lightweight Supabase PostgREST client for Glip Driver.
 * Performs direct, secure REST requests to the user's Supabase backend
 * without requiring heavy external dependencies.
 */
object SupabaseClient {

    private const val TAG = "SupabaseClient"

    // Project URL and Anon Public Key provided by the user
    const val SUPABASE_URL = "https://prolxgzzgsnznhvatnye.supabase.co"
    const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InByb2x4Z3p6Z3Nuem5odmF0bnllIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTIwOTcwMDAsImV4cCI6MjEwNzY3MzAwMH0.P5LwxLaB7W7yOwll-4Sp53n79NaX2FnnrXWsnH3cpyY"

    private const val REST_BASE = "$SUPABASE_URL/rest/v1"

    private fun getIsoTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    private fun openConnection(endpoint: String, method: String): HttpURLConnection {
        val url = URL("$REST_BASE$endpoint")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.setRequestProperty("apikey", SUPABASE_ANON_KEY)
        conn.setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Accept", "application/json")
        return conn
    }

    fun isValidUuid(str: String?): Boolean {
        if (str.isNullOrBlank()) return false
        return try {
            java.util.UUID.fromString(str)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Upsert driver profile to Supabase `drivers` table on register or login.
     */
    suspend fun upsertDriverProfile(
        profile: DriverProfile,
        vehicle: Vehicle? = null,
        isOnline: Boolean = true
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val endpoint = "/drivers?on_conflict=mobile_number"
            val conn = openConnection(endpoint, "POST")
            conn.setRequestProperty("Prefer", "resolution=merge-duplicates,return=representation")
            conn.doOutput = true

            val json = JSONObject().apply {
                put("full_name", profile.fullName)
                put("mobile_number", profile.mobileNumber)
                if (vehicle != null) {
                    put("vehicle_number", vehicle.registrationNumberDisplay)
                    put("vehicle_model", vehicle.displayName)
                    put("vehicle_type", vehicle.category.name)
                    put("total_seats", vehicle.totalCapacity)
                    put("available_seats", vehicle.totalCapacity - vehicle.occupiedSeats)
                }
                put("is_online", isOnline)
                put("last_updated_at", getIsoTimestamp())
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(json.toString())
                writer.flush()
            }

            val code = conn.responseCode
            Log.d(TAG, "upsertDriverProfile response code: $code")
            code in 200..299
        } catch (e: Exception) {
            Log.w(TAG, "upsertDriverProfile failed: ${e.message}")
            false
        }
    }

    /**
     * Look up existing driver by mobile number for account recovery ("I already have an account").
     */
    suspend fun getDriverByMobile(mobileNumber: String): DriverProfile? = withContext(Dispatchers.IO) {
        try {
            val endpoint = "/drivers?mobile_number=eq.$mobileNumber&select=*"
            val conn = openConnection(endpoint, "GET")
            val code = conn.responseCode
            if (code in 200..299) {
                val response = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val arr = JSONArray(response)
                if (arr.length() > 0) {
                    val obj = arr.getJSONObject(0)
                    val name = obj.optString("full_name", "Glip Driver")
                    val id = obj.optString("id", "drv_${mobileNumber.takeLast(4)}")
                    return@withContext DriverProfile(
                        id = id,
                        fullName = name,
                        mobileNumber = mobileNumber,
                        email = null,
                        status = DriverStatus.DEMO_ACTIVE
                    )
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "getDriverByMobile failed: ${e.message}")
            null
        }
    }

    /**
     * Push real-time GPS coordinates and compass bearing to Supabase.
     * Consumer/Passenger App listens to this table in real-time to show live vehicle movement!
     */
    suspend fun updateLiveLocation(
        mobileNumber: String,
        lat: Double,
        lng: Double,
        bearing: Float,
        isOnline: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        if (mobileNumber.isBlank() || lat == 0.0 || lng == 0.0) return@withContext false
        try {
            val endpoint = "/drivers?mobile_number=eq.$mobileNumber"
            val conn = openConnection(endpoint, "PATCH")
            conn.doOutput = true

            val json = JSONObject().apply {
                put("current_lat", lat)
                put("current_lng", lng)
                put("bearing", bearing)
                put("is_online", isOnline)
                put("last_updated_at", getIsoTimestamp())
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(json.toString())
                writer.flush()
            }

            val code = conn.responseCode
            code in 200..299
        } catch (e: Exception) {
            Log.w(TAG, "updateLiveLocation failed: ${e.message}")
            false
        }
    }

    /**
     * Sync active/saved destination route to Supabase `driver_routes`.
     */
    suspend fun syncDriverRoute(
        driverIdentifier: String?,
        destinationName: String,
        destLat: Double,
        destLng: Double,
        isActive: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        if (destinationName.isBlank() || destLat == 0.0 || destLng == 0.0) return@withContext false
        try {
            val endpoint = "/driver_routes"
            val conn = openConnection(endpoint, "POST")
            conn.setRequestProperty("Prefer", "resolution=merge-duplicates")
            conn.doOutput = true

            val json = JSONObject().apply {
                if (!driverIdentifier.isNullOrBlank() && isValidUuid(driverIdentifier)) {
                    put("driver_id", driverIdentifier)
                }
                put("origin_name", "Current Location")
                put("destination_name", destinationName)
                put("dest_lat", destLat)
                put("dest_lng", destLng)
                put("is_active", isActive)
                put("created_at", getIsoTimestamp())
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(json.toString())
                writer.flush()
            }

            val code = conn.responseCode
            code in 200..299
        } catch (e: Exception) {
            Log.w(TAG, "syncDriverRoute failed: ${e.message}")
            false
        }
    }

    /**
     * Fetch real-time pending ride requests submitted by passengers.
     */
    suspend fun fetchPendingRideRequests(driverIdentifier: String?): List<PassengerRequest> = withContext(Dispatchers.IO) {
        val list = mutableListOf<PassengerRequest>()
        try {
            val endpoint = "/ride_requests?status=in.(PENDING,requested)&order=created_at.desc&limit=10"
            val conn = openConnection(endpoint, "GET")
            val code = conn.responseCode
            if (code in 200..299) {
                val response = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val arr = JSONArray(response)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val assignedDriver = obj.optString("driver_id", "")
                    if (assignedDriver.isNotBlank() && !driverIdentifier.isNullOrBlank() && assignedDriver != driverIdentifier) {
                        continue
                    }
                    val id = obj.optString("id")
                    val passName = obj.optString("passenger_name", "Passenger")
                    val passMobile = obj.optString("passenger_mobile", "")
                    val pickup = obj.optString("pickup_name", "")
                    val drop = obj.optString("drop_name", "")
                    val pLat = obj.optDouble("pickup_lat", 0.0)
                    val pLng = obj.optDouble("pickup_lng", 0.0)
                    val dLat = obj.optDouble("drop_lat", 0.0)
                    val dLng = obj.optDouble("drop_lng", 0.0)
                    val seats = obj.optInt("requested_seats", 1)
                    val fare = obj.optDouble("fare_amount", 0.0).toInt().coerceAtLeast(30)

                    list.add(
                        PassengerRequest(
                            id = id,
                            passengerAlias = passName,
                            passengerMobile = passMobile,
                            pickupName = pickup,
                            dropoffName = drop,
                            pickupLat = pLat,
                            pickupLng = pLng,
                            dropoffLat = dLat,
                            dropoffLng = dLng,
                            requestedSeats = seats,
                            approximateTime = "Just now",
                            illustrativeFare = fare,
                            matchResult = RouteMatchResult(
                                level = CompatibilityLevel.GOOD_MATCH,
                                explanation = "Passenger along your route corridor",
                                isCapacityAvailable = true
                            )
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "fetchPendingRideRequests error: ${e.message}")
        }
        list
    }

    /**
     * Mark a ride request as ACCEPTED by driver in Supabase.
     */
    suspend fun acceptRideRequest(requestId: String, driverIdentifier: String?): Boolean = withContext(Dispatchers.IO) {
        try {
            val endpoint = "/ride_requests?id=eq.$requestId"
            val conn = openConnection(endpoint, "PATCH")
            conn.doOutput = true

            val json = JSONObject().apply {
                put("status", "ACCEPTED")
                if (!driverIdentifier.isNullOrBlank() && isValidUuid(driverIdentifier)) {
                    put("driver_id", driverIdentifier)
                }
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(json.toString())
                writer.flush()
            }
            conn.responseCode in 200..299
        } catch (e: Exception) {
            Log.w(TAG, "acceptRideRequest error: ${e.message}")
            false
        }
    }

    /**
     * Mark a ride request as DECLINED by driver in Supabase.
     */
    suspend fun declineRideRequest(requestId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val endpoint = "/ride_requests?id=eq.$requestId"
            val conn = openConnection(endpoint, "PATCH")
            conn.doOutput = true

            val json = JSONObject().apply {
                put("status", "DECLINED")
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(json.toString())
                writer.flush()
            }
            conn.responseCode in 200..299
        } catch (e: Exception) {
            Log.w(TAG, "declineRideRequest error: ${e.message}")
            false
        }
    }
}

