package com.glip.driver.ui.onboarding

import android.content.Context
import android.location.Geocoder
import android.view.MotionEvent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.location.Location
import com.glip.driver.domain.model.DriverRoute
import com.glip.driver.domain.repository.DriverRepository
import com.glip.driver.location.DriverLocationManager
import com.glip.driver.map.createDriverVehiclePuck
import org.osmdroid.views.overlay.Marker
import com.glip.driver.map.OsmGeocoderHelper
import com.glip.driver.map.RouteCoordinateResolver
import com.glip.driver.ui.components.GlipPrimaryButton
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
import java.util.Locale
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class SavedDestinationItem(
    val id: String,
    val title: String,
    val address: String,
    val iconCategory: String, // "HOME", "BUS", "CAR"
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val isDeletable: Boolean = false
)

private fun loadSavedDestinations(prefs: SharedPreferences): List<SavedDestinationItem> {
    val raw = prefs.getString("saved_destinations_json", null) ?: return emptyList()
    val list = mutableListOf<SavedDestinationItem>()
    try {
        val arr = JSONArray(raw)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                SavedDestinationItem(
                    id = obj.optString("id", "dest_$i"),
                    title = obj.optString("title", ""),
                    address = obj.optString("address", ""),
                    iconCategory = obj.optString("iconCategory", "CAR"),
                    lat = obj.optDouble("lat", 0.0),
                    lng = obj.optDouble("lng", 0.0),
                    isDeletable = obj.optBoolean("isDeletable", true)
                )
            )
        }
    } catch (_: Exception) {}
    return list
}

private fun persistSavedDestinations(prefs: SharedPreferences, items: List<SavedDestinationItem>) {
    try {
        val arr = JSONArray()
        for (item in items) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("address", item.address)
                put("iconCategory", item.iconCategory)
                put("lat", item.lat)
                put("lng", item.lng)
                put("isDeletable", item.isDeletable)
            }
            arr.put(obj)
        }
        prefs.edit().putString("saved_destinations_json", arr.toString()).apply()
    } catch (_: Exception) {}
}

