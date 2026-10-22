package com.glip.driver.data.remote

import android.location.Location
import android.util.Log
import com.glip.driver.domain.model.DriverProfile
import com.glip.driver.domain.model.DriverStatus
import com.glip.driver.domain.model.Vehicle
import com.glip.driver.domain.model.VehicleCategory
import com.glip.driver.domain.model.PassengerRequest
import com.glip.driver.domain.model.RouteMatchResult
import com.glip.driver.domain.model.CompatibilityLevel
import com.glip.driver.domain.model.DemoTrip
import com.glip.driver.domain.model.DemoTripStatus
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

    private fun getIsoTimestampMinusSeconds(seconds: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(System.currentTimeMillis() - (seconds * 1000L)))
    }

    private fun isTimestampFresh(isoTimestamp: String, maxAgeSeconds: Long = 90): Boolean {
        if (isoTimestamp.isBlank()) return false
        return try {
            val clean = isoTimestamp.substringBefore('.').substringBefore('+').substringBefore('Z')
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = sdf.parse(clean) ?: return false
            val ageMs = System.currentTimeMillis() - date.time
            ageMs in -5000L..(maxAgeSeconds * 1000L) // Allow small clock skew
        } catch (e: Exception) {
            true
        }
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
        } catch (e: Exception) {
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
                if (!profile.licenseNumber.isNullOrBlank()) {
                    put("license_number", profile.licenseNumber)
                }
                if (!profile.vehiclePlate.isNullOrBlank()) {
                    put("vehicle_plate", profile.vehiclePlate)
                    put("vehicle_number", profile.vehiclePlate)
                }
                if (vehicle != null) {
                    put("vehicle_number", vehicle.registrationNumberDisplay)
                    put("vehicle_plate", vehicle.registrationNumberDisplay)
                    put("vehicle_model", vehicle.displayName)
                    put("vehicle_name", vehicle.displayName)
                    put("vehicle_type", vehicle.category.name)
                    put("total_seats", vehicle.totalCapacity)
                    put("total_capacity", vehicle.totalCapacity)
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
     * Look up existing driver and associated vehicle by mobile number for account recovery ("I already have an account").
     * Returns Pair(profile, vehicle) if found in Supabase `drivers` table, or null if driver does not exist.
     */
    suspend fun getDriverWithVehicleByMobile(mobileNumber: String): Pair<DriverProfile, Vehicle?>? = withContext(Dispatchers.IO) {
        try {
            val cleanMobile = mobileNumber.replace(Regex("[^0-9]"), "").takeLast(10)
            if (cleanMobile.length < 10) return@withContext null
            val endpoint = "/drivers?or=(mobile_number.eq.$cleanMobile,mobile_number.eq.%2B91$cleanMobile,mobile_number.ilike.*$cleanMobile*)&select=*"
            val conn = openConnection(endpoint, "GET")
            val code = conn.responseCode
            if (code in 200..299) {
                val response = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val arr = JSONArray(response)
                if (arr.length() > 0) {
                    val obj = arr.getJSONObject(0)
                    val name = obj.optString("full_name", "Glip Driver")
                    val id = obj.optString("id", "drv_${cleanMobile.takeLast(4)}")
                    val license = obj.optString("license_number", "").takeIf { it.isNotBlank() }
                    val plate = obj.optString("vehicle_plate", obj.optString("vehicle_number", "")).takeIf { it.isNotBlank() }
                    val profile = DriverProfile(
                        id = id,
                        fullName = name,
                        mobileNumber = cleanMobile,
                        email = null,
                        licenseNumber = license,
                        vehiclePlate = plate,
                        status = DriverStatus.DEMO_ACTIVE
                    )

                    val vehicleTypeStr = obj.optString("vehicle_type", "")
                    val vehicleModel = obj.optString("vehicle_model", obj.optString("vehicle_name", ""))
                    val totalCap = obj.optInt("total_capacity", obj.optInt("total_seats", 4))
                    val availSeats = obj.optInt("available_seats", totalCap)
                    val category = try {
                        if (vehicleTypeStr.isNotBlank()) VehicleCategory.valueOf(vehicleTypeStr) else VehicleCategory.CAR
                    } catch (e: Exception) {
                        VehicleCategory.CAR
                    }

                    val vehicle = Vehicle(
                        id = "veh_$id",
                        displayName = vehicleModel.ifBlank { name },
                        category = category,
                        registrationNumberDisplay = plate ?: "CG 08 0000",
                        totalCapacity = if (totalCap > 0) totalCap else category.defaultCapacity,
                        occupiedSeats = 0,
                        availableSeats = if (availSeats > 0) availSeats else (if (totalCap > 0) totalCap else category.defaultCapacity)
                    )

                    return@withContext Pair(profile, vehicle)
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "getDriverWithVehicleByMobile failed: ${e.message}")
            null
        }
    }

    /**
     * Looks up all active rides assigned to this driver in Supabase (status ACCEPTED or IN_PROGRESS).
     * Used on app startup/resume so active orders are never lost after app kill.
     */
    suspend fun fetchActiveTripsForDriver(driverId: String?, driverMobile: String?): List<DemoTrip> = withContext(Dispatchers.IO) {
        val list = mutableListOf<DemoTrip>()
        try {
            val cleanMobile = driverMobile?.replace(Regex("[^0-9]"), "")?.takeLast(10) ?: ""
            val filter = if (!driverId.isNullOrBlank() && cleanMobile.isNotBlank()) {
                "or=(driver_id.eq.$driverId,driver_id.eq.$cleanMobile,driver_id.ilike.*$cleanMobile*)"
            } else if (!driverId.isNullOrBlank()) {
                "driver_id.eq.$driverId"
            } else if (cleanMobile.isNotBlank()) {
                "or=(driver_id.eq.$cleanMobile,driver_id.ilike.*$cleanMobile*)"
            } else {
                return@withContext emptyList()
            }

            val endpoint = "/ride_requests?status=in.(ACCEPTED,IN_PROGRESS)&$filter&order=created_at.desc&limit=5"
            val conn = openConnection(endpoint, "GET")
            val code = conn.responseCode
            if (code in 200..299) {
                val response = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val arr = JSONArray(response)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val reqId = obj.optString("id")
                    val statusStr = obj.optString("status", "ACCEPTED")
                    val isPickedUp = statusStr.equals("IN_PROGRESS", ignoreCase = true)
                    val fare = obj.optDouble("fare_amount", 40.0).toInt()
                    val pName = obj.optString("passenger_name", "Passenger")
                    val pMobile = obj.optString("passenger_mobile", "")
                    val pickup = obj.optString("pickup_name", "Pickup Location")
                    val drop = obj.optString("drop_name", "Drop Location")
                    val pLat = obj.optDouble("pickup_lat", 0.0)
                    val pLng = obj.optDouble("pickup_lng", 0.0)
                    val dLat = obj.optDouble("drop_lat", 0.0)
                    val dLng = obj.optDouble("drop_lng", 0.0)
                    val seats = obj.optInt("requested_seats", 1)
                    val otpVal = obj.optString("otp", "")

                    list.add(
                        DemoTrip(
                            id = reqId,
                            requestId = reqId,
                            pickupName = pickup,
                            dropoffName = drop,
                            pickupLat = pLat,
                            pickupLng = pLng,
                            dropoffLat = dLat,
                            dropoffLng = dLng,
                            passengerAlias = pName,
                            passengerMobile = pMobile,
                            requestedSeats = seats,
                            scheduledTimeDisplay = "Now",
                            illustrativeFare = fare,
                            status = DemoTripStatus.ACCEPTED,
                            otp = otpVal,
                            isPickedUp = isPickedUp
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "fetchActiveTripsForDriver failed: ${e.message}")
        }
        list
    }

    /**
     * Look up existing driver by mobile number for account recovery ("I already have an account").
     */
    suspend fun getDriverByMobile(mobileNumber: String): DriverProfile? {
        return getDriverWithVehicleByMobile(mobileNumber)?.first
    }

    /**
     * Push online/offline status change immediately to Supabase `drivers` table.
     */
    suspend fun updateOnlineStatus(
        mobileNumber: String,
        driverId: String? = null,
        isOnline: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        if (mobileNumber.isBlank() && driverId.isNullOrBlank()) return@withContext false
        try {
            val endpoint = if (!driverId.isNullOrBlank() && isValidUuid(driverId)) {
                "/drivers?id=eq.$driverId"
            } else {
                val enc = java.net.URLEncoder.encode(mobileNumber.trim(), "UTF-8")
                "/drivers?mobile_number=eq.$enc"
            }
            val conn = openConnection(endpoint, "PATCH")
            conn.doOutput = true

            val json = JSONObject().apply {
                put("is_online", isOnline)
                put("last_updated_at", getIsoTimestamp())
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(json.toString())
                writer.flush()
            }
            val code = conn.responseCode
            Log.d(TAG, "updateOnlineStatus code: $code (isOnline=$isOnline)")
            code in 200..299
        } catch (e: Exception) {
            Log.w(TAG, "updateOnlineStatus failed: ${e.message}")
            false
        }
    }

    /**
     * Push real-time GPS coordinates and compass bearing to Supabase.
     * Consumer/Passenger App listens to this table in real-time to show live vehicle movement!
     */
    suspend fun updateLiveLocation(
        mobileNumber: String,
        driverId: String? = null,
        lat: Double,
        lng: Double,
        bearing: Float,
        isOnline: Boolean,
        vehicleCategory: VehicleCategory? = null
    ): Boolean = withContext(Dispatchers.IO) {
        if ((mobileNumber.isBlank() && driverId.isNullOrBlank()) || lat == 0.0 || lng == 0.0) return@withContext false
        try {
            val endpoint = if (!driverId.isNullOrBlank() && isValidUuid(driverId)) {
                "/drivers?id=eq.$driverId"
            } else {
                val enc = java.net.URLEncoder.encode(mobileNumber.trim(), "UTF-8")
                "/drivers?mobile_number=eq.$enc"
            }
            val conn = openConnection(endpoint, "PATCH")
            conn.doOutput = true

            val json = JSONObject().apply {
                put("current_lat", lat)
                put("current_lng", lng)
                put("bearing", bearing)
                put("is_online", isOnline)
                if (vehicleCategory != null) {
                    put("vehicle_type", vehicleCategory.name)
                }
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
        mobileNumber: String? = null,
        destinationName: String,
        destLat: Double,
        destLng: Double,
        isActive: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        if (destinationName.isBlank() || destLat == 0.0 || destLng == 0.0) return@withContext false
        try {
            var uuid = if (!driverIdentifier.isNullOrBlank() && isValidUuid(driverIdentifier)) driverIdentifier else null
            if (uuid == null && !mobileNumber.isNullOrBlank()) {
                val d = getDriverByMobile(mobileNumber)
                if (d != null && isValidUuid(d.id)) {
                    uuid = d.id
                }
            }

            // Clean up any stale routes for this driver first
            if (uuid != null) {
                try {
                    val delConn = openConnection("/driver_routes?driver_id=eq.$uuid", "DELETE")
                    delConn.responseCode
                    delConn.disconnect()
                } catch (e: Exception) {}
            }

            val endpoint = "/driver_routes"
            val conn = openConnection(endpoint, "POST")
            conn.setRequestProperty("Prefer", "return=representation")
            conn.doOutput = true

            val json = JSONObject().apply {
                if (uuid != null) {
                    put("driver_id", uuid)
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

    data class OnlineDriverInfo(
        val id: String,
        val mobileNumber: String,
        val lat: Double,
        val lng: Double,
        val vehicleType: String
    )

    private var cachedOnlineDrivers: List<OnlineDriverInfo> = emptyList()
    private var lastOnlineDriversFetchTime: Long = 0L
    private val localDeclinedRequests = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    fun markLocalDeclined(requestId: String) {
        localDeclinedRequests.add(requestId)
    }

    fun isLocalDeclined(requestId: String): Boolean = localDeclinedRequests.contains(requestId)

    fun getSecondsSince(isoTimestamp: String): Long {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val cleanIso = isoTimestamp.split(".")[0].replace("Z", "")
            val date = sdf.parse(cleanIso) ?: return 0L
            val diffMs = System.currentTimeMillis() - date.time
            (diffMs / 1000L).coerceAtLeast(0L)
        } catch (_: Exception) {
            0L
        }
    }

    /**
     * Fetches online drivers to calculate proximity ranks for waterfall dispatching.
     * Cached for 6 seconds to prevent excessive network calls.
     */
    suspend fun getNearbyOnlineDrivers(): List<OnlineDriverInfo> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (now - lastOnlineDriversFetchTime < 6000L && cachedOnlineDrivers.isNotEmpty()) {
            return@withContext cachedOnlineDrivers
        }
        val list = mutableListOf<OnlineDriverInfo>()
        try {
            val endpoint = "/drivers?is_online=eq.true&select=id,mobile_number,current_lat,current_lng,vehicle_type"
            val conn = openConnection(endpoint, "GET")
            if (conn.responseCode in 200..299) {
                val resp = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val arr = JSONArray(resp)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val lat = obj.optDouble("current_lat", 0.0)
                    val lng = obj.optDouble("current_lng", 0.0)
                    if (lat != 0.0 && lng != 0.0) {
                        list.add(
                            OnlineDriverInfo(
                                id = obj.optString("id", ""),
                                mobileNumber = obj.optString("mobile_number", ""),
                                lat = lat,
                                lng = lng,
                                vehicleType = obj.optString("vehicle_type", "")
                            )
                        )
                    }
                }
                cachedOnlineDrivers = list
                lastOnlineDriversFetchTime = now
            }
        } catch (e: Exception) {
            Log.w(TAG, "getNearbyOnlineDrivers error: ${e.message}")
        }
        list
    }

    /**
     * Fetch real-time pending ride requests submitted by passengers.
     * Matches if assigned to driver's UUID/mobile, OR if open corridor request strictly matching driver's vehicle type.
     * Enforces strict city/proximity radius (<= 15 km) and Rapido/Uber-style Waterfall Cascading Dispatch:
     * - Nearest Driver (Rank 1): Gets request immediately (0s - 90s)
     * - 2nd Nearest (Rank 2): Gets request after 15s if Rank 1 didn't accept
     * - 3rd Nearest (Rank 3): Gets request after 30s
     * - Rank 4+: Gets request after 45s
     */
    suspend fun fetchPendingRideRequests(
        driverUuid: String?,
        driverMobile: String? = null,
        driverVehicleCategory: VehicleCategory? = null,
        driverCapacity: Int? = null,
        driverLat: Double = 0.0,
        driverLng: Double = 0.0
    ): List<PassengerRequest> = withContext(Dispatchers.IO) {
        val list = mutableListOf<PassengerRequest>()
        try {
            // Uber/Rapido real-time filter: ONLY fetch fresh requests created within the last 90 seconds!
            val cutoffIso = getIsoTimestampMinusSeconds(90)
            val endpoint = "/ride_requests?status=in.(PENDING,REQUESTED,pending,requested)&created_at=gte.$cutoffIso&order=created_at.desc&limit=10"
            val conn = openConnection(endpoint, "GET")
            val code = conn.responseCode
            if (code in 200..299) {
                val response = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val arr = JSONArray(response)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val id = obj.optString("id")
                    if (isLocalDeclined(id)) {
                        continue // Driver previously rejected this ride; skip!
                    }

                    val createdAt = obj.optString("created_at", "")
                    if (!isTimestampFresh(createdAt, maxAgeSeconds = 90)) {
                        continue // Discard stale/expired requests from earlier
                    }
                    val assignedDriver = obj.optString("driver_id", "").trim()
                    
                    val isDirectMatch = (!driverUuid.isNullOrBlank() && assignedDriver.equals(driverUuid, ignoreCase = true)) ||
                            (!driverMobile.isNullOrBlank() && assignedDriver.equals(driverMobile, ignoreCase = true))

                    // If assigned to another specific driver, skip!
                    if (!assignedDriver.isBlank() && !isDirectMatch) {
                        continue
                    }

                    val seats = obj.optInt("requested_seats", 1)

                    // Vehicle Category Filter:
                    // Strict separation: Auto gets Auto, E-Rickshaw gets E-Rickshaw, Cab gets Cab
                    if (driverVehicleCategory != null) {
                        val isCategoryMatch = when (driverVehicleCategory) {
                            VehicleCategory.BIKE -> false
                            VehicleCategory.E_RICKSHAW -> seats == 4 || (isDirectMatch && seats <= 4)
                            VehicleCategory.AUTO_RICKSHAW -> seats == 5 || (isDirectMatch && seats <= 5)
                            VehicleCategory.CAR -> seats == 4 || (isDirectMatch && seats <= 4)
                            VehicleCategory.VAN -> seats in 4..7
                            VehicleCategory.MINI_BUS -> seats > 7
                            else -> true
                        }
                        if (!isCategoryMatch) {
                            continue
                        }
                    }

                    val pLat = obj.optDouble("pickup_lat", 0.0)
                    val pLng = obj.optDouble("pickup_lng", 0.0)
                    val dLat = obj.optDouble("drop_lat", 0.0)
                    val dLng = obj.optDouble("drop_lng", 0.0)

                    // Proximity & City Boundary Filter (Max 10 KM):
                    // Prevents rides from another city or far location from reaching this driver!
                    var myDistanceMeters = 0f
                    if (driverLat != 0.0 && driverLng != 0.0 && pLat != 0.0 && pLng != 0.0) {
                        val distArr = FloatArray(1)
                        Location.distanceBetween(driverLat, driverLng, pLat, pLng, distArr)
                        myDistanceMeters = distArr[0]

                        // HARD IMMUTABLE PROXIMITY CHECK:
                        // Never accept or deliver any ride further than 10 KM!
                        // Even if isDirectMatch is true, distant drivers (>10 KM) in other cities are strictly skipped!
                        if (myDistanceMeters > 10_000f) {
                            continue // In a different city or too far away! Skip.
                        }
                    } else if (driverLat == 0.0 || driverLng == 0.0) {
                        // Driver coordinates unknown/unset - do not deliver rides blindly without proximity
                        continue
                    }

                    // Waterfall Cascading Dispatch:
                    // Send to the nearest driver first. If not accepted within 15s, expand to next nearest.
                    if (!isDirectMatch && pLat != 0.0 && pLng != 0.0) {
                        val ageSeconds = getSecondsSince(createdAt)
                        val allOnline = getNearbyOnlineDrivers()
                        val matchingDrivers = allOnline.filter { drv ->
                            if (driverVehicleCategory != null && drv.vehicleType.isNotBlank()) {
                                drv.vehicleType.equals(driverVehicleCategory.name, ignoreCase = true)
                            } else true
                        }.map { drv ->
                            val dArr = FloatArray(1)
                            Location.distanceBetween(drv.lat, drv.lng, pLat, pLng, dArr)
                            Pair(drv, dArr[0])
                        }.filter { it.second <= 10_000f }
                         .sortedBy { it.second }

                        val myRank = matchingDrivers.indexOfFirst { (drv, _) ->
                            (!driverUuid.isNullOrBlank() && drv.id.equals(driverUuid, ignoreCase = true)) ||
                                    (!driverMobile.isNullOrBlank() && drv.mobileNumber.equals(driverMobile, ignoreCase = true))
                        }.let { idx ->
                            if (idx >= 0) idx + 1
                            else {
                                when {
                                    myDistanceMeters <= 2_500f -> 1
                                    myDistanceMeters <= 5_000f -> 2
                                    myDistanceMeters <= 8_000f -> 3
                                    else -> 4
                                }
                            }
                        }

                        val isEligibleNow = when (myRank) {
                            1 -> true
                            2 -> ageSeconds >= 15
                            3 -> ageSeconds >= 30
                            else -> ageSeconds >= 45
                        }

                        if (!isEligibleNow) {
                            continue // Wait for closer driver to decide first
                        }
                    }

                    val rawName = obj.optString("passenger_name", "Passenger")
                    val passName = rawName.replace(Regex("\\s*\\([^)]*\\)"), "").trim().ifBlank { "Passenger" }
                    val passMobile = obj.optString("passenger_mobile", "")
                    val pickup = obj.optString("pickup_name", "")
                    val drop = obj.optString("drop_name", "")
                    val fare = obj.optDouble("fare_amount", 0.0).toInt().coerceAtLeast(16)
                    val otp = obj.optString("otp", "")

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
                            ),
                            otp = otp
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
     * Mark a ride request as IN_PROGRESS (OTP verified, passenger picked up) in Supabase.
     */
    suspend fun startRideRequest(requestId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val endpoint = "/ride_requests?id=eq.$requestId"
            val conn = openConnection(endpoint, "PATCH")
            conn.doOutput = true

            val json = JSONObject().apply {
                put("status", "IN_PROGRESS")
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(json.toString())
                writer.flush()
            }
            conn.responseCode in 200..299
        } catch (e: Exception) {
            Log.w(TAG, "startRideRequest error: ${e.message}")
            false
        }
    }

    /**
     * Update fare_amount in Supabase when a personal ride is converted to pooling.
     */
    suspend fun updateRideFare(requestId: String, newFare: Double): Boolean = withContext(Dispatchers.IO) {
        try {
            val endpoint = "/ride_requests?id=eq.$requestId"
            val conn = openConnection(endpoint, "PATCH")
            conn.doOutput = true

            val json = JSONObject().apply {
                put("fare_amount", newFare)
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(json.toString())
                writer.flush()
            }
            conn.responseCode in 200..299
        } catch (e: Exception) {
            Log.w(TAG, "updateRideFare error: ${e.message}")
            false
        }
    }

    /**
     * Mark a ride request as DECLINED by driver.
     * Open pooling/corridor requests are marked locally so this driver doesn't see them again,
     * but the request stays available in Supabase for the next nearest driver!
     * Direct assignments are updated in Supabase.
     */
    suspend fun declineRideRequest(requestId: String, isDirectAssignment: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        markLocalDeclined(requestId)
        if (!isDirectAssignment) {
            return@withContext true
        }
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

    /**
     * Update available seats count for driver in Supabase drivers table.
     */
    suspend fun updateDriverSeats(driverIdentifier: String?, availableSeats: Int): Boolean = withContext(Dispatchers.IO) {
        if (driverIdentifier.isNullOrBlank()) return@withContext false
        try {
            val filter = if (isValidUuid(driverIdentifier)) "id=eq.$driverIdentifier" else "mobile_number=eq.$driverIdentifier"
            val conn = openConnection("/drivers?$filter", "PATCH")
            conn.doOutput = true

            val json = JSONObject().apply {
                put("available_seats", availableSeats)
                put("last_updated_at", getIsoTimestamp())
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(json.toString())
                writer.flush()
            }
            conn.responseCode in 200..299
        } catch (e: Exception) {
            Log.w(TAG, "updateDriverSeats error: ${e.message}")
            false
        }
    }

    /**
     * Mark a ride request as COMPLETED in Supabase.
     */
    suspend fun completeRideRequest(requestId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val endpoint = "/ride_requests?id=eq.$requestId"
            val conn = openConnection(endpoint, "PATCH")
            conn.doOutput = true

            val json = JSONObject().apply {
                put("status", "COMPLETED")
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(json.toString())
                writer.flush()
            }
            conn.responseCode in 200..299
        } catch (e: Exception) {
            Log.w(TAG, "completeRideRequest error: ${e.message}")
            false
        }
    }

    /**
     * Mark a ride request as CANCELLED in Supabase.
     */
    suspend fun cancelRideRequest(requestId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val endpoint = "/ride_requests?id=eq.$requestId"
            val conn = openConnection(endpoint, "PATCH")
            conn.doOutput = true

            val json = JSONObject().apply {
                put("status", "CANCELLED")
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(json.toString())
                writer.flush()
            }
            conn.responseCode in 200..299
        } catch (e: Exception) {
            Log.w(TAG, "cancelRideRequest error: ${e.message}")
            false
        }
    }

    /**
     * Checks if any active accepted ride request has been CANCELLED by the passenger.
     */
    suspend fun checkCancelledRequests(requestIds: List<String>): Set<String> = withContext(Dispatchers.IO) {
        if (requestIds.isEmpty()) return@withContext emptySet()
        val cancelled = mutableSetOf<String>()
        try {
            val inFilter = requestIds.joinToString(",")
            val endpoint = "/ride_requests?id=in.($inFilter)&status=eq.CANCELLED&select=id"
            val conn = openConnection(endpoint, "GET")
            if (conn.responseCode in 200..299) {
                val resp = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val arr = JSONArray(resp)
                for (i in 0 until arr.length()) {
                    val id = arr.getJSONObject(i).optString("id", "")
                    if (id.isNotBlank()) cancelled.add(id)
                }
            }
        } catch (_: Exception) {}
        cancelled
    }
}

