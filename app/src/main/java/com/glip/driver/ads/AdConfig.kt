package com.glip.driver.ads

/**
 * Advertisement System Configuration for Glip Driver Android App
 */
object AdConfig {
    /**
     * Base URL for the live Advertisement Management System API.
     * Points to production Vercel deployment.
     */
    var BASE_URL: String = "https://advertisement-management-system.vercel.app"

    // Driver App Fixed-Size (250dp) In-App Ad Container
    const val SLOT_DRIVER_HOME_BANNER = "driver_home_banner" // DriverHomeScreen: Fixed 250dp, Shape 10dp, 1080x750px

    // Ultra-fast live sync refresh interval (2.0 seconds for near-instant ad updates)
    const val REFRESH_INTERVAL_MILLIS: Long = 2_000L
}