@Composable
fun RouteSetupScreen(
    repository: DriverRepository,
    onSavedOrSkipped: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentRoute by repository.routeFlow.collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var destination by remember(currentRoute) {
        mutableStateOf(currentRoute?.destinationName ?: "")
    }
    var destLat by remember(currentRoute) {
        mutableStateOf(currentRoute?.destLat ?: 0.0)
    }
    var destLng by remember(currentRoute) {
        mutableStateOf(currentRoute?.destLng ?: 0.0)
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showMapPicker by remember { mutableStateOf(false) }
    var showSameLocationDialog by remember { mutableStateOf(false) }

    val locationManager = remember { DriverLocationManager(context.applicationContext) }
    val liveLocation by locationManager.currentLocation.collectAsState()
    val isGpsEnabled by locationManager.isGpsEnabled.collectAsState()

    LaunchedEffect(Unit) {
        if (DriverLocationManager.hasLocationPermission(context)) {
            locationManager.startTracking()
            locationManager.refreshGpsStatus()
        }
    }

    val prefs = remember {
        context.getSharedPreferences("glip_driver_demo_prefs", Context.MODE_PRIVATE)
    }

    // Saved Destinations with On/Off Toggle (User-created routes)
    val savedDestinations = remember {
        mutableStateListOf<SavedDestinationItem>().apply {
            addAll(loadSavedDestinations(prefs))
        }
    }

    if (showSameLocationDialog) {
        AlertDialog(
            onDismissRequest = { showSameLocationDialog = false },
            title = {
                Text(
                    text = "Invalid Destination",
                    fontWeight = FontWeight.Bold,
                    color = GlipNavyPrimary
                )
            },
            text = {
                Text(
                    text = "Your Live location and destination is same. Please change and choose a different destination.",
                    color = Color(0xFF334155),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showSameLocationDialog = false }
                ) {
                    Text("OK", fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .verticalScroll(scrollState)
        ) {
            // Top Header with Back Action (Only "Set Your Route", no extra text)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onSavedOrSkipped) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = GlipNavyPrimary
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Set Your Route",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = GlipNavyPrimary
                )
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Single Final Destination Input Box named "Go To Destination" with manual typing & Map button
        OutlinedTextField(
            value = destination,
            onValueChange = {
                destination = it
                errorMessage = null
            },
            label = { Text("Go To Destination") },
            placeholder = { Text("Type address or tap map") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = Color(0xFFEF4444)
                )
            },
            trailingIcon = {
                Surface(
                    onClick = { showMapPicker = true },
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = "Pick on Map",
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Map",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0284C7)
                        )
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                color = Color(0xFFFEE2E2),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = errorMessage!!,
                    color = GlipErrorRed,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Save Route Button - Adds to Saved Destinations with On/Off Button (Does NOT force to Map immediately)
        GlipPrimaryButton(
            text = "Save Route",
            onClick = {
                val destText = destination.trim()
                if (destText.isBlank()) {
                    errorMessage = "Please enter or pick a destination."
                    return@GlipPrimaryButton
                }

                coroutineScope.launch {
                    val (resolvedLat, resolvedLng) = if (destLat != 0.0 && destLng != 0.0) {
                        Pair(destLat, destLng)
                    } else {
                        val geo = OsmGeocoderHelper.searchLocation(context, destText)
                        if (geo != null) {
                            Pair(geo.latitude, geo.longitude)
                        } else {
                            RouteCoordinateResolver.resolveCoordinates(destText)
                        }
                    }

                    // Check if resolved destination is same as current live location
                    val curGps = liveLocation ?: locationManager.getBestLastKnownLocation()
                    if (curGps != null && curGps.latitude != 0.0 && resolvedLat != 0.0) {
                        val results = FloatArray(1)
                        Location.distanceBetween(curGps.latitude, curGps.longitude, resolvedLat, resolvedLng, results)
                        if (results[0] < 200) {
                            showSameLocationDialog = true
                            return@launch
                        }
                    }

                    // Always add to Saved Destinations so it appears below with an On/Off toggle
                    val existingIndex = savedDestinations.indexOfFirst {
                        it.title.equals(destText, ignoreCase = true) || it.address.equals(destText, ignoreCase = true)
                    }
                    if (existingIndex < 0) {
                        savedDestinations.add(
                            0,
                            SavedDestinationItem(
                                id = "dest_${System.currentTimeMillis()}",
                                title = destText,
                                address = destText,
                                iconCategory = "CAR",
                                lat = resolvedLat,
                                lng = resolvedLng,
                                isDeletable = true
                            )
                        )
                        persistSavedDestinations(prefs, savedDestinations)
                    }

                    // Clear input box so user clearly sees the newly added destination in the list below
                    destination = ""
                    destLat = 0.0
                    destLng = 0.0
                    errorMessage = null

                    Toast.makeText(
                        context,
                        "$destText Saved Destinations me add ho gaya! Map par set karne ke liye ON karein.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Saved Destinations with On/Off Toggle
        Text(
            text = "Saved Destinations",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = GlipNavyPrimary
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (savedDestinations.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Text(
                    text = "No saved destinations yet. Type or pick a destination above and tap 'Save Route'.",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(14.dp)
                )
            }
        } else {

        savedDestinations.forEach { item ->
            val isActive = currentRoute != null &&
                    currentRoute?.isActive == true &&
                    (currentRoute?.destinationName.equals(item.title, ignoreCase = true) ||
                            currentRoute?.destinationName.equals(item.address, ignoreCase = true))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isActive) Color(0xFFF0FDF4) else Color(0xFFF8FAFC)
                ),
                border = BorderStroke(
                    1.5.dp,
                    if (isActive) Color(0xFF10B981) else Color(0xFFE2E8F0)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Category Icon
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isActive) Color(0xFFDCFCE7) else Color(0xFFE2E8F0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (item.iconCategory) {
                                    "HOME" -> Icons.Default.Home
                                    "BUS" -> Icons.Default.DirectionsBus
                                    else -> Icons.Default.DirectionsCar
                                },
                                contentDescription = null,
                                tint = if (isActive) Color(0xFF16A34A) else Color(0xFF475569),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isActive) Color(0xFF15803D) else GlipNavyPrimary
                                    )
                                )
                                if (isActive) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = Color(0xFF16A34A),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "ACTIVE",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = item.address,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF64748B),
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    // ON / OFF Toggle Switch
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = isActive,
                            onCheckedChange = { enable ->
                                if (enable) {
                                    destination = item.address
                                    destLat = item.lat
                                    destLng = item.lng

                                    coroutineScope.launch {
                                        val (resolvedLat, resolvedLng) = if (item.lat != 0.0 && item.lng != 0.0) {
                                            Pair(item.lat, item.lng)
                                        } else {
                                            RouteCoordinateResolver.resolveCoordinates(item.address)
                                        }

                                        val updated = DriverRoute(
                                            id = "rte_${item.id}",
                                            originName = "Current Location",
                                            destinationName = item.address,
                                            destLat = resolvedLat,
                                            destLng = resolvedLng,
                                            isActive = true
                                        )
                                        repository.updateRoute(updated)
                                        val prof = repository.getProfile()
                                        if (prof != null) {
                                            com.glip.driver.data.remote.SupabaseClient.syncDriverRoute(
                                                driverIdentifier = prof.id,
                                                mobileNumber = prof.mobileNumber,
                                                destinationName = item.address,
                                                destLat = resolvedLat,
                                                destLng = resolvedLng,
                                                isActive = true
                                            )
                                        }
                                        Toast.makeText(
                                            context,
                                            "${item.title} activated! Co-passengers can now find your vehicle.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        onSavedOrSkipped()
                                    }
                                } else {
                                    // Deactivate route
                                    coroutineScope.launch {
                                        val updated = currentRoute?.copy(isActive = false) ?: DriverRoute(
                                            originName = "Current Location",
                                            destinationName = item.address,
                                            isActive = false
                                        )
                                        repository.updateRoute(updated)
                                        val prof = repository.getProfile()
                                        if (prof != null) {
                                            com.glip.driver.data.remote.SupabaseClient.syncDriverRoute(
                                                driverIdentifier = prof.id,
                                                mobileNumber = prof.mobileNumber,
                                                destinationName = item.address,
                                                destLat = 0.0,
                                                destLng = 0.0,
                                                isActive = false
                                            )
                                        }
                                        Toast.makeText(context, "${item.title} deactivated.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF10B981),
                                uncheckedThumbColor = Color(0xFFCBD5E1),
                                uncheckedTrackColor = Color(0xFFF1F5F9)
                            )
                        )

                        if (item.isDeletable) {
                            IconButton(onClick = {
                                savedDestinations.remove(item)
                                persistSavedDestinations(prefs, savedDestinations)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Route",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        }

        Spacer(modifier = Modifier.height(16.dp))
        }

        // Full Screen Interactive Map Picker Overlay (Zero Dialog, No Window Panning/Jumping)
        if (showMapPicker) {
            val currentGps = liveLocation ?: locationManager.getBestLastKnownLocation()
            val initialPickerLat = if (currentGps != null && currentGps.latitude != 0.0) currentGps.latitude else 21.0974
            val initialPickerLng = if (currentGps != null && currentGps.longitude != 0.0) currentGps.longitude else 81.0336

            DestinationMapPickerView(
                driverLiveLocation = liveLocation,
                locationManager = locationManager,
                initialLat = initialPickerLat,
                initialLng = initialPickerLng,
                onLocationSelected = { pickedAddress, lat, lng ->
                    destination = pickedAddress
                    destLat = lat
                    destLng = lng
                    errorMessage = null
                    showMapPicker = false
                },
                onDismiss = { showMapPicker = false }
            )
        }
    }
}

/**
 * Interactive Full-screen Map Picker View with locked top search bar, live GPS driver tracking,
 * center crosshair pin, real-time geocoding, and Blue Current Location FAB.
 */
@Composable
fun DestinationMapPickerView(
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
    var showSameLocationDialog by remember { mutableStateOf(false) }
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

    if (showSameLocationDialog) {
        AlertDialog(
            onDismissRequest = { showSameLocationDialog = false },
            title = {
                Text(
                    text = "Invalid Destination",
                    fontWeight = FontWeight.Bold,
                    color = GlipNavyPrimary
                )
            },
            text = {
                Text(
                    text = "Your Live location and destination is same. Please change and choose a different destination.",
                    color = Color(0xFF334155),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showSameLocationDialog = false }
                ) {
                    Text("OK", fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    fun triggerReverseGeocode() {
        reverseGeocodeJob?.cancel()
        reverseGeocodeJob = coroutineScope.launch {
            delay(500) // Debounce so it only fires when the user stops dragging
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
        // 1. Rock-solid Fullscreen Native OpenStreetMap (Base Layer - Never shifts or jumps)
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
                    val initialPoint = GeoPoint(centerLat, centerLng)
                    controller.setZoom(17.0)
                    controller.setCenter(initialPoint)

                    // Driver live vehicle marker
                    if (driverLiveLocation != null && driverLiveLocation.latitude != 0.0) {
                        val puck = Marker(this).apply {
                            position = GeoPoint(driverLiveLocation.latitude, driverLiveLocation.longitude)
                            icon = createDriverVehiclePuck(ctx)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            title = "Your Current Location"
                        }
                        overlays.add(puck)
                    }

                    // Smooth touch handling without interrupting drag
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

                    // Listen to map movement to live reverse-geocode center position
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
            update = { mv ->
                if (driverLiveLocation != null && driverLiveLocation.latitude != 0.0) {
                    var driverMarker = mv.overlays.filterIsInstance<Marker>().find { it.title == "Your Current Location" }
                    if (driverMarker == null) {
                        driverMarker = Marker(mv).apply {
                            icon = createDriverVehiclePuck(mv.context)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            title = "Your Current Location"
                        }
                        mv.overlays.add(driverMarker)
                    }
                    driverMarker.position = GeoPoint(driverLiveLocation.latitude, driverLiveLocation.longitude)
                    mv.invalidate()
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Fixed Center Pin & Target Shadow Dot
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

        // 3. Floating Top Bar: Title, Close Button & Full Search Bar (Never jumps or hides!)
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 6.dp,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Select Destination on Map",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = GlipNavyPrimary
                        )
                        Text(
                            text = "Point crosshair pin to your target location",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Search Bar with Search Icon, Clear, and Go
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search city, station, or landmark...", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF0284C7)
                            )
                        } else if (searchQuery.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { performSearch() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Text(
                                        "Go",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0284C7),
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = { performSearch() }
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 4. Blue Current Location FAB Button (Above bottom card)
        FloatingActionButton(
            onClick = {
                val curLat: Double
                val curLng: Double
                if (driverLiveLocation != null && driverLiveLocation.latitude != 0.0) {
                    curLat = driverLiveLocation.latitude
                    curLng = driverLiveLocation.longitude
                } else {
                    val best = locationManager.getBestLastKnownLocation()
                    if (best != null && best.latitude != 0.0) {
                        curLat = best.latitude
                        curLng = best.longitude
                    } else {
                        curLat = 0.0
                        curLng = 0.0
                    }
                }

                if (curLat != 0.0 && curLng != 0.0) {
                    val myPoint = GeoPoint(curLat, curLng)
                    mapViewInstance?.controller?.apply {
                        setZoom(17.0)
                        animateTo(myPoint)
                    }
                    coroutineScope.launch {
                        currentPickedAddress = "Locating address..."
                        val addr = OsmGeocoderHelper.reverseGeocode(context, curLat, curLng)
                        currentPickedAddress = addr
                    }
                } else {
                    locationManager.refreshGpsStatus()
                    locationManager.startTracking()
                    Toast.makeText(
                        context,
                        "Device GPS search kar raha hai... Kripya 2 second ruk kar dubara tap karein.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 180.dp)
                .size(46.dp),
            containerColor = Color(0xFF0284C7),
            contentColor = Color.White,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "My Current Location",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        // 5. Floating Bottom Action Card: Selected Location + Confirm Destination Button
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .navigationBarsPadding(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier.padding(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Selected Location",
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
                    text = "Confirm Destination",
                    onClick = {
                        val center = mapViewInstance?.mapCenter
                        val lat = center?.latitude ?: initialLat
                        val lng = center?.longitude ?: initialLng

                        // Check if picked location is same as current live location
                        val curGps = driverLiveLocation ?: locationManager.getBestLastKnownLocation()
                        if (curGps != null && curGps.latitude != 0.0 && lat != 0.0) {
                            val results = FloatArray(1)
                            Location.distanceBetween(curGps.latitude, curGps.longitude, lat, lng, results)
                            if (results[0] < 200) {
                                showSameLocationDialog = true
                                return@GlipPrimaryButton
                            }
                        }

                        onLocationSelected(currentPickedAddress, lat, lng)
                    }
                )
            }
        }
    }
}
