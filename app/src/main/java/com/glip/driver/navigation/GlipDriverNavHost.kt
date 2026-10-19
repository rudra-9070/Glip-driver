package com.glip.driver.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.glip.driver.data.repository.DemoDriverRepositoryImpl
import com.glip.driver.ui.activity.ActivityScreen
import com.glip.driver.ui.auth.LoginScreen
import com.glip.driver.ui.auth.RegisterScreen
import com.glip.driver.ui.auth.WelcomeScreen
import com.glip.driver.ui.diagnostics.DiagnosticsScreen
import com.glip.driver.ui.home.HomeScreen
import com.glip.driver.ui.onboarding.RouteSetupScreen
import com.glip.driver.ui.onboarding.VehicleSetupScreen
import com.glip.driver.ui.profile.ProfileScreen
import com.glip.driver.ui.requests.RequestsScreen
import com.glip.driver.ui.services.ComingSoonScreen
import com.glip.driver.ui.services.SchoolTransportationScreen
import com.glip.driver.ui.wallet.WalletScreen

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.glip.driver.ui.auth.OtpVerificationScreen
import com.glip.driver.ui.auth.DriverDetailsScreen
import com.glip.driver.ui.auth.VehicleCategorySelectScreen

@Composable
fun GlipDriverNavHost(
    navController: NavHostController,
    repository: DemoDriverRepositoryImpl,
    modifier: Modifier = Modifier
) {
    val profile by repository.profileFlow.collectAsState(initial = null)
    val startDestination = if (profile != null) Screen.Home.route else Screen.Register.route

    var otpIsLogin by remember { mutableStateOf(false) }
    var otpMobileNumber by remember { mutableStateOf("") }
    var otpFullName by remember { mutableStateOf("") }
    var otpGeneratedCode by remember { mutableStateOf("") }

    var regPersonalName by remember { mutableStateOf("") }
    var regVehiclePlate by remember { mutableStateOf("") }
    var regLicenseNumber by remember { mutableStateOf("") }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier.fillMaxSize()
    ) {
        // Welcome & Auth
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onNavigateToLogin = { navController.navigate(Screen.Login.route) },
                onNavigateToDiagnostics = { navController.navigate(Screen.Diagnostics.route) }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                repository = repository,
                onNavigateToOtp = { isLogin, mobile, name, otp ->
                    otpIsLogin = isLogin
                    otpMobileNumber = mobile
                    otpFullName = name
                    otpGeneratedCode = otp
                    navController.navigate(Screen.OtpVerification.route)
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Login.route) {
            LoginScreen(
                repository = repository,
                onNavigateToOtp = { isLogin, mobile, name, otp ->
                    otpIsLogin = isLogin
                    otpMobileNumber = mobile
                    otpFullName = name
                    otpGeneratedCode = otp
                    navController.navigate(Screen.OtpVerification.route)
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.OtpVerification.route) {
            OtpVerificationScreen(
                repository = repository,
                isLogin = otpIsLogin,
                mobileNumber = otpMobileNumber,
                fullName = otpFullName,
                expectedOtp = otpGeneratedCode,
                onVerificationSuccess = { isNewUser ->
                    if (isNewUser) {
                        navController.navigate(Screen.DriverDetails.route)
                    } else {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                        }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // Step 2: Driver & Vehicle Details (Screenshot 1)
        composable(Screen.DriverDetails.route) {
            DriverDetailsScreen(
                initialName = regPersonalName.ifBlank { otpFullName },
                onBack = { navController.popBackStack() },
                onNext = { name, plate, license ->
                    regPersonalName = name
                    regVehiclePlate = plate
                    regLicenseNumber = license
                    navController.navigate(Screen.VehicleCategorySelect.route)
                }
            )
        }

        // Step 3: Vehicle Category (Auto, E-Rickshaw, Car, Van, Mini bus) -> Auto creates vehicle and goes to Home
        composable(Screen.VehicleCategorySelect.route) {
            VehicleCategorySelectScreen(
                repository = repository,
                mobileNumber = otpMobileNumber,
                fullName = regPersonalName,
                vehiclePlate = regVehiclePlate,
                licenseNumber = regLicenseNumber,
                onRegistrationComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        // Standalone Vehicle Edit (Accessible via Drawer menu pencil icon)
        composable(Screen.VehicleSetup.route) {
            VehicleSetupScreen(
                repository = repository,
                onSavedOrSkipped = {
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.RouteSetup.route) {
            RouteSetupScreen(
                repository = repository,
                onSavedOrSkipped = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        // Main App Screens (Accessible via Top Hamburger Menu)
        composable(Screen.Home.route) {
            HomeScreen(
                repository = repository,
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToWallet = { navController.navigate(Screen.Wallet.route) },
                onNavigateToActivity = { navController.navigate(Screen.Activity.route) },
                onNavigateToRequests = { navController.navigate(Screen.Requests.route) },
                onEditVehicle = { navController.navigate(Screen.VehicleSetup.route) },
                onEditRoute = { navController.navigate(Screen.RouteSetup.route) },
                onViewAllRequests = { navController.navigate(Screen.Requests.route) },
                onNavigateToDiagnostics = { navController.navigate(Screen.Diagnostics.route) }
            )
        }
        composable(Screen.Wallet.route) {
            WalletScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Requests.route) {
            RequestsScreen(
                repository = repository,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Activity.route) {
            ActivityScreen(
                repository = repository,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Profile.route) {
            ProfileScreen(
                repository = repository,
                onBack = { navController.popBackStack() },
                onEditVehicle = { navController.navigate(Screen.VehicleSetup.route) },
                onEditRoute = { navController.navigate(Screen.RouteSetup.route) },
                onNavigateToParcel = { navController.navigate(Screen.ParcelService.route) },
                onNavigateToSchool = { navController.navigate(Screen.SchoolService.route) },
                onNavigateToDiagnostics = { navController.navigate(Screen.Diagnostics.route) },
                onLoggedOut = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Diagnostics & Error Logs Screen
        composable(Screen.Diagnostics.route) {
            DiagnosticsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // Future Services
        composable(Screen.ParcelService.route) {
            ComingSoonScreen(
                title = "Parcel Delivery (Coming Soon)",
                description = "Earn extra along your regular route by delivering light local parcels. Fleet verification and parcel scanning will be activated in upcoming phases.",
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.SchoolService.route) {
            SchoolTransportationScreen(
                repository = repository,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
