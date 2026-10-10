package com.glip.driver.domain.usecase

import com.glip.driver.domain.model.CompatibilityLevel
import com.glip.driver.domain.model.DriverRoute
import com.glip.driver.domain.model.RouteMatchResult
import com.glip.driver.domain.model.Vehicle

/**
 * Route-Based Seat Sharing Matching Logic.
 * This separates matching rules from UI logic so it can be ported directly to a backend.
 */
class RouteMatchingEngine {

    fun evaluateMatch(
        driverRoute: DriverRoute,
        vehicle: Vehicle,
        pickupName: String,
        dropoffName: String,
        requestedSeats: Int
    ): RouteMatchResult {
        // 1. Seat Capacity Check
        if (requestedSeats > vehicle.availableSeats) {
            return RouteMatchResult(
                level = CompatibilityLevel.NOT_COMPATIBLE,
                explanation = "Request asks for $requestedSeats seat(s), but you only have ${vehicle.availableSeats} available seat(s).",
                estimatedDetourMinutes = null,
                isCapacityAvailable = false
            )
        }

        val fullStops = driverRoute.fullStopSequence.map { it.lowercase().trim() }
        val pickupNorm = pickupName.lowercase().trim()
        val dropoffNorm = dropoffName.lowercase().trim()

        val pickupIdx = fullStops.indexOfFirst { it.contains(pickupNorm) || pickupNorm.contains(it) }
        val dropoffIdx = fullStops.indexOfFirst { it.contains(dropoffNorm) || dropoffNorm.contains(it) }

        // Both stops exist along the sequential stops in order
        if (pickupIdx != -1 && dropoffIdx != -1) {
            return if (pickupIdx < dropoffIdx) {
                val isExactEndpoints = (pickupIdx == 0 && dropoffIdx == fullStops.lastIndex)
                RouteMatchResult(
                    level = CompatibilityLevel.GOOD_MATCH,
                    explanation = if (isExactEndpoints) {
                        "Perfect match along complete route direction from origin to destination."
                    } else {
                        "Sequential match: pickup and drop-off align directly along stops with 0 km major diversion."
                    },
                    estimatedDetourMinutes = if (isExactEndpoints) 0 else 3,
                    isCapacityAvailable = true
                )
            } else {
                RouteMatchResult(
                    level = CompatibilityLevel.NOT_COMPATIBLE,
                    explanation = "Opposite travel direction: pickup occurs after destination along driver's planned itinerary.",
                    estimatedDetourMinutes = null,
                    isCapacityAvailable = true
                )
            }
        }

        // Partial match: only pickup OR dropoff is directly on the planned corridor
        if (pickupIdx != -1 || dropoffIdx != -1) {
            val matchedPoint = if (pickupIdx != -1) pickupName else dropoffName
            return RouteMatchResult(
                level = CompatibilityLevel.PARTIAL_MATCH,
                explanation = "Corridor overlap: connects with '$matchedPoint' along your route with ~1.5 km corridor detour.",
                estimatedDetourMinutes = 7,
                isCapacityAvailable = true
            )
        }

        // Incompatible / Outside corridor
        return RouteMatchResult(
            level = CompatibilityLevel.NOT_COMPATIBLE,
            explanation = "Completely off-route: neither pickup nor drop-off aligns with your corridor stops.",
            estimatedDetourMinutes = 24,
            isCapacityAvailable = true
        )
    }
}
