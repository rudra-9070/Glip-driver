package com.glip.driver.domain.model

enum class DemoTripStatus(val label: String) {
    ACCEPTED("Demo Accepted"),
    DECLINED("Demo Declined"),
    COMPLETED("Demo Completed"),
    CANCELLED("Demo Cancelled")
}

data class DemoTrip(
    val id: String,
    val requestId: String,
    val pickupName: String,
    val dropoffName: String,
    val pickupLat: Double = 0.0,
    val pickupLng: Double = 0.0,
    val dropoffLat: Double = 0.0,
    val dropoffLng: Double = 0.0,
    val passengerAlias: String = "Passenger",
    val passengerMobile: String = "",
    val requestedSeats: Int,
    val scheduledTimeDisplay: String,
    val illustrativeFare: Int,
    val status: DemoTripStatus,
    val timestamp: Long = System.currentTimeMillis(),
    val otp: String = "",
    val isPickedUp: Boolean = false
)
