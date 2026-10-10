package com.glip.driver.domain.model

enum class ServiceType(val title: String, val subtitle: String, val isAvailable: Boolean) {
    RIDE_POOLING(
        "Local Route Pooling",
        "Share unoccupied seats on your planned daily route",
        true
    ),
    PARCEL_DELIVERY(
        "Parcel Delivery",
        "Deliver lightweight local packages along your route",
        false
    ),
    SCHOOL_TRANSPORT(
        "School Transportation",
        "Dedicated daily student pooling with route verification",
        false
    )
}
