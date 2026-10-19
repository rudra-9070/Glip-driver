package com.glip.driver.ui.services

import android.content.Context
import android.location.Location
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.glip.driver.data.remote.SchoolApplicationData
import com.glip.driver.data.remote.SchoolSupabaseClient
import com.glip.driver.domain.repository.DriverRepository
import com.glip.driver.location.DriverLocationManager
import com.glip.driver.map.OsmGeocoderHelper
import com.glip.driver.ui.components.GlipPrimaryButton
import com.glip.driver.ui.theme.GlipAccentTeal
import com.glip.driver.ui.theme.GlipErrorRed
import com.glip.driver.ui.theme.GlipNavyPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView

@Composable
fun SchoolTransportationScreen(
    repository: DriverRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val profile by repository.profileFlow.collectAsState(initial = null)
    val vehicle by repository.vehicleFlow.collectAsState(initial = null)

    val locationManager = remember { DriverLocationManager(context) }
    val driverLiveLocation by locationManager.currentLocation.collectAsState()

    var driverName by remember { mutableStateOf("") }
    var driverMobile by remember { mutableStateOf("") }
    var vehicleNumber by remember { mutableStateOf("") }
    var homeAddress by remember { mutableStateOf("") }
    var homeLat by remember { mutableDoubleStateOf(0.0) }
    var homeLng by remember { mutableDoubleStateOf(0.0) }

    val prefs = remember {
        context.getSharedPreferences("glip_driver_demo_prefs", Context.MODE_PRIVATE)
    }

    val cleanMobile = remember(profile?.mobileNumber, driverMobile) {
        val raw = if (driverMobile.isNotBlank()) driverMobile else (profile?.mobileNumber ?: "")
        raw.replace(Regex("[^0-9]"), "").takeLast(10)
    }

    var applicationData by remember { mutableStateOf<SchoolApplicationData?>(null) }
    var isLocatingGps by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var showMapPicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Start location tracking for quick GPS detection
    LaunchedEffect(Unit) {
        locationManager.startTracking()
    }

    // Check if driver has already submitted an application (local + Supabase)
    LaunchedEffect(cleanMobile) {
        if (cleanMobile.isNotBlank()) {
            val isSubmittedLocally = prefs.getBoolean("school_app_submitted_$cleanMobile", false)
            if (isSubmittedLocally) {
                applicationData = SchoolApplicationData(
                    driverName = prefs.getString("school_app_name_$cleanMobile", "") ?: "",
                    driverMobile = cleanMobile,
                    vehicleNumber = prefs.getString("school_app_vehicle_$cleanMobile", "") ?: "",
                    homeAddress = prefs.getString("school_app_address_$cleanMobile", "") ?: "",
                    status = prefs.getString("school_app_status_$cleanMobile", "PENDING") ?: "PENDING",
                    driverSchoolId = prefs.getString("school_app_id_$cleanMobile", null)
                )
            }
            try {
                val remoteApp = SchoolSupabaseClient.getSchoolTransportationApplication(cleanMobile)
                if (remoteApp != null) {
                    applicationData = remoteApp
                    prefs.edit()
                        .putBoolean("school_app_submitted_$cleanMobile", true)
                        .putString("school_app_name_$cleanMobile", remoteApp.driverName)
                        .putString("school_app_vehicle_$cleanMobile", remoteApp.vehicleNumber)
                        .putString("school_app_address_$cleanMobile", remoteApp.homeAddress)
                        .putString("school_app_status_$cleanMobile", remoteApp.status)
                        .putString("school_app_id_$cleanMobile", remoteApp.driverSchoolId)
                        .apply()
                }
            } catch (e: Exception) {}
        }
    }

    // Pre-populate fields from profile and vehicle registration
    LaunchedEffect(profile, vehicle) {
        if (driverName.isBlank() && profile?.fullName != null) {
            driverName = profile?.fullName ?: ""
        }
        if (driverMobile.isBlank() && profile?.mobileNumber != null) {
            driverMobile = profile?.mobileNumber ?: ""
        }
        if (vehicleNumber.isBlank()) {
            val plate = profile?.vehiclePlate?.takeIf { it.isNotBlank() }
                ?: vehicle?.registrationNumberDisplay?.takeIf { it.isNotBlank() }
                ?: ""
            vehicleNumber = plate
        }
    }

    if (showMapPicker) {
        HomeAddressMapPickerView(
            driverLiveLocation = driverLiveLocation,
            locationManager = locationManager,
            initialLat = if (homeLat != 0.0) homeLat else (driverLiveLocation?.latitude ?: 21.0974),
            initialLng = if (homeLng != 0.0) homeLng else (driverLiveLocation?.longitude ?: 81.0336),
            onLocationSelected = { address, lat, lng ->
                homeAddress = address
                homeLat = lat
                homeLng = lng
                showMapPicker = false
            },
            onDismiss = { showMapPicker = false }
        )
        return
    }

    // PENDING STATUS SCREEN (Shown when driver has already submitted the form)
    if (applicationData != null) {
        val app = applicationData!!
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(scrollState)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = GlipNavyPrimary
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "School Transportation",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = GlipNavyPrimary
                        )
                    )
                    Text(
                        text = "Student route pooling service",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B))
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 1. Pending Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Surface(
                        color = Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0xFFFCD34D))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(text = "⏳", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "UNDER REVIEW",
                                color = Color(0xFFB45309),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Your Request is pending",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = GlipNavyPrimary,
                            fontSize = 22.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Our Team is checking Your Request",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0284C7),
                            fontSize = 14.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Aapka registration form team ke paas pahunch gaya hai. Verification complete hone par school routes aapko assign kiye jayenge aur notifications ke through inform kiya jayega.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF64748B),
                            lineHeight = 18.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Submitted Application Details Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Your Submitted Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = GlipNavyPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    DetailItemRow(label = "Driver Name", value = app.driverName.ifBlank { profile?.fullName ?: "Driver" })
                    DetailItemRow(label = "Mobile Number", value = if (app.driverMobile.isNotBlank()) "+91 ${app.driverMobile}" else (profile?.mobileNumber ?: ""))
                    DetailItemRow(label = "Vehicle Number", value = app.vehicleNumber.ifBlank { vehicle?.registrationNumberDisplay ?: "Not set" })
                    DetailItemRow(label = "Home Address", value = app.homeAddress.ifBlank { "Location saved" })
                    DetailItemRow(
                        label = "School Driver ID",
                        value = app.driverSchoolId ?: "Pending (Verification ke baad assign hoga)",
                        isHighlight = app.driverSchoolId != null
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Assigned School Rides / Requests Section
            Text(
                text = "Assigned School Rides",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = GlipNavyPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "🎒", fontSize = 36.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No Student Rides Assigned Yet",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = GlipNavyPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Jaise hi aapke area me school students ki rides hamari team dwara aapko offer/assign hongi, vo tasks yahan show honge.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Refresh Status Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            val remoteApp = SchoolSupabaseClient.getSchoolTransportationApplication(cleanMobile)
                            if (remoteApp != null) {
                                applicationData = remoteApp
                                Toast.makeText(context, "Status refreshed!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Your request is still in review.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text(
                        text = "🔄 Refresh Status",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0284C7),
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(scrollState)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = GlipNavyPrimary
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = "School Transportation",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = GlipNavyPrimary
                    )
                )
                Text(
                    text = "Register for student route pooling",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B))
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {

                // Driver Name Field
                Text(
                    text = "Driver Name",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF475569)
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = driverName,
                    onValueChange = { driverName = it },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF64748B))
                    },
                    placeholder = { Text("Enter your full name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Mobile Number Field
                Text(
                    text = "Mobile Number",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF475569)
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = driverMobile,
                    onValueChange = { driverMobile = it },
                    leadingIcon = {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF64748B))
                    },
                    placeholder = { Text("Enter your 10-digit mobile number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Vehicle Number Field
                Text(
                    text = "Vehicle Number",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF475569)
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = vehicleNumber,
                    onValueChange = { vehicleNumber = it.uppercase() },
                    leadingIcon = {
                        Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = Color(0xFF64748B))
                    },
                    placeholder = { Text("e.g. CG 04 AB 1234") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Note above Home Address
                Surface(
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Add your home location to receive ride requests from nearby school students.",
                            color = Color(0xFF1E40AF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Home Address Field
                Text(
                    text = "Home Address",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF475569)
                )
                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = homeAddress,
                    onValueChange = { homeAddress = it },
                    leadingIcon = {
                        Icon(Icons.Default.Home, contentDescription = null, tint = Color(0xFF64748B))
                    },
                    trailingIcon = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            // GPS Button
                            IconButton(
                                onClick = {
                                    isLocatingGps = true
                                    coroutineScope.launch {
                                        val loc = locationManager.getBestLastKnownLocation()
                                            ?: driverLiveLocation
                                        if (loc != null && loc.latitude != 0.0) {
                                            val addr = OsmGeocoderHelper.reverseGeocode(
                                                context,
                                                loc.latitude,
                                                loc.longitude
                                            )
                                            if (addr.isNotBlank()) {
                                                homeAddress = addr
                                                homeLat = loc.latitude
                                                homeLng = loc.longitude
                                                Toast.makeText(context, "Current location set as Home", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            Toast.makeText(context, "Acquiring GPS location, please wait...", Toast.LENGTH_SHORT).show()
                                        }
                                        isLocatingGps = false
                                    }
                                }
                            ) {
                                if (isLocatingGps) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.MyLocation,
                                        contentDescription = "My Location",
                                        tint = Color(0xFF0284C7),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            // Choose on Map Button
                            IconButton(
                                onClick = { showMapPicker = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = "Choose on Map",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    },
                    placeholder = { Text("Enter home address or pick on map") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                color = Color(0xFFFEE2E2),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = errorMessage!!,
                    color = GlipErrorRed,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Save & Continue Button
        GlipPrimaryButton(
            text = if (isSubmitting) "Saving..." else "Save & Continue",
            enabled = !isSubmitting,
            onClick = {
                if (driverName.isBlank()) {
                    errorMessage = "Please enter your name"
                    return@GlipPrimaryButton
                }
                if (driverMobile.isBlank()) {
                    errorMessage = "Please enter your mobile number"
                    return@GlipPrimaryButton
                }
                if (vehicleNumber.isBlank()) {
                    errorMessage = "Please enter your vehicle number"
                    return@GlipPrimaryButton
                }
                if (homeAddress.isBlank()) {
                    errorMessage = "Please enter or pick your home address"
                    return@GlipPrimaryButton
                }

                errorMessage = null
                isSubmitting = true

                coroutineScope.launch {
                    val (success, err) = SchoolSupabaseClient.registerSchoolTransportation(
                        driverName = driverName,
                        driverMobile = driverMobile,
                        vehicleNumber = vehicleNumber,
                        homeAddress = homeAddress,
                        homeLat = homeLat,
                        homeLng = homeLng
                    )
                    isSubmitting = false
                    if (success) {
                        val clean = driverMobile.replace(Regex("[^0-9]"), "").takeLast(10)
                        val submittedData = SchoolApplicationData(
                            driverName = driverName,
                            driverMobile = clean,
                            vehicleNumber = vehicleNumber,
                            homeAddress = homeAddress,
                            homeLat = homeLat,
                            homeLng = homeLng,
                            status = "PENDING"
                        )
                        prefs.edit()
                            .putBoolean("school_app_submitted_$clean", true)
                            .putString("school_app_name_$clean", driverName)
                            .putString("school_app_vehicle_$clean", vehicleNumber)
                            .putString("school_app_address_$clean", homeAddress)
                            .putString("school_app_status_$clean", "PENDING")
                            .apply()
                        applicationData = submittedData
                        Toast.makeText(context, "Registration submitted successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        errorMessage = if (err?.contains("relation", ignoreCase = true) == true || err?.contains("PGRST205", ignoreCase = true) == true) {
                            "Table 'school_transportation' not found in database. Please run the SQL script in your Supabase SQL editor."
                        } else {
                            "Failed to save details: $err"
                        }
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
private fun DetailItemRow(label: String, value: String, isHighlight: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF64748B)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.SemiBold,
            color = if (isHighlight) Color(0xFF16A34A) else GlipNavyPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

/**
 * Fullscreen Interactive OpenStreetMap picker for Home Address.
 * Features top search box, GPS button, center pin, and real-time reverse geocoding.
 */
@Composable
private fun HomeAddressMapPickerView(
    driverLiveLocation: Location?,
    locationManager: DriverLocationManager,
    initialLat: Double,
    initialLng: Double,
    onLocationSelected: (address: String, lat: Double, lng: Double) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var mapViewInstance by remember { mutableStateOf<MapView?>(null) }
    var currentPickedAddress by remember { mutableStateOf("Locating address...") }
    var reverseGeocodeJob by remember { mutableStateOf<Job?>(null) }
    var hasCenteredOnLiveGps by remember { mutableStateOf(false) }

    // Initial reverse geocode on launch
    LaunchedEffect(initialLat, initialLng) {
        val addr = OsmGeocoderHelper.reverseGeocode(context, initialLat, initialLng)
        currentPickedAddress = addr
    }

    // Automatically focus on driver's live GPS location as soon as it arrives
    LaunchedEffect(driverLiveLocation, mapViewInstance) {
        val loc = driverLiveLocation
        val mv = mapViewInstance
        if (loc != null && loc.latitude != 0.0 && mv != null && !hasCenteredOnLiveGps) {
            hasCenteredOnLiveGps = true
            val geoPoint = GeoPoint(loc.latitude, loc.longitude)
            mv.controller.setZoom(17.0)
            mv.controller.setCenter(geoPoint)
            val addr = OsmGeocoderHelper.reverseGeocode(context, loc.latitude, loc.longitude)
            currentPickedAddress = addr
        }
    }

    fun triggerReverseGeocode() {
        reverseGeocodeJob?.cancel()
        reverseGeocodeJob = coroutineScope.launch {
            delay(500)
            val mv = mapViewInstance ?: return@launch
            val center = mv.mapCenter ?: return@launch
            currentPickedAddress = "Locating address..."
            val addr = OsmGeocoderHelper.reverseGeocode(context, center.latitude, center.longitude)
            currentPickedAddress = addr
        }
    }

    fun performSearch() {
        val query = searchQuery.trim()
        if (query.isBlank()) return
        focusManager.clearFocus()
        keyboardController?.hide()
        isSearching = true
        coroutineScope.launch {
            val result = OsmGeocoderHelper.searchLocation(context, query)
            withContext(Dispatchers.Main) {
                isSearching = false
                if (result != null) {
                    val point = GeoPoint(result.latitude, result.longitude)
                    mapViewInstance?.controller?.apply {
                        setZoom(17.0)
                        animateTo(point)
                    }
                    currentPickedAddress = result.address
                } else {
                    Toast.makeText(context, "Location not found for: $query", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 1. Fullscreen MapView
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    isTilesScaledToDpi = true
                    isFlingEnabled = true
                    minZoomLevel = 4.0
                    maxZoomLevel = 20.0
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

                    val centerLat = if (initialLat != 0.0) initialLat else 21.0974
                    val centerLng = if (initialLng != 0.0) initialLng else 81.0336
                    controller.setZoom(17.0)
                    controller.setCenter(GeoPoint(centerLat, centerLng))

                    setOnTouchListener { v, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                                v.parent?.requestDisallowInterceptTouchEvent(true)
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                v.parent?.requestDisallowInterceptTouchEvent(false)
                            }
                        }
                        false
                    }

                    addMapListener(object : MapListener {
                        override fun onScroll(event: ScrollEvent?): Boolean {
                            triggerReverseGeocode()
                            return false
                        }

                        override fun onZoom(event: ZoomEvent?): Boolean {
                            triggerReverseGeocode()
                            return false
                        }
                    })

                    onResume()
                    mapViewInstance = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Fixed Center Pin
        Icon(
            imageVector = Icons.Default.Place,
            contentDescription = "Center Point Pin",
            tint = Color(0xFFEF4444),
            modifier = Modifier
                .align(Alignment.Center)
                .size(46.dp)
                .offset(y = (-23).dp)
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(8.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
        )

        // 3. Floating Top Bar with Search Field & Close
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF64748B)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "Pick Home Location",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = GlipNavyPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Search Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search colony, area or city...", fontSize = 13.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { performSearch() }),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = { performSearch() },
                                enabled = !isSearching
                            ) {
                                if (isSearching) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = Color(0xFF0284C7)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = Color(0xFF0284C7)
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 4. Floating Action Button: Re-center on My Current GPS Location
        FloatingActionButton(
            onClick = {
                val curGps = driverLiveLocation ?: locationManager.getBestLastKnownLocation()
                if (curGps != null && curGps.latitude != 0.0) {
                    val curLat = curGps.latitude
                    val curLng = curGps.longitude
                    mapViewInstance?.controller?.apply {
                        setZoom(17.0)
                        animateTo(GeoPoint(curLat, curLng))
                    }
                    coroutineScope.launch {
                        currentPickedAddress = "Locating address..."
                        val addr = OsmGeocoderHelper.reverseGeocode(context, curLat, curLng)
                        currentPickedAddress = addr
                    }
                } else {
                    locationManager.refreshGpsStatus()
                    locationManager.startTracking()
                    Toast.makeText(context, "Acquiring GPS location...", Toast.LENGTH_SHORT).show()
                }
            },
            shape = CircleShape,
            containerColor = Color(0xFF0284C7),
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 180.dp)
                .size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "My Live Location",
                modifier = Modifier.size(24.dp)
            )
        }

        // 5. Bottom Card: Display Selected Address & Confirm Button
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Selected Home Address",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentPickedAddress,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GlipNavyPrimary,
                    maxLines = 3
                )
                Spacer(modifier = Modifier.height(10.dp))
                GlipPrimaryButton(
                    text = "Confirm Home Address",
                    onClick = {
                        val center = mapViewInstance?.mapCenter
                        val lat = center?.latitude ?: initialLat
                        val lng = center?.longitude ?: initialLng
                        onLocationSelected(currentPickedAddress, lat, lng)
                    }
                )
            }
        }
    }
}
