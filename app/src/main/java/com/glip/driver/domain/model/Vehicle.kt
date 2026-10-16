package com.glip.driver.domain.model

enum class VehicleCategory(val displayName: String, val defaultCapacity: Int) {
    AUTO_RICKSHAW("Auto-rickshaw", 3),
    E_RICKSHAW("E-rickshaw", 4),
    CAR("Car / Hatchback / Sedan", 4),
    VAN("Van / MPV", 6),
    MINI_BUS("Mini-bus / Shuttle", 12),
    OTHER("Other Local Vehicle", 4)
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
