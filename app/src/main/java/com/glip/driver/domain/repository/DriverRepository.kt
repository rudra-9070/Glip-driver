package com.glip.driver.domain.repository

import com.glip.driver.domain.model.DemoTrip
import com.glip.driver.domain.model.DemoTripStatus
import com.glip.driver.domain.model.DriverProfile
import com.glip.driver.domain.model.DriverRoute
import com.glip.driver.domain.model.PassengerRequest
import com.glip.driver.domain.model.Vehicle
import kotlinx.coroutines.flow.Flow

/**
 * DriverRepository interface abstraction.
 * Currently backed by DemoDriverRepositoryImpl, future implementations
 * can connect to Supabase or custom REST endpoints without breaking UI.
 */
interface DriverRepository {
    // Session & Profile
    val profileFlow: Flow<DriverProfile?>
    suspend fun getProfile(): DriverProfile?
    suspend fun saveProfile(profile: DriverProfile)
    suspend fun logout()
    suspend fun clearAllDemoData()

    // Vehicle
    val vehicleFlow: Flow<Vehicle>
    suspend fun getVehicle(): Vehicle
    suspend fun updateVehicle(vehicle: Vehicle)

    // Route
    val routeFlow: Flow<DriverRoute>
    suspend fun getRoute(): DriverRoute
    suspend fun updateRoute(route: DriverRoute)

    // Availability
    val isAvailableFlow: Flow<Boolean>
    suspend fun setAvailable(available: Boolean)

    // Simulated Passenger Requests
    val requestsFlow: Flow<List<PassengerRequest>>
    suspend fun getRequests(): List<PassengerRequest>
    suspend fun acceptRequest(requestId: String): Result<DemoTrip>
    suspend fun declineRequest(requestId: String): Result<Unit>

    // Activity / Trip Management
    val tripsFlow: Flow<List<DemoTrip>>
    suspend fun getTrips(): List<DemoTrip>
    suspend fun updateTripStatus(tripId: String, newStatus: DemoTripStatus)
}
