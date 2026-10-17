package com.glip.driver.domain.model

enum class DriverStatus(val label: String) {
    DEMO_UNVERIFIED("Not Verified"),
    DEMO_ACTIVE("Active"),
    OFFLINE("Offline")
}

data class DriverProfile(
    val id: String = "drv_demo_001",
    val fullName: String,
    val mobileNumber: String,
    val email: String? = null,
    val photoUrl: String? = null,
    val licenseNumber: String? = null,
    val vehiclePlate: String? = null,
    val status: DriverStatus = DriverStatus.DEMO_UNVERIFIED,
    val createdAt: Long = System.currentTimeMillis()
) {
    val maskedMobile: String
        get() {
            val clean = mobileNumber.filter { it.isDigit() }
            return if (clean.length >= 10) {
                val start = clean.take(2)
                val end = clean.takeLast(2)
                "+91 $start******$end"
            } else {
                mobileNumber
            }
        }
}
