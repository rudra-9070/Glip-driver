package com.glip.driver.navigation

sealed class Screen(val route: String) {
    // Auth & Onboarding Flow
    data object Welcome : Screen("welcome")
    data object Register : Screen("register")
    data object Login : Screen("login")
    data object OtpVerification : Screen("otp_verification")
    data object VehicleSetup : Screen("vehicle_setup")
    data object RouteSetup : Screen("route_setup")

    // Main Bottom Navigation Tabs
    data object Home : Screen("home")
    data object Requests : Screen("requests")
    data object Activity : Screen("activity")
    data object Profile : Screen("profile")

    // Coming Soon Services
    data object ParcelService : Screen("service_parcel")
    data object SchoolService : Screen("service_school")

    // Diagnostics & Error Logs
    data object Diagnostics : Screen("diagnostics")
}
