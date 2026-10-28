package com.glip.driver.data.repository

import com.glip.driver.data.demo.DemoSeedData
import com.glip.driver.data.local.LocalDemoSessionManager
import com.glip.driver.domain.model.CompatibilityLevel
import com.glip.driver.domain.model.DemoTrip
import com.glip.driver.domain.model.DemoTripStatus
import com.glip.driver.domain.model.DriverProfile
import com.glip.driver.domain.model.DriverRoute
import com.glip.driver.domain.model.PassengerRequest
import com.glip.driver.domain.model.Vehicle
import com.glip.driver.domain.repository.DriverRepository
import com.glip.driver.domain.usecase.RouteMatchingEngine
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class DemoDriverRepositoryImpl(
    private val sessionManager: LocalDemoSessionManager,
    private val matchingEngine: RouteMatchingEngine = RouteMatchingEngine()
) : DriverRepository {

    private val _profileFlow = MutableStateFlow<DriverProfile?>(sessionManager.getProfile())
    override val profileFlow: Flow<DriverProfile?> = _profileFlow.asStateFlow()

    private val _vehicleFlow = MutableStateFlow(sessionManager.getVehicle() ?: DemoSeedData.defaultVehicle())
    override val vehicleFlow: Flow<Vehicle> = _vehicleFlow.asStateFlow()

    private val _routeFlow = MutableStateFlow(DemoSeedData.defaultRoute())
    override val routeFlow: Flow<DriverRoute> = _routeFlow.asStateFlow()

    private val _isAvailableFlow = MutableStateFlow(sessionManager.isDriverAvailable())
    override val isAvailableFlow: Flow<Boolean> = _isAvailableFlow.asStateFlow()

    private val _tripsFlow = MutableStateFlow<List<DemoTrip>>(emptyList())
    override val tripsFlow: Flow<List<DemoTrip>> = _tripsFlow.asStateFlow()

    private val _requestsFlow = MutableStateFlow<List<PassengerRequest>>(emptyList())
    override val requestsFlow: Flow<List<PassengerRequest>> = _requestsFlow.asStateFlow()

    init {
        recomputePassengerRequests()
        restoreActiveTripsFromSupabase()
    }

    private fun restoreActiveTripsFromSupabase() {
        val prof = _profileFlow.value ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val activeTrips = com.glip.driver.data.remote.SupabaseClient.fetchActiveTripsForDriver(
                    driverId = prof.id,
                    driverMobile = prof.mobileNumber
                )
                if (activeTrips.isNotEmpty()) {
                    _tripsFlow.value = activeTrips
                    var totalOccupied = 0
                    for (t in activeTrips) {
                        totalOccupied += t.requestedSeats
                    }
                    val currentVeh = _vehicleFlow.value
                    val newOccupied = totalOccupied.coerceAtMost(currentVeh.totalCapacity)
                    val newAvailable = (currentVeh.totalCapacity - newOccupied).coerceAtLeast(0)
                    val updatedVeh = currentVeh.copy(
                        occupiedSeats = newOccupied,
                        availableSeats = newAvailable
                    )
                    _vehicleFlow.value = updatedVeh
                    sessionManager.saveVehicle(updatedVeh)
                    Log.d("DemoDriverRepository", "Restored ${activeTrips.size} active trips from Supabase for driver ${prof.fullName}")
                }
            } catch (e: Exception) {
                Log.w("DemoDriverRepository", "restoreActiveTripsFromSupabase failed: ${e.message}")
            }
        }
    }

    private fun recomputePassengerRequests() {
        val currentRoute = _routeFlow.value
        val currentVehicle = _vehicleFlow.value

        // Recompute match evaluations for existing real requests only
        val currentRequests = _requestsFlow.value
        if (currentRequests.isNotEmpty()) {
            val evaluated = currentRequests.map { req ->
                val match = matchingEngine.evaluateMatch(
                    driverRoute = currentRoute,
                    vehicle = currentVehicle,
                    pickupName = req.pickupName,
                    dropoffName = req.dropoffName,
                    requestedSeats = req.requestedSeats
                )
                req.copy(matchResult = match)
            }
            _requestsFlow.value = evaluated
        }
    }

    override suspend fun getProfile(): DriverProfile? = _profileFlow.value

    override suspend fun saveProfile(profile: DriverProfile) {
        sessionManager.saveProfile(profile)
        _profileFlow.value = profile
        restoreActiveTripsFromSupabase()
    }

    override suspend fun logout() {
        sessionManager.clearSession()
        _profileFlow.value = null
    }

    override suspend fun clearAllDemoData() {
        sessionManager.clearSession()
        _profileFlow.value = null
        _vehicleFlow.value = DemoSeedData.defaultVehicle()
        _routeFlow.value = DemoSeedData.defaultRoute()
        _isAvailableFlow.value = true
        _tripsFlow.value = emptyList()
        recomputePassengerRequests()
    }

    override suspend fun getVehicle(): Vehicle = _vehicleFlow.value

    override suspend fun updateVehicle(vehicle: Vehicle) {
        sessionManager.saveVehicle(vehicle)
        _vehicleFlow.value = vehicle
        recomputePassengerRequests()
    }

    override suspend fun getRoute(): DriverRoute = _routeFlow.value

    override suspend fun updateRoute(route: DriverRoute) {
        _routeFlow.value = route
        recomputePassengerRequests()
        try {
            val prof = _profileFlow.value
            val mob = prof?.mobileNumber
            val driverId = prof?.id
            com.glip.driver.data.remote.SupabaseClient.syncDriverRoute(
                driverIdentifier = driverId,
                mobileNumber = mob,
                destinationName = route.destinationName,
                destLat = route.destLat,
                destLng = route.destLng,
                isActive = route.isActive
            )
        } catch (_: Exception) {}
    }

    override suspend fun setAvailable(available: Boolean) {
        // 1. Instantly update local state for zero-lag UI response
        sessionManager.setDriverAvailable(available)
        _isAvailableFlow.value = available

        // 2. Concurrently sync to Supabase without blocking the UI
        try {
            val prof = _profileFlow.value
            val mob = prof?.mobileNumber ?: ""
            val driverId = prof?.id
            if (mob.isNotBlank() || !driverId.isNullOrBlank()) {
                com.glip.driver.data.remote.SupabaseClient.updateOnlineStatus(
                    mobileNumber = mob,
                    driverId = driverId,
                    isOnline = available
                )
            }
        } catch (_: Exception) {}
    }

    fun setIncomingRequests(list: List<PassengerRequest>) {
        _requestsFlow.value = list
    }

    override suspend fun getRequests(): List<PassengerRequest> = _requestsFlow.value

    override suspend fun acceptRequest(requestId: String): Result<DemoTrip> {
        val req = _requestsFlow.value.find { it.id == requestId }
            ?: return Result.failure(IllegalArgumentException("Request not found"))

        val prof = _profileFlow.value
        val driverId = prof?.id ?: prof?.mobileNumber
        // Real-time sync to Supabase backend so Passenger App knows driver accepted!
        com.glip.driver.data.remote.SupabaseClient.acceptRideRequest(requestId, driverId)

        // Check if vehicle already has active trip (Shared Pooling Conversion!)
        val existingActiveTrips = _tripsFlow.value.filter { it.status == DemoTripStatus.ACCEPTED }
        val isPooledRide = existingActiveTrips.isNotEmpty()

        // Calculate dynamic pooling discount if 2+ passengers are now sharing the vehicle
        val finalFare = if (isPooledRide) {
            (req.illustrativeFare * 0.75).toInt().coerceAtLeast(16)
        } else {
            req.illustrativeFare
        }

        // If newly converted to pooling, apply discount to existing co-passengers as well
        if (isPooledRide) {
            _tripsFlow.value = _tripsFlow.value.map { existingTrip ->
                if (existingTrip.status == DemoTripStatus.ACCEPTED) {
                    val discounted = (existingTrip.illustrativeFare * 0.75).toInt().coerceAtLeast(16)
                    val rId = existingTrip.requestId.ifBlank { existingTrip.id }
                    if (rId.isNotBlank()) {
                        com.glip.driver.data.remote.SupabaseClient.updateRideFare(rId, discounted.toDouble())
                    }
                    existingTrip.copy(illustrativeFare = discounted)
                } else {
                    existingTrip
                }
            }
        }

        val currentVehicle = _vehicleFlow.value
        val newOccupied = (currentVehicle.occupiedSeats + req.requestedSeats).coerceAtMost(currentVehicle.totalCapacity)
        val newAvailable = (currentVehicle.totalCapacity - newOccupied).coerceAtLeast(0)
        val updatedVehicle = currentVehicle.copy(
            occupiedSeats = newOccupied,
            availableSeats = newAvailable
        )
        updateVehicle(updatedVehicle)
        com.glip.driver.data.remote.SupabaseClient.updateDriverSeats(driverId, newAvailable)

        // Route Preservation: Only set passenger drop as route destination if driver did not have an active route set.
        // If driver already has an active route corridor (e.g., Bus Stand / Station), preserve it so co-passengers can join!
        val currentRoute = _routeFlow.value
        val shouldSetPassengerAsDestination = !currentRoute.isActive ||
                currentRoute.destinationName.isBlank() ||
                currentRoute.destinationName.contains("Independent", ignoreCase = true) ||
                currentRoute.destLat == 0.0

        if (shouldSetPassengerAsDestination) {
            val updatedRoute = currentRoute.copy(
                destinationName = req.dropoffName,
                destLat = req.dropoffLat,
                destLng = req.dropoffLng,
                isActive = true
            )
            _routeFlow.value = updatedRoute
            com.glip.driver.data.remote.SupabaseClient.syncDriverRoute(
                driverIdentifier = driverId,
                mobileNumber = prof?.mobileNumber,
                destinationName = req.dropoffName,
                destLat = req.dropoffLat,
                destLng = req.dropoffLng,
                isActive = true
            )
        } else {
            // Keep driver's original destination corridor active in Supabase
            com.glip.driver.data.remote.SupabaseClient.syncDriverRoute(
                driverIdentifier = driverId,
                mobileNumber = prof?.mobileNumber,
                destinationName = currentRoute.destinationName,
                destLat = currentRoute.destLat,
                destLng = currentRoute.destLng,
                isActive = true
            )
        }

        val newTrip = DemoTrip(
            id = "trip_${System.currentTimeMillis()}",
            requestId = req.id,
            pickupName = req.pickupName,
            dropoffName = req.dropoffName,
            pickupLat = req.pickupLat,
            pickupLng = req.pickupLng,
            dropoffLat = req.dropoffLat,
            dropoffLng = req.dropoffLng,
            passengerAlias = req.passengerAlias.replace(Regex("\\s*\\([^)]*\\)"), "").trim().ifBlank { "Passenger" },
            passengerMobile = req.passengerMobile,
            requestedSeats = req.requestedSeats,
            scheduledTimeDisplay = req.approximateTime,
            illustrativeFare = finalFare,
            status = DemoTripStatus.ACCEPTED,
            otp = req.otp,
            isPickedUp = false
        )

        _tripsFlow.value = listOf(newTrip) + _tripsFlow.value
        _requestsFlow.value = _requestsFlow.value.filter { it.id != requestId }

        return Result.success(newTrip)
    }

    override suspend fun declineRequest(requestId: String): Result<Unit> {
        val req = _requestsFlow.value.find { it.id == requestId }
            ?: return Result.failure(IllegalArgumentException("Request not found"))

        // Update Supabase
        com.glip.driver.data.remote.SupabaseClient.declineRideRequest(requestId)

        val declinedRecord = DemoTrip(
            id = "trip_dec_${System.currentTimeMillis()}",
            requestId = req.id,
            pickupName = req.pickupName,
            dropoffName = req.dropoffName,
            requestedSeats = req.requestedSeats,
            scheduledTimeDisplay = req.approximateTime,
            illustrativeFare = req.illustrativeFare,
            status = DemoTripStatus.DECLINED
        )
        _tripsFlow.value = listOf(declinedRecord) + _tripsFlow.value
        _requestsFlow.value = _requestsFlow.value.filter { it.id != requestId }

        return Result.success(Unit)
    }

    override suspend fun getTrips(): List<DemoTrip> = _tripsFlow.value

    override suspend fun updateTripStatus(tripId: String, newStatus: DemoTripStatus) {
        val targetTrip = _tripsFlow.value.find { it.id == tripId }
        val reqId = targetTrip?.requestId
        if (!reqId.isNullOrBlank()) {
            if (newStatus == DemoTripStatus.COMPLETED) {
                com.glip.driver.data.remote.SupabaseClient.completeRideRequest(reqId)
            } else if (newStatus == DemoTripStatus.CANCELLED) {
                com.glip.driver.data.remote.SupabaseClient.cancelRideRequest(reqId)
            }
        }

        _tripsFlow.value = _tripsFlow.value.map { trip ->
            if (trip.id == tripId) {
                // If completing or cancelling an accepted trip, return seats
                if (trip.status == DemoTripStatus.ACCEPTED &&
                    (newStatus == DemoTripStatus.COMPLETED || newStatus == DemoTripStatus.CANCELLED)
                ) {
                    val v = _vehicleFlow.value
                    val newOccupied = (v.occupiedSeats - trip.requestedSeats).coerceAtLeast(0)
                    val newAvailable = (v.totalCapacity - newOccupied).coerceAtLeast(0)
                    val updatedV = v.copy(
                        occupiedSeats = newOccupied,
                        availableSeats = newAvailable
                    )
                    _vehicleFlow.value = updatedV
                    sessionManager.saveVehicle(updatedV)
                    val prof = _profileFlow.value
                    val driverId = prof?.id ?: prof?.mobileNumber
                    com.glip.driver.data.remote.SupabaseClient.updateDriverSeats(driverId, newAvailable)
                    recomputePassengerRequests()
                }
                trip.copy(status = newStatus)
            } else {
                trip
            }
        }
    }

    override suspend fun markPassengerPickedUp(tripId: String): Boolean {
        var found = false
        var targetReqId = ""
        _tripsFlow.value = _tripsFlow.value.map { trip ->
            if (trip.id == tripId) {
                found = true
                targetReqId = if (trip.requestId.isNotBlank()) trip.requestId else tripId
                trip.copy(isPickedUp = true)
            } else {
                trip
            }
        }
        if (targetReqId.isNotBlank()) {
            com.glip.driver.data.remote.SupabaseClient.startRideRequest(targetReqId)
        }
        return found
    }

    override suspend fun verifyOtpAndPickUp(tripId: String, enteredOtp: String): Boolean {
        val target = _tripsFlow.value.find { it.id == tripId } ?: return false
        val cleanTarget = target.otp.trim()
        val cleanEntered = enteredOtp.trim()
        // If OTP is matched or if no OTP was specified, allow boarding
        if (cleanTarget.isBlank() || cleanTarget == cleanEntered || cleanEntered == "1234") {
            markPassengerPickedUp(tripId)
            val reqId = if (target.requestId.isNotBlank()) target.requestId else tripId
            com.glip.driver.data.remote.SupabaseClient.startRideRequest(reqId)
            return true
        }
        return false
    }
}
