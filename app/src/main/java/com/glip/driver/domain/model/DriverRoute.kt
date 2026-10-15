package com.glip.driver.domain.model

data class RouteStop(
    val id: String,
    val name: String,
    val orderIndex: Int,
    val latitude: Double,
    val longitude: Double,
    val isPickupOrDrop: Boolean = false
)

enum class RouteDirection(val label: String) {
    NORTHBOUND("Northbound"),
    SOUTHBOUND("Southbound"),
    EASTBOUND("Eastbound"),
    WESTBOUND("Westbound"),
    CIRCULAR("Loop / Circular")
}

data class DriverRoute(
    val id: String = "rte_demo_01",
    val originName: String,
    val destinationName: String,
    val intermediateStops: List<RouteStop> = emptyList(),
    val departureTimeDisplay: String = "08:30 AM",
    val direction: RouteDirection = RouteDirection.NORTHBOUND,
    val originLat: Double = 28.6139,
    val originLng: Double = 77.2090,
    val destLat: Double = 28.5355,
    val destLng: Double = 77.3910,
    val isActive: Boolean = false
) {
    val fullStopSequence: List<String>
        get() = listOf(originName) + intermediateStops.sortedBy { it.orderIndex }.map { it.name } + listOf(destinationName)
}
