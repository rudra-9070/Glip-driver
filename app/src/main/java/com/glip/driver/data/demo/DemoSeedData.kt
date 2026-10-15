package com.glip.driver.data.demo

import com.glip.driver.domain.model.DriverRoute
import com.glip.driver.domain.model.RouteDirection
import com.glip.driver.domain.model.RouteStop
import com.glip.driver.domain.model.Vehicle
import com.glip.driver.domain.model.VehicleCategory

data class RawDemoRequest(
    val id: String,
    val passengerAlias: String,
    val pickupName: String,
    val dropoffName: String,
    val pickupLat: Double,
    val pickupLng: Double,
    val dropoffLat: Double,
    val dropoffLng: Double,
    val requestedSeats: Int,
    val approximateTime: String,
    val illustrativeFare: Int
)

object DemoSeedData {

    fun defaultVehicle(): Vehicle = Vehicle(
        id = "veh_auto_001",
        displayName = "Green Bajaj RE Auto",
        category = VehicleCategory.AUTO_RICKSHAW,
        registrationNumberDisplay = "DL-01-DEMO-2024",
        totalCapacity = 3,
        occupiedSeats = 0
    )

    fun defaultRoute(): DriverRoute = DriverRoute(
        id = "rte_delhi_corridor_01",
        originName = "Connaught Place (Inner Circle)",
        destinationName = "Noida Sector 18 (Atta Market)",
        intermediateStops = listOf(
            RouteStop("st_1", "Pragati Maidan", 1, 28.6180, 77.2420),
            RouteStop("st_2", "Akshardham Flyover", 2, 28.6127, 77.2773),
            RouteStop("st_3", "Mayur Vihar Phase 1", 3, 28.6045, 77.2946)
        ),
        departureTimeDisplay = "08:45 AM",
        direction = RouteDirection.EASTBOUND,
        originLat = 28.6315,
        originLng = 77.2167,
        destLat = 28.5708,
        destLng = 77.3261,
        isActive = false
    )

    val sampleRawRequests: List<RawDemoRequest> = listOf(
        RawDemoRequest(
            id = "req_101",
            passengerAlias = "Rahul S.",
            pickupName = "Pragati Maidan",
            dropoffName = "Mayur Vihar Phase 1",
            pickupLat = 28.6180,
            pickupLng = 77.2420,
            dropoffLat = 28.6045,
            dropoffLng = 77.2946,
            requestedSeats = 1,
            approximateTime = "08:50 AM",
            illustrativeFare = 65
        ),
        RawDemoRequest(
            id = "req_102",
            passengerAlias = "Pooja & Amit",
            pickupName = "Connaught Place",
            dropoffName = "Noida Sector 18",
            pickupLat = 28.6315,
            pickupLng = 77.2167,
            dropoffLat = 28.5708,
            dropoffLng = 77.3261,
            requestedSeats = 2,
            approximateTime = "08:45 AM",
            illustrativeFare = 140
        ),
        RawDemoRequest(
            id = "req_103",
            passengerAlias = "Sunita V.",
            pickupName = "Akshardham Flyover",
            dropoffName = "Noida Sector 18",
            pickupLat = 28.6127,
            pickupLng = 77.2773,
            dropoffLat = 28.5708,
            dropoffLng = 77.3261,
            requestedSeats = 1,
            approximateTime = "09:00 AM",
            illustrativeFare = 50
        ),
        RawDemoRequest(
            id = "req_104",
            passengerAlias = "Vikram K.",
            pickupName = "Mayur Vihar Phase 1",
            dropoffName = "Connaught Place", // Reverse direction
            pickupLat = 28.6045,
            pickupLng = 77.2946,
            dropoffLat = 28.6315,
            dropoffLng = 77.2167,
            requestedSeats = 1,
            approximateTime = "09:10 AM",
            illustrativeFare = 80
        ),
        RawDemoRequest(
            id = "req_105",
            passengerAlias = "Office Group (4)",
            pickupName = "Connaught Place",
            dropoffName = "Mayur Vihar Phase 1",
            pickupLat = 28.6315,
            pickupLng = 77.2167,
            dropoffLat = 28.6045,
            dropoffLng = 77.2946,
            requestedSeats = 4, // Exceeds auto capacity (3)
            approximateTime = "08:50 AM",
            illustrativeFare = 220
        ),
        RawDemoRequest(
            id = "req_106",
            passengerAlias = "Deepak M.",
            pickupName = "Karol Bagh Metro", // Off-route
            dropoffName = "Rohini Sector 7",
            pickupLat = 28.6517,
            pickupLng = 77.1906,
            dropoffLat = 28.7088,
            dropoffLng = 77.1215,
            requestedSeats = 1,
            approximateTime = "09:30 AM",
            illustrativeFare = 110
        )
    )
}
