package com.glip.driver.domain.model

enum class VehicleCategory(val displayName: String, val defaultCapacity: Int) {
    BIKE("Bike / Motorcycle", 1),
    AUTO_RICKSHAW("Auto", 5),
    E_RICKSHAW("E-Rickshaw", 4),
    CAR("Car / Hatchback / Sedan", 4),
    VAN("Van", 7),
    MINI_BUS("Mini bus", 20),
    OTHER("Other Vehicle", 4)
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
