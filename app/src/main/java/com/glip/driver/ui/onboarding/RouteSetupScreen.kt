package com.glip.driver.ui.onboarding

import android.content.Context
import android.location.Geocoder
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.glip.driver.domain.model.DriverRoute
import com.glip.driver.domain.repository.DriverRepository
import com.glip.driver.map.RouteCoordinateResolver
import com.glip.driver.ui.components.GlipPrimaryButton
import com.glip.driver.ui.theme.GlipErrorRed
import com.glip.driver.ui.theme.GlipNavyPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import java.util.Locale

data class SavedDestinationItem(
    val id: String,
    val title: String,
    val address: String,
    val iconCategory: String, // "HOME", "BUS", "CAR"
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val isDeletable: Boolean = false
)

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

    // Saved Destinations with On/Off Toggle
    val savedDestinations = remember {
        mutableStateListOf(
            SavedDestinationItem(
                id = "fav_home",
                title = "Home",
                address = "Home (Ghar Wapsi)",
                iconCategory = "HOME",
                lat = 21.1873,
                lng = 80.7618
            ),
            SavedDestinationItem(
                id = "fav_bus",
                title = "Bus Stand",
                address = "City Bus Stand",
                iconCategory = "BUS",
                lat = 21.0974,
                lng = 81.0336
            )
        )
    }

    Column(
        modifier = modifier
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

        // Save Route Button
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
                        RouteCoordinateResolver.resolveCoordinates(destText)
                    }

                    // Check if already in saved destinations list; if not, add it
                    val existingIndex = savedDestinations.indexOfFirst {
                        it.title.equals(destText, ignoreCase = true) || it.address.equals(destText, ignoreCase = true)
                    }
                    if (existingIndex < 0) {
                        savedDestinations.add(
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
                    }

                    val updated = DriverRoute(
                        id = currentRoute?.id ?: "rte_active_01",
                        originName = "Current Location",
                        destinationName = destText,
                        destLat = resolvedLat,
                        destLng = resolvedLng,
                        isActive = true
                    )
                    repository.updateRoute(updated)
                    Toast.makeText(context, "Route saved to $destText!", Toast.LENGTH_SHORT).show()
                    onSavedOrSkipped()
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
        Text(
            text = "Turn ON to broadcast your route to passengers along this way",
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B), fontSize = 12.sp)
        )

        Spacer(modifier = Modifier.height(12.dp))

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
                                        Toast.makeText(
                                            context,
                                            "${item.title} activated! Co-passengers can now find your vehicle.",
                                            Toast.LENGTH_SHORT
                                        ).show()
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
                            IconButton(onClick = { savedDestinations.remove(item) }) {
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

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Interactive Map Picker Dialog with Search Bar and Center Pin
    if (showMapPicker) {
        DestinationMapPickerDialog(
            initialLat = if (destLat != 0.0) destLat else 21.0974,
            initialLng = if (destLng != 0.0) destLng else 81.0336,
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

/**
 * Interactive Dialog: Map with search bar, draggable center pin, and real-time location resolution.
 */
@Composable
fun DestinationMapPickerDialog(
    initialLat: Double,
    initialLng: Double,
    onLocationSelected: (address: String, lat: Double, lng: Double) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var mapViewInstance by remember { mutableStateOf<MapView?>(null) }
    var currentPickedAddress by remember { mutableStateOf("Move map to point destination") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar: Title and Close button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Select Destination on Map",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
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

                // Search Bar in Map
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search landmark, station, or city...", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = {
                                    val query = searchQuery.trim()
                                    coroutineScope.launch(Dispatchers.IO) {
                                        try {
                                            val geocoder = Geocoder(context, Locale.getDefault())
                                            val results = geocoder.getFromLocationName(query, 1)
                                            if (!results.isNullOrEmpty()) {
                                                val found = results[0]
                                                withContext(Dispatchers.Main) {
                                                    val point = GeoPoint(found.latitude, found.longitude)
                                                    mapViewInstance?.controller?.animateTo(point)
                                                    currentPickedAddress = found.getAddressLine(0) ?: query
                                                }
                                            } else {
                                                val (lat, lng) = RouteCoordinateResolver.resolveCoordinates(query)
                                                withContext(Dispatchers.Main) {
                                                    mapViewInstance?.controller?.animateTo(GeoPoint(lat, lng))
                                                    currentPickedAddress = query
                                                }
                                            }
                                        } catch (e: Exception) {
                                            val (lat, lng) = RouteCoordinateResolver.resolveCoordinates(query)
                                            withContext(Dispatchers.Main) {
                                                mapViewInstance?.controller?.animateTo(GeoPoint(lat, lng))
                                                currentPickedAddress = query
                                            }
                                        }
                                    }
                                }) {
                                    Text("Go", fontWeight = FontWeight.Bold, color = Color(0xFF0284C7), fontSize = 12.sp)
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Map Canvas with Center Pin
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    AndroidView(
                        factory = { ctx ->
                            MapView(ctx).apply {
                                setTileSource(TileSourceFactory.MAPNIK)
                                setMultiTouchControls(true)
                                isTilesScaledToDpi = true
                                isFlingEnabled = true
                                maxZoomLevel = 26.0
                                zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

                                controller.setZoom(17.0)
                                val initialPoint = GeoPoint(initialLat, initialLng)
                                controller.setCenter(initialPoint)

                                // Prevent parent touch interception
                                setOnTouchListener { v, _ ->
                                    v.parent?.requestDisallowInterceptTouchEvent(true)
                                    false
                                }

                                onResume()
                                mapViewInstance = this
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Center Crosshair Pin
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = "Center Point Pin",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(46.dp)
                            .offset(y = (-23).dp)
                    )

                    // Center Pin Target Shadow Dot
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    )
                }

                // Bottom Action Bar: Confirm Location
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
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
                            maxLines = 2
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        GlipPrimaryButton(
                            text = "Confirm Destination",
                            onClick = {
                                val center = mapViewInstance?.mapCenter
                                val lat = center?.latitude ?: initialLat
                                val lng = center?.longitude ?: initialLng

                                coroutineScope.launch(Dispatchers.IO) {
                                    var resolved = currentPickedAddress
                                    try {
                                        val geocoder = Geocoder(context, Locale.getDefault())
                                        val addresses = geocoder.getFromLocation(lat, lng, 1)
                                        if (!addresses.isNullOrEmpty()) {
                                            resolved = addresses[0].locality ?: addresses[0].subLocality ?: addresses[0].featureName ?: addresses[0].getAddressLine(0) ?: "Destination (${String.format("%.4f, %.4f", lat, lng)})"
                                        }
                                    } catch (e: Exception) {
                                        if (resolved.contains("Move map", ignoreCase = true)) {
                                            resolved = "Destination (${String.format("%.4f, %.4f", lat, lng)})"
                                        }
                                    }

                                    withContext(Dispatchers.Main) {
                                        onLocationSelected(resolved, lat, lng)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
