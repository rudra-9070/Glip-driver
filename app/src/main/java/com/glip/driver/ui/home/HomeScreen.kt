package com.glip.driver.ui.home

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.draw.shadow
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.glip.driver.data.repository.DemoDriverRepositoryImpl
import com.glip.driver.location.DriverLocationManager
import com.glip.driver.map.GlipOsmMapView
import com.glip.driver.ui.theme.GlipAccentTeal
import com.glip.driver.ui.theme.GlipNavyDark
import com.glip.driver.ui.theme.GlipNavyPrimary
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    repository: DemoDriverRepositoryImpl,
    onNavigateToProfile: () -> Unit = {},
    onNavigateToActivity: () -> Unit = {},
    onNavigateToRequests: () -> Unit = {},
    onEditVehicle: () -> Unit = {},
    onEditRoute: () -> Unit = {},
    onViewAllRequests: () -> Unit = {},
    onNavigateToDiagnostics: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    // Handle back press to close drawer if open
    BackHandler(enabled = drawerState.isOpen) {
        coroutineScope.launch { drawerState.close() }
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val locationManager = remember { DriverLocationManager(context.applicationContext) }
    val liveLocation by locationManager.currentLocation.collectAsState()
    val isGpsEnabled by locationManager.isGpsEnabled.collectAsState()

    var hasPermission by remember {
        mutableStateOf(DriverLocationManager.hasLocationPermission(context))
    }
    var showGpsTurnOnDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        hasPermission = granted
        if (granted) {
            locationManager.startTracking()
            if (!locationManager.refreshGpsStatus()) {
                showGpsTurnOnDialog = true
            }
        }
    }

    // App me aate hi (or on resume), check location permission & GPS status every time
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val hasPerm = DriverLocationManager.hasLocationPermission(context)
                hasPermission = hasPerm
                val gpsOn = locationManager.refreshGpsStatus()
                if (hasPerm) {
                    locationManager.startTracking()
                    if (!gpsOn) {
                        showGpsTurnOnDialog = true
                    }
                } else {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                locationManager.stopTracking()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            locationManager.stopTracking()
        }
    }

    val driverRoute by repository.routeFlow.collectAsState(initial = null)
    val vehicle by repository.vehicleFlow.collectAsState(initial = null)
    val isAvailable by repository.isAvailableFlow.collectAsState(initial = true)
    val requests by repository.requestsFlow.collectAsState(initial = emptyList())
    val profile by repository.profileFlow.collectAsState(initial = null)

    val origin = driverRoute?.originName ?: "Motipur"
    val destination = driverRoute?.destinationName ?: "Baruraj"

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.White,
                drawerContentColor = Color(0xFF0F172A),
                modifier = Modifier.width(300.dp)
            ) {
                // Sleek Clean White Drawer Header with Close Button
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC))
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(GlipNavyPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = "Glip Logo",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Glip Driver",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GlipNavyPrimary
                                )
                                Text(
                                    text = "Driver Partner App",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        // Close / Back Button inside Drawer
                        IconButton(
                            onClick = { coroutineScope.launch { drawerState.close() } },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Menu",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Divider(color = Color(0xFFE2E8F0), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // 1. Profile Tab (Top Tab in Drawer)
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Profile",
                            tint = GlipNavyPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Profile",
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A),
                            fontSize = 15.sp
                        )
                    },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        onNavigateToProfile()
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )

                // 2. Activity Tab
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.FormatListBulleted,
                            contentDescription = "Activity",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Activity",
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A),
                            fontSize = 15.sp
                        )
                    },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        onNavigateToActivity()
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                // Diagnostics item at bottom of drawer
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = "Diagnostics",
                            tint = Color(0xFF94A3B8)
                        )
                    },
                    label = {
                        Text(
                            text = "Diagnostics & Logs",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        onNavigateToDiagnostics()
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
        ) {
            // Top Hero Header Bar Box: 60dp Height, Premium Sleek Layout
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Hamburger Menu Button (Toggles drawer open/close)
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                if (drawerState.isOpen) drawerState.close() else drawerState.open()
                            }
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu Bar",
                            tint = GlipNavyPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // 2. Premium Custom Online / Offline Toggle Button
                    val isOnline = isAvailable
                    val toggleBgColor by animateColorAsState(
                        targetValue = if (isOnline) Color(0xFF16A34A) else Color(0xFFF1F5F9),
                        label = "toggleBg"
                    )
                    val toggleBorderColor by animateColorAsState(
                        targetValue = if (isOnline) Color(0xFF15803D) else Color(0xFFCBD5E1),
                        label = "toggleBorder"
                    )
                    val toggleTextColor by animateColorAsState(
                        targetValue = if (isOnline) Color.White else Color(0xFF64748B),
                        label = "toggleText"
                    )

                    Surface(
                        onClick = {
                            coroutineScope.launch {
                                repository.setAvailable(!isOnline)
                            }
                        },
                        shape = RoundedCornerShape(22.dp),
                        color = toggleBgColor,
                        border = BorderStroke(1.dp, toggleBorderColor),
                        modifier = Modifier
                            .width(132.dp)
                            .height(40.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Text Label on appropriate side
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = if (isOnline) Arrangement.Start else Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isOnline) "ONLINE" else "OFFLINE",
                                    color = toggleTextColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            // Sliding Circular Thumb with Shadow
                            Box(
                                modifier = Modifier
                                    .align(if (isOnline) Alignment.CenterEnd else Alignment.CenterStart)
                                    .size(30.dp)
                                    .shadow(elevation = 3.dp, shape = CircleShape)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (isOnline) Color(0xFF16A34A) else Color(0xFF94A3B8))
                                )
                            }
                        }
                    }

                    // 3. Planned Route Setup Button
                    Surface(
                        onClick = onEditRoute,
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF0F9FF),
                        border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AltRoute,
                                contentDescription = "Planned Route Setup",
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Route",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0284C7)
                            )
                        }
                    }
                }
            }

            // Advertisement Space (Directly below Top Bar Box)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Advertisement Space",
                        color = Color(0xFF94A3B8),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // High-visibility GPS Alert Banner (if GPS is turned off on device)
            if (!isGpsEnabled || !hasPermission) {
                Surface(
                    onClick = {
                        if (!hasPermission) {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        } else {
                            DriverLocationManager.openLocationSettings(context)
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, Color(0xFFFECACA)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOff,
                            contentDescription = "GPS Alert",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (!hasPermission) {
                                "Location permission chahiye live map ke liye. Tap to Allow."
                            } else {
                                "⚠️ Device GPS OFF hai. Live location ke liye yahan tap karke ON karein."
                            },
                            color = Color(0xFFDC2626),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Live Native OpenStreetMap View Container (Ola / Uber / Rapido Style with Live Driver GPS)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    GlipOsmMapView(
                        route = driverRoute,
                        requests = requests,
                        driverLiveLocation = liveLocation,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // 2 Seat Available & Passenger Requests Box (At bottom of screen)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Seat capacity status
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE0F2FE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = "Vehicle",
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            val availSeats = vehicle?.availableSeats ?: 3
                            Text(
                                text = "$availSeats Seat(s) Available",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = vehicle?.displayName ?: "Vehicle Active",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    // Right: Requests Action Button
                    Surface(
                        onClick = onViewAllRequests,
                        shape = RoundedCornerShape(16.dp),
                        color = if (requests.isNotEmpty()) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (requests.isNotEmpty()) Color(0xFFBBF7D0) else Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = "Requests",
                                tint = if (requests.isNotEmpty()) Color(0xFF16A34A) else Color(0xFF64748B),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Requests (${requests.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (requests.isNotEmpty()) Color(0xFF16A34A) else Color(0xFF64748B)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }

        // GPS ON Prompt Dialog - Prompt driver to turn ON GPS whenever it is off
        if (showGpsTurnOnDialog && !isGpsEnabled) {
            AlertDialog(
                onDismissRequest = { showGpsTurnOnDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.LocationOff,
                        contentDescription = "Turn On GPS",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        text = "Turn On Device GPS",
                        fontWeight = FontWeight.Bold,
                        color = GlipNavyPrimary
                    )
                },
                text = {
                    Text(
                        text = "Aapka device GPS location OFF hai. Glip Driver app me live location dekhne aur raste ke passengers match karne ke liye device GPS ON karein.",
                        color = Color(0xFF475569),
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showGpsTurnOnDialog = false
                            DriverLocationManager.openLocationSettings(context)
                        }
                    ) {
                        Text(
                            text = "TURN ON GPS",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0284C7)
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showGpsTurnOnDialog = false }) {
                        Text("Later", color = Color(0xFF94A3B8))
                    }
                }
            )
        }
    }
}
