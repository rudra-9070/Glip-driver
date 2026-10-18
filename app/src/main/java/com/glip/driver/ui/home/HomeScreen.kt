package com.glip.driver.ui.home

import android.Manifest
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.text.style.TextOverflow
import kotlin.math.roundToInt
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
import androidx.compose.runtime.LaunchedEffect
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
import com.glip.driver.domain.model.VehicleCategory
import com.glip.driver.location.DriverLocationManager
import com.glip.driver.map.GlipOsmMapView
import com.glip.driver.ui.theme.GlipAccentTeal
import com.glip.driver.ui.theme.GlipNavyDark
import com.glip.driver.ui.theme.GlipNavyPrimary
import kotlinx.coroutines.launch
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationCompat

fun playRideRequestAlert(context: android.content.Context, passengerName: String, fare: Int, pickupName: String) {
    try {
        val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val ringtone = RingtoneManager.getRingtone(context, notificationUri)
        ringtone?.play()
    } catch (e: Exception) {}

    try {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400), -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 400, 200, 400), -1)
        }
    } catch (e: Exception) {}

    try {
        val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as? NotificationManager
        val channelId = "glip_driver_rides"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Ride Requests",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming passenger ride requests"
                enableVibration(true)
            }
            notificationManager?.createNotificationChannel(channel)
        }
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("New Ride Request: ₹$fare")
            .setContentText("$passengerName wants a seat at $pickupName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
        notificationManager?.notify(101, builder.build())
    } catch (e: Exception) {}
}

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
    var hasOverlayPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else true
        )
    }
    var showOverlayPermissionPrompt by remember { mutableStateOf(false) }

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
                hasOverlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Settings.canDrawOverlays(context)
                } else true
                val hasPerm = DriverLocationManager.hasLocationPermission(context)
                hasPermission = hasPerm
                val gpsOn = locationManager.refreshGpsStatus()
                if (hasPerm) {
                    locationManager.startTracking()
                    if (!gpsOn) {
                        showGpsTurnOnDialog = true
                    }
                } else {
                    val perms = buildList {
                        add(Manifest.permission.ACCESS_FINE_LOCATION)
                        add(Manifest.permission.ACCESS_COARSE_LOCATION)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }.toTypedArray()
                    permissionLauncher.launch(perms)
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
    val trips by repository.tripsFlow.collectAsState(initial = emptyList())
    val activeTrips = trips.filter { it.status == com.glip.driver.domain.model.DemoTripStatus.ACCEPTED }
    val activeTrip = activeTrips.firstOrNull()

    var verifyingTrip by remember { mutableStateOf<com.glip.driver.domain.model.DemoTrip?>(null) }
    var enteredOtpText by remember { mutableStateOf("") }
    var otpErrorMessage by remember { mutableStateOf<String?>(null) }

    val origin = driverRoute?.originName ?: "Current Location"
    val destination = driverRoute?.destinationName ?: ""

    // Keep driver ONLINE in background via Foreground Service (Uber / Rapido / Ola style)
    LaunchedEffect(isAvailable, hasPermission) {
        if (isAvailable && hasPermission) {
            com.glip.driver.service.GlipDriverDispatchService.start(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                showOverlayPermissionPrompt = true
            }
        } else if (!isAvailable) {
            com.glip.driver.service.GlipDriverDispatchService.stop(context)
        }
    }

    // Sync live driver GPS location, bearing, and vehicle type to Supabase in real-time
    LaunchedEffect(liveLocation, isAvailable, vehicle) {
        val loc = liveLocation
        val prof = profile
        val veh = vehicle
        if (loc != null && loc.latitude != 0.0 && prof != null && (prof.mobileNumber.isNotBlank() || prof.id.isNotBlank())) {
            com.glip.driver.data.remote.SupabaseClient.updateLiveLocation(
                mobileNumber = prof.mobileNumber,
                driverId = prof.id,
                lat = loc.latitude,
                lng = loc.longitude,
                bearing = loc.bearing,
                isOnline = isAvailable,
                vehicleCategory = veh?.category
            )
        }
    }

    // Auto-sync real Supabase UUID from remote drivers table so driverId is guaranteed to match
    LaunchedEffect(profile?.mobileNumber) {
        val mob = profile?.mobileNumber
        if (!mob.isNullOrBlank()) {
            try {
                val remoteDriver = com.glip.driver.data.remote.SupabaseClient.getDriverByMobile(mob)
                if (remoteDriver != null && remoteDriver.id != profile?.id) {
                    repository.saveProfile(remoteDriver)
                }
            } catch (e: Exception) {}
        }
    }

    var lastSeenRequestId by remember { mutableStateOf<String?>(null) }
    var isSyncingOnline by remember { mutableStateOf(false) }

    // Real-time polling for incoming passenger ride requests whenever driver is ONLINE
    // Strictly filtered to match this driver's registered vehicle category and nearby waterfall dispatch
    LaunchedEffect(isAvailable, profile, vehicle, liveLocation) {
        if (!isAvailable) {
            repository.setIncomingRequests(emptyList())
            return@LaunchedEffect
        }
        val prof = profile
        val driverUuid = prof?.id
        val driverMobile = prof?.mobileNumber
        val cat = vehicle?.category
        val cap = vehicle?.totalCapacity
        while (true) {
            try {
                val loc = liveLocation
                val realRequests = com.glip.driver.data.remote.SupabaseClient.fetchPendingRideRequests(
                    driverUuid = driverUuid,
                    driverMobile = driverMobile,
                    driverVehicleCategory = cat,
                    driverCapacity = cap,
                    driverLat = loc?.latitude ?: 0.0,
                    driverLng = loc?.longitude ?: 0.0
                )
                repository.setIncomingRequests(realRequests)
                val topReq = realRequests.firstOrNull()
                if (topReq != null && topReq.id != lastSeenRequestId) {
                    lastSeenRequestId = topReq.id
                    playRideRequestAlert(
                        context = context,
                        passengerName = topReq.passengerAlias,
                        fare = topReq.illustrativeFare,
                        pickupName = topReq.pickupName
                    )
                }
            } catch (e: Exception) {}
            kotlinx.coroutines.delay(1800L) // Poll every 1.8 seconds for rapid real-time dispatch
        }
    }

    // Real-time listener: If passenger cancels active trip from Consumer App, auto-cancel on driver phone
    LaunchedEffect(activeTrips) {
        val reqIds = activeTrips.map { it.requestId }.filter { it.isNotBlank() }
        if (reqIds.isEmpty()) return@LaunchedEffect
        while (true) {
            kotlinx.coroutines.delay(2500L)
            try {
                val cancelledIds = com.glip.driver.data.remote.SupabaseClient.checkCancelledRequests(reqIds)
                if (cancelledIds.isNotEmpty()) {
                    cancelledIds.forEach { cId ->
                        val tripToCancel = activeTrips.find { it.requestId == cId }
                        if (tripToCancel != null) {
                            repository.updateTripStatus(tripToCancel.id, com.glip.driver.domain.model.DemoTripStatus.CANCELLED)
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

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

                // 3. Vehicle Setup & Edit Card (with Pencil Icon)
                val currentVeh = vehicle
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Text(
                                text = when (currentVeh?.category) {
                                    VehicleCategory.AUTO_RICKSHAW -> "🛺"
                                    VehicleCategory.E_RICKSHAW -> "🛺"
                                    VehicleCategory.CAR -> "🚗"
                                    VehicleCategory.VAN -> "🚐"
                                    VehicleCategory.MINI_BUS -> "🚌"
                                    else -> "🚗"
                                },
                                fontSize = 22.sp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = currentVeh?.displayName?.ifBlank { currentVeh.category.displayName } ?: "My Vehicle",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = GlipNavyPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${currentVeh?.registrationNumberDisplay ?: "Plate"} • ${currentVeh?.totalCapacity ?: 4} Seats",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                onEditVehicle()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Vehicle",
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Floating Overlay Permission Banner (Uber / Rapido Style Alert)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !hasOverlayPermission) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "🔔", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Floating Ride Alerts",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF92400E)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Background me ride alerts pane ke liye 'Display over other apps' allow karein.",
                                fontSize = 11.sp,
                                color = Color(0xFF78350F),
                                lineHeight = 15.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    try {
                                        val intent = Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        ).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Allow Permission", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

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
            // Top Advertisement Space (Prominent Banner at the top of screen)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(115.dp)
                        .background(Color(0xFFFAFAFA)),
                    contentAlignment = Alignment.Center
                ) {
                    // Subtle Ad tag in top-right
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 8.dp, end = 10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFF1F5F9))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "AD",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Advertisement Space",
                            color = Color(0xFF64748B),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Sponsored Banner Area",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // High-visibility GPS Alert Banner (if GPS is turned off on device)
            if (!isGpsEnabled || !hasPermission) {
                Surface(
                    onClick = {
                        if (!hasPermission) {
                            val perms = buildList {
                                add(Manifest.permission.ACCESS_FINE_LOCATION)
                                add(Manifest.permission.ACCESS_COARSE_LOCATION)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    add(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }.toTypedArray()
                            permissionLauncher.launch(perms)
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
                        route = if (isAvailable && driverRoute?.isActive == true && !driverRoute?.destinationName.isNullOrBlank() && driverRoute?.destinationName != "Independent Ride") driverRoute else null,
                        requests = if (isAvailable && (activeTrips.isEmpty() || (vehicle?.availableSeats ?: 1) > 0)) requests else emptyList(),
                        activeTrip = activeTrip,
                        activeTrips = activeTrips,
                        driverLiveLocation = liveLocation,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Offline state overlay banner
                    if (!isAvailable) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF0F172A).copy(alpha = 0.82f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFEE2E2)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOff,
                                            contentDescription = "Offline",
                                            tint = Color(0xFFDC2626),
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = "YOU ARE OFFLINE",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Please turn online to get orders and receive passenger ride requests along your route.",
                                        fontSize = 13.sp,
                                        color = Color(0xFF64748B),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(18.dp))
                                    Button(
                                        onClick = {
                                            if (!isSyncingOnline) {
                                                isSyncingOnline = true
                                                coroutineScope.launch {
                                                    try {
                                                        repository.setAvailable(true)
                                                        kotlinx.coroutines.delay(400)
                                                    } finally {
                                                        isSyncingOnline = false
                                                    }
                                                }
                                            }
                                        },
                                        enabled = !isSyncingOnline,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(46.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                                    ) {
                                        if (isSyncingOnline) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "CONNECTING...",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        } else {
                                            Text(
                                                text = "GO ONLINE",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 1. ACTIVE TRIP CARDS (Rendered OUTSIDE map, directly BELOW the Map Card)
            if (activeTrips.isNotEmpty()) {
                val sortedTrips = remember(activeTrips, liveLocation) {
                    activeTrips.sortedWith(
                        compareBy<com.glip.driver.domain.model.DemoTrip> { it.isPickedUp }
                            .thenBy { trip ->
                                if (!trip.isPickedUp) {
                                    calculateDistanceKm(
                                        lat1 = liveLocation?.latitude ?: 0.0,
                                        lon1 = liveLocation?.longitude ?: 0.0,
                                        lat2 = trip.pickupLat,
                                        lon2 = trip.pickupLng
                                    )
                                } else {
                                    calculateDistanceKm(
                                        lat1 = liveLocation?.latitude ?: 0.0,
                                        lon1 = liveLocation?.longitude ?: 0.0,
                                        lat2 = trip.dropoffLat,
                                        lon2 = trip.dropoffLng
                                    )
                                }
                            }
                    )
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    border = BorderStroke(1.5.dp, Color(0xFF10B981))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(sortedTrips, key = { _, trip -> trip.id }) { index, trip ->
                                val cleanPassenger = trip.passengerAlias.replace(Regex("\\s*\\([^)]*\\)"), "").trim().ifBlank { "Rider" }
                                val dropTitle = trip.dropoffName.split(",").firstOrNull()?.trim() ?: trip.dropoffName
                                val distToPickup = calculateDistanceKm(
                                    lat1 = liveLocation?.latitude ?: 0.0,
                                    lon1 = liveLocation?.longitude ?: 0.0,
                                    lat2 = trip.pickupLat,
                                    lon2 = trip.pickupLng
                                )
                                val distToDrop = calculateDistanceKm(
                                    lat1 = liveLocation?.latitude ?: 0.0,
                                    lon1 = liveLocation?.longitude ?: 0.0,
                                    lat2 = trip.dropoffLat,
                                    lon2 = trip.dropoffLng
                                )
                                val etaMins = ((distToDrop / 28.0) * 60).roundToInt().coerceAtLeast(1)

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (!trip.isPickedUp) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (!trip.isPickedUp) Color(0xFF86EFAC) else Color(0xFFE2E8F0)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        // Passenger Name & Drop Location
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = cleanPassenger,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF0F172A),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "Drop: $dropTitle",
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF475569),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            // Fare badge
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFDCFCE7),
                                                border = BorderStroke(1.dp, Color(0xFF86EFAC))
                                            ) {
                                                Text(
                                                    text = "₹${trip.illustrativeFare}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF166534),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        if (!trip.isPickedUp) {
                                            // BEFORE OTP ENTERED:
                                            // Passenger Name, Drop Location, Call Icon, Cancel, ENTER OTP / PICK UP
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Quick Call Button
                                                if (trip.passengerMobile.isNotBlank()) {
                                                    Surface(
                                                        onClick = {
                                                            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                                                data = Uri.parse("tel:${trip.passengerMobile}")
                                                            }
                                                            context.startActivity(dialIntent)
                                                        },
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = Color.White,
                                                        border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                                                        modifier = Modifier.size(36.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                imageVector = Icons.Default.Phone,
                                                                contentDescription = "Call Passenger",
                                                                tint = Color(0xFF15803D),
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                    }
                                                }

                                                // Cancel Button
                                                OutlinedButton(
                                                    onClick = {
                                                        coroutineScope.launch {
                                                            repository.updateTripStatus(trip.id, com.glip.driver.domain.model.DemoTripStatus.CANCELLED)
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(36.dp),
                                                    shape = RoundedCornerShape(8.dp),
                                                    border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                                ) {
                                                    Text(
                                                        text = "Cancel",
                                                        color = Color(0xFFDC2626),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }

                                                // ENTER OTP / PICK UP Button
                                                Button(
                                                    onClick = {
                                                        verifyingTrip = trip
                                                        enteredOtpText = ""
                                                        otpErrorMessage = null
                                                    },
                                                    modifier = Modifier
                                                        .weight(2.2f)
                                                        .height(36.dp),
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = "ENTER OTP / PICK UP",
                                                            color = Color.White,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        } else {
                                            // AFTER OTP ENTERED:
                                            // "Reaching drop location in [X mins]" (green)
                                            // "Destination is [X.X km] away" (slate gray)
                                            // Cancel button, DROP PASSENGER button (CALL ICON REMOVED!)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Reaching drop location in ",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF0F172A)
                                                )
                                                Text(
                                                    text = "$etaMins mins",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF15803D)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Destination is $distToDrop km away",
                                                fontSize = 12.sp,
                                                color = Color(0xFF64748B)
                                            )

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Cancel Button
                                                OutlinedButton(
                                                    onClick = {
                                                        coroutineScope.launch {
                                                            repository.updateTripStatus(trip.id, com.glip.driver.domain.model.DemoTripStatus.CANCELLED)
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(36.dp),
                                                    shape = RoundedCornerShape(8.dp),
                                                    border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                                ) {
                                                    Text(
                                                        text = "Cancel",
                                                        color = Color(0xFFDC2626),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }

                                                // DROP PASSENGER Button
                                                Button(
                                                    onClick = {
                                                        coroutineScope.launch {
                                                            repository.updateTripStatus(trip.id, com.glip.driver.domain.model.DemoTripStatus.COMPLETED)
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .weight(2.2f)
                                                        .height(36.dp),
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = "DROP PASSENGER",
                                                            color = Color.White,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. INCOMING RIDE REQUEST CARDS (When idle OR when driver has remaining seats to pool)
            val remainingSeats = (vehicle?.availableSeats ?: ((vehicle?.totalCapacity ?: 1) - activeTrips.size)).coerceAtLeast(0)
            if (isAvailable && requests.isNotEmpty() && (activeTrips.isEmpty() || remainingSeats > 0)) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = if (activeTrips.isNotEmpty()) 220.dp else 360.dp)
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(requests, key = { it.id }) { req ->
                        val isPersonal = req.passengerAlias.contains("Personal", ignoreCase = true) || req.requestedSeats >= 3
                        val distToPickup = calculateDistanceKm(
                            lat1 = liveLocation?.latitude ?: 0.0,
                            lon1 = liveLocation?.longitude ?: 0.0,
                            lat2 = req.pickupLat,
                            lon2 = req.pickupLng
                        )
                        val tripDist = calculateDistanceKm(
                            lat1 = req.pickupLat,
                            lon1 = req.pickupLng,
                            lat2 = req.dropoffLat,
                            lon2 = req.dropoffLng
                        )
                        val pickupTitle = req.pickupName.split(",").firstOrNull()?.trim() ?: req.pickupName
                        val dropTitle = req.dropoffName.split(",").firstOrNull()?.trim() ?: req.dropoffName

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                                // Header: Fare + Pooling Tag + Cash
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "₹${req.illustrativeFare}",
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF0F172A)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (activeTrips.isNotEmpty()) Color(0xFFDCFCE7) else if (isPersonal) Color(0xFFFEF3C7) else Color(0xFFF1F5F9),
                                            border = BorderStroke(1.dp, if (activeTrips.isNotEmpty()) Color(0xFF86EFAC) else if (isPersonal) Color(0xFFFDE68A) else Color(0xFFE2E8F0))
                                        ) {
                                            Text(
                                                text = if (activeTrips.isNotEmpty()) "⭐ POOL REQUEST" else if (isPersonal) "⭐ PERSONAL" else "${req.requestedSeats} SEAT",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (activeTrips.isNotEmpty()) Color(0xFF166534) else if (isPersonal) Color(0xFFB45309) else Color(0xFF475569),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFF8FAFC),
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                    ) {
                                        Text(
                                            text = "Cash",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF64748B),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Pickup Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "• ${if (distToPickup > 0) "${distToPickup} km" else "Near you"}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A),
                                        modifier = Modifier.width(72.dp)
                                    )
                                    Text(
                                        text = pickupTitle,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Dropoff Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "▼ ${if (tripDist > 0) "${tripDist} km" else "Drop"}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A),
                                        modifier = Modifier.width(72.dp)
                                    )
                                    Text(
                                        text = dropTitle,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Decline (X) + Accept Button
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        onClick = {
                                            coroutineScope.launch {
                                                repository.declineRequest(req.id)
                                            }
                                        },
                                        shape = CircleShape,
                                        color = Color.White,
                                        border = BorderStroke(1.5.dp, Color(0xFFCBD5E1)),
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Decline",
                                                tint = Color(0xFF64748B),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                repository.acceptRequest(req.id)
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF5B838))
                                    ) {
                                        Text(
                                            text = if (activeTrips.isNotEmpty()) "Accept (Pool Share)" else "Accept",
                                            color = Color(0xFF0F172A),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Controls Bar Box: Menu, Compact Online/Offline Toggle, Route (Full-Width Docked, No Left/Right Gap)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
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

                    // 2. Compact Online / Offline Toggle (Only Border turns Green, background stays white)
                    val isOnline = isAvailable
                    val toggleBorderColor by animateColorAsState(
                        targetValue = if (isOnline) Color(0xFF16A34A) else Color(0xFFCBD5E1),
                        label = "toggleBorder"
                    )
                    val toggleTextColor by animateColorAsState(
                        targetValue = if (isOnline) Color(0xFF16A34A) else Color(0xFF64748B),
                        label = "toggleText"
                    )

                    Surface(
                        onClick = {
                            if (!isSyncingOnline) {
                                isSyncingOnline = true
                                coroutineScope.launch {
                                    try {
                                        repository.setAvailable(!isOnline)
                                        kotlinx.coroutines.delay(400)
                                    } finally {
                                        isSyncingOnline = false
                                    }
                                }
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        border = BorderStroke(if (isOnline) 1.5.dp else 1.dp, toggleBorderColor),
                        modifier = Modifier
                            .width(108.dp)
                            .height(38.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Text Label on appropriate side
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp),
                                horizontalArrangement = if (isOnline) Arrangement.Start else Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isSyncingOnline) "SYNC..." else if (isOnline) "ONLINE" else "OFFLINE",
                                    color = toggleTextColor,
                                    fontSize = if (isSyncingOnline) 9.sp else if (isOnline) 11.sp else 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            // Sliding Circular Thumb with Shadow
                            Box(
                                modifier = Modifier
                                    .align(if (isOnline) Alignment.CenterEnd else Alignment.CenterStart)
                                    .size(28.dp)
                                    .shadow(elevation = 2.dp, shape = CircleShape)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(
                                        width = 1.dp,
                                        color = if (isOnline) Color(0xFF16A34A).copy(alpha = 0.3f) else Color(0xFFE2E8F0),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSyncingOnline) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = if (isOnline) Color(0xFF16A34A) else Color(0xFF0284C7)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(if (isOnline) Color(0xFF16A34A) else Color(0xFF94A3B8))
                                    )
                                }
                            }
                        }
                    }

                    // 3. Planned Route Setup Button
                    Surface(
                        onClick = onEditRoute,
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF0F9FF),
                        border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
                        modifier = Modifier.height(38.dp)
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
        }

        // 4-Digit Passenger Ride OTP Verification Dialog
        if (verifyingTrip != null) {
            val trip = verifyingTrip!!
            AlertDialog(
                onDismissRequest = { verifyingTrip = null },
                shape = RoundedCornerShape(20.dp),
                containerColor = Color.White,
                icon = {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDCFCE7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = "Verify Ride OTP",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column {
                        val cleanPassenger = trip.passengerAlias.replace(Regex("\\s*\\([^)]*\\)"), "").trim().ifBlank { "Passenger" }
                        Text(
                            text = "$cleanPassenger se 4-digit ride OTP lein. OTP verify hone par map me passenger pickup marker ki blinking band ho jayegi.",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = enteredOtpText,
                            onValueChange = { if (it.length <= 4) enteredOtpText = it },
                            label = { Text("4-Digit OTP") },
                            placeholder = { Text(if (trip.otp.isNotBlank()) "e.g. ${trip.otp}" else "e.g. 1234") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (otpErrorMessage != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = otpErrorMessage!!,
                                color = Color(0xFFDC2626),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val isMatch = repository.verifyOtpAndPickUp(trip.id, enteredOtpText)
                                if (isMatch) {
                                    verifyingTrip = null
                                } else {
                                    otpErrorMessage = "Galat OTP! Kripya passenger ka 4-digit OTP enter karein."
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                    ) {
                        Text("VERIFY & ONBOARD", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            // Direct Boarding fallback so driver is never blocked
                            coroutineScope.launch {
                                repository.markPassengerPickedUp(trip.id)
                                verifyingTrip = null
                            }
                        }
                    ) {
                        Text("DIRECT BOARDING", color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                    }
                }
            )
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

        // Floating Overlay Permission Dialog - Prompts driver to allow "Display over other apps"
        if (showOverlayPermissionPrompt && !hasOverlayPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AlertDialog(
                onDismissRequest = { showOverlayPermissionPrompt = false },
                icon = {
                    Text(text = "🔔", fontSize = 32.sp)
                },
                title = {
                    Text(
                        text = "Enable Floating Ride Alerts",
                        fontWeight = FontWeight.Bold,
                        color = GlipNavyPrimary
                    )
                },
                text = {
                    Text(
                        text = "Rapido / Uber / Ola ki tarah phone me bahar hote huye bhi ride alerts pane ke liye 'Display over other apps' allow karein.",
                        color = Color(0xFF475569),
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showOverlayPermissionPrompt = false
                            try {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                ).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }
                    ) {
                        Text(
                            text = "ALLOW PERMISSION",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0284C7)
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showOverlayPermissionPrompt = false }) {
                        Text("Later", color = Color(0xFF94A3B8))
                    }
                }
            )
        }
    }
}

private fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    if (lat1 == 0.0 || lon1 == 0.0 || lat2 == 0.0 || lon2 == 0.0) return 0.0
    val results = FloatArray(1)
    Location.distanceBetween(lat1, lon1, lat2, lon2, results)
    return Math.round((results[0] / 1000.0) * 10.0) / 10.0
}
