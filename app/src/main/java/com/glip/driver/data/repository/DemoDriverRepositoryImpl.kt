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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class DemoDriverRepositoryImpl(
    private val sessionManager: LocalDemoSessionManager,
    private val matchingEngine: RouteMatchingEngine = RouteMatchingEngine()
) : DriverRepository {

    private val _profileFlow = MutableStateFlow<DriverProfile?>(sessionManager.getProfile())
    override val profileFlow: Flow<DriverProfile?> = _profileFlow.asStateFlow()

    private val _vehicleFlow = MutableStateFlow(DemoSeedData.defaultVehicle())
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
        _vehicleFlow.value = vehicle
        recomputePassengerRequests()
    }

    override suspend fun getRoute(): DriverRoute = _routeFlow.value

    override suspend fun updateRoute(route: DriverRoute) {
        _routeFlow.value = route
        recomputePassengerRequests()
    }

    override suspend fun setAvailable(available: Boolean) {
        sessionManager.setDriverAvailable(available)
        _isAvailableFlow.value = available
    }

    override suspend fun getRequests(): List<PassengerRequest> = _requestsFlow.value

    override suspend fun acceptRequest(requestId: String): Result<DemoTrip> {
        val req = _requestsFlow.value.find { it.id == requestId }
            ?: return Result.failure(IllegalArgumentException("Request not found"))

        if (req.matchResult.level == CompatibilityLevel.NOT_COMPATIBLE) {
            return Result.failure(IllegalStateException("Cannot accept an incompatible route request"))
        }

        val currentVehicle = _vehicleFlow.value
        if (req.requestedSeats > currentVehicle.availableSeats) {
            return Result.failure(IllegalStateException("Not enough seats available"))
        }

        // Update vehicle occupied seats locally for demo
        val updatedVehicle = currentVehicle.copy(
            occupiedSeats = currentVehicle.occupiedSeats + req.requestedSeats
        )
        updateVehicle(updatedVehicle)

        val newTrip = DemoTrip(
            id = "trip_${System.currentTimeMillis()}",
            requestId = req.id,
            pickupName = req.pickupName,
            dropoffName = req.dropoffName,
            requestedSeats = req.requestedSeats,
            scheduledTimeDisplay = req.approximateTime,
            illustrativeFare = req.illustrativeFare,
            status = DemoTripStatus.ACCEPTED
        )

        _tripsFlow.value = listOf(newTrip) + _tripsFlow.value
        // Remove from pending requests
        _requestsFlow.value = _requestsFlow.value.filter { it.id != requestId }

        return Result.success(newTrip)
    }

    override suspend fun declineRequest(requestId: String): Result<Unit> {
        val req = _requestsFlow.value.find { it.id == requestId }
            ?: return Result.failure(IllegalArgumentException("Request not found"))

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
        _tripsFlow.value = _tripsFlow.value.map { trip ->
            if (trip.id == tripId) {
                // If completing or cancelling an accepted trip, return seats
                if (trip.status == DemoTripStatus.ACCEPTED &&
                    (newStatus == DemoTripStatus.COMPLETED || newStatus == DemoTripStatus.CANCELLED)
                ) {
                    val v = _vehicleFlow.value
                    val newOccupied = (v.occupiedSeats - trip.requestedSeats).coerceAtLeast(0)
                    _vehicleFlow.value = v.copy(occupiedSeats = newOccupied)
                    recomputePassengerRequests()
                }
                trip.copy(status = newStatus)
            } else {
                trip
            }
        }
    }
}
