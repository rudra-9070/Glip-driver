package com.glip.driver.domain.model

enum class VehicleCategory(val displayName: String, val defaultCapacity: Int, val perKmRate: Int) {
    BIKE("Bike", 1, 16),
    E_RICKSHAW("E - Rickshaw", 5, 22),
    AUTO_RICKSHAW("Auto", 7, 27),
    CAR("CAB", 4, 31),
    VAN("Van", 7, 35),
    MINI_BUS("Mini bus", 20, 40),
    OTHER("Other Vehicle", 4, 25)
}

data class Vehicle(
    val id: String = "veh_demo_101",
    val displayName: String,
    val category: VehicleCategory,
    val registrationNumberDisplay: String = "DL-01-DEMO-99", // Clearly marked demo
    val totalCapacity: Int,
    val occupiedSeats: Int,
    val availableSeats: Int = (totalCapacity - occupiedSeats).coerceAtLeast(0)
) {
    init {
        require(totalCapacity > 0) { "Capacity must be positive." }
        require(occupiedSeats >= 0) { "Occupied seats cannot be negative." }
        require(occupiedSeats <= totalCapacity) { "Occupied seats cannot exceed total capacity." }
    }
}
