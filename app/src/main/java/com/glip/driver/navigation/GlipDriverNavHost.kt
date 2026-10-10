package com.glip.driver.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.glip.driver.data.repository.DemoDriverRepositoryImpl
import com.glip.driver.ui.activity.ActivityScreen
import com.glip.driver.ui.auth.LoginScreen
import com.glip.driver.ui.auth.RegisterScreen
import com.glip.driver.ui.auth.WelcomeScreen
import com.glip.driver.ui.home.HomeScreen
import com.glip.driver.ui.onboarding.RouteSetupScreen
import com.glip.driver.ui.onboarding.VehicleSetupScreen
import com.glip.driver.ui.profile.ProfileScreen
import com.glip.driver.ui.requests.RequestsScreen
import com.glip.driver.ui.services.ComingSoonScreen
import com.glip.driver.ui.theme.GlipAccentTeal
import com.glip.driver.ui.theme.GlipNavyDark
import com.glip.driver.ui.theme.GlipNavyPrimary

data class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home.route, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Screen.Requests.route, "Requests", Icons.Filled.DirectionsCar, Icons.Outlined.DirectionsCar),
    BottomNavItem(Screen.Activity.route, "Activity", Icons.Filled.FormatListBulleted, Icons.Outlined.FormatListBulleted),
    BottomNavItem(Screen.Profile.route, "Profile", Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle)
)

@Composable
fun GlipDriverNavHost(
    navController: NavHostController,
    repository: DemoDriverRepositoryImpl,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val profile by repository.profileFlow.collectAsState(initial = null)
    val startDestination = if (profile != null) Screen.Home.route else Screen.Welcome.route

    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Requests.route,
        Screen.Activity.route,
        Screen.Profile.route
    )

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = GlipNavyPrimary,
                    tonalElevation = 8.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title,
                                    tint = if (selected) GlipAccentTeal else Color(0xFF94A3B8)
                                )
                            },
                            label = {
                                Text(
                                    text = item.title,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) GlipAccentTeal else Color(0xFF94A3B8)
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = GlipNavyDark
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Welcome & Auth
            composable(Screen.Welcome.route) {
                WelcomeScreen(
                    onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                    onNavigateToLogin = { navController.navigate(Screen.Login.route) }
                )
            }
            composable(Screen.Register.route) {
                RegisterScreen(
                    repository = repository,
                    onRegistrationSuccess = {
                        navController.navigate(Screen.VehicleSetup.route) {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                        }
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
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate(Screen.Register.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            // Onboarding Setups
            composable(Screen.VehicleSetup.route) {
                VehicleSetupScreen(
                    repository = repository,
                    onSavedOrSkipped = {
                        navController.navigate(Screen.RouteSetup.route)
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

            // Main App Tabs
            composable(Screen.Home.route) {
                HomeScreen(
                    repository = repository,
                    onEditVehicle = { navController.navigate(Screen.VehicleSetup.route) },
                    onEditRoute = { navController.navigate(Screen.RouteSetup.route) },
                    onViewAllRequests = { navController.navigate(Screen.Requests.route) }
                )
            }
            composable(Screen.Requests.route) {
                RequestsScreen(repository = repository)
            }
            composable(Screen.Activity.route) {
                ActivityScreen(repository = repository)
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    repository = repository,
                    onEditVehicle = { navController.navigate(Screen.VehicleSetup.route) },
                    onEditRoute = { navController.navigate(Screen.RouteSetup.route) },
                    onNavigateToParcel = { navController.navigate(Screen.ParcelService.route) },
                    onNavigateToSchool = { navController.navigate(Screen.SchoolService.route) },
                    onLoggedOut = {
                        navController.navigate(Screen.Welcome.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
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
                ComingSoonScreen(
                    title = "School Transportation (Coming Soon)",
                    description = "Safe scheduled pooling for neighborhood students. Background background check badges and dedicated safety cameras will be enabled soon.",
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
