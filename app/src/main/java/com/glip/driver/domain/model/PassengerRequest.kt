package com.glip.driver.domain.model

enum class CompatibilityLevel(val displayName: String) {
    GOOD_MATCH("Good Demo Match"),
    PARTIAL_MATCH("Partial Route Match"),
    NOT_COMPATIBLE("Not Compatible")
}

data class RouteMatchResult(
    val level: CompatibilityLevel,
    val explanation: String,
    val estimatedDetourMinutes: Int? = null, // illustrative estimate
    val isCapacityAvailable: Boolean = true
)

data class PassengerRequest(
    val id: String,
    val passengerAlias: String = "Demo Passenger",
    val pickupName: String,
    val dropoffName: String,
    val pickupLat: Double,
    val pickupLng: Double,
    val dropoffLat: Double,
    val dropoffLng: Double,
    val requestedSeats: Int,
    val approximateTime: String,
    val illustrativeFare: Int, // Demo INR
    val matchResult: RouteMatchResult
)
