package com.glip.driver.ui.onboarding

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
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
import com.glip.driver.domain.model.DriverRoute
import com.glip.driver.domain.repository.DriverRepository
import com.glip.driver.domain.usecase.InputValidationUtil
import com.glip.driver.map.RouteCoordinateResolver
import com.glip.driver.ui.components.GlipPrimaryButton
import com.glip.driver.ui.theme.GlipAccentTeal
import com.glip.driver.ui.theme.GlipErrorRed
import com.glip.driver.ui.theme.GlipNavyDark
import com.glip.driver.ui.theme.GlipNavyPrimary
import kotlinx.coroutines.launch

data class FavoriteRoutePreset(
    val id: String,
    val title: String,
    val iconCategory: String, // HOME, BUS, TRAIN, CAR
    val origin: String,
    val destination: String,
    val departureTime: String,
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

    var origin by remember(currentRoute) {
        mutableStateOf(currentRoute?.originName ?: "Motipur")
    }
    var destination by remember(currentRoute) {
        mutableStateOf(currentRoute?.destinationName ?: "Baruraj")
    }
    var departureTime by remember(currentRoute) {
        mutableStateOf(currentRoute?.departureTimeDisplay ?: "08:30 AM")
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showAddFavoriteDialog by remember { mutableStateOf(false) }
    var customFavoriteName by remember { mutableStateOf("") }

    // Persistent Favorite Corridors (Destination Mode)
    val favoriteRoutes = remember {
        mutableStateListOf(
            FavoriteRoutePreset(
                id = "fav_home",
                title = "Home (Ghar Wapsi)",
                iconCategory = "HOME",
                origin = "Motipur Chowk",
                destination = "Baruraj Home",
                departureTime = "06:30 PM"
            ),
            FavoriteRoutePreset(
                id = "fav_bus",
                title = "Bus Stand Express",
                iconCategory = "BUS",
                origin = "Motipur",
                destination = "Muzaffarpur Bairia Bus Stand",
                departureTime = "08:30 AM"
            ),
            FavoriteRoutePreset(
                id = "fav_train",
                title = "Railway Station Route",
                iconCategory = "TRAIN",
                origin = "Baruraj",
                destination = "Motipur Railway Station",
                departureTime = "09:15 AM"
            ),
            FavoriteRoutePreset(
                id = "fav_daily",
                title = "Daily Commute",
                iconCategory = "CAR",
                origin = "Motipur Main Road",
                destination = "Baruraj Chowk",
                departureTime = "10:00 AM"
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
        // Top Header with Back Action
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
            Column {
                Text(
                    text = "Set Your Route",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = GlipNavyPrimary
                    )
                )
                Text(
                    text = "Define your planned journey corridor",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B))
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 1. Starting Point Input Box
        OutlinedTextField(
            value = origin,
            onValueChange = {
                origin = it
                errorMessage = null
            },
            label = { Text("Starting Point") },
            placeholder = { Text("e.g. Motipur") },
            leadingIcon = {
                Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF10B981))
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Final Destination Input Box
        OutlinedTextField(
            value = destination,
            onValueChange = {
                destination = it
                errorMessage = null
            },
            label = { Text("Final Destination") },
            placeholder = { Text("e.g. Baruraj") },
            leadingIcon = {
                Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFFEF4444))
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Approximate Departure Time Input Box
        OutlinedTextField(
            value = departureTime,
            onValueChange = { departureTime = it },
            label = { Text("Approximate Departure Time") },
            placeholder = { Text("e.g. 08:30 AM") },
            leadingIcon = {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = GlipNavyPrimary)
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

        // 4. Save Button
        GlipPrimaryButton(
            text = "Save Route",
            onClick = {
                val validation = InputValidationUtil.validateRoute(origin, destination)
                if (!validation.isValid) {
                    errorMessage = validation.errorMessage
                    return@GlipPrimaryButton
                }

                coroutineScope.launch {
                    val (oLat, oLng) = RouteCoordinateResolver.resolveCoordinates(origin)
                    val (dLat, dLng) = RouteCoordinateResolver.resolveCoordinates(destination)

                    val updated = DriverRoute(
                        id = currentRoute?.id ?: "rte_active_01",
                        originName = origin.trim(),
                        destinationName = destination.trim(),
                        departureTimeDisplay = departureTime.trim(),
                        originLat = oLat,
                        originLng = oLng,
                        destLat = dLat,
                        destLng = dLng
                    )
                    repository.updateRoute(updated)
                    Toast.makeText(context, "Route saved: ${origin.trim()} ➔ ${destination.trim()}", Toast.LENGTH_SHORT).show()
                    onSavedOrSkipped()
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Save Current Route as Favorite Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = {
                    customFavoriteName = "${origin.trim()} to ${destination.trim()}"
                    showAddFavoriteDialog = true
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Save as Favorite",
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "Save as Favorite Route",
                    color = Color(0xFF0284C7),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 5. Favorite & Quick Routes Section (Destination Mode)
        Text(
            text = "Favorite & Quick Routes (Destination Mode)",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = GlipNavyPrimary
            )
        )
        Text(
            text = "Turn ON a route to broadcast your journey corridor to co-passengers",
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B), fontSize = 12.sp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // List of Favorite Routes with On/Off Toggle
        favoriteRoutes.forEach { preset ->
            val isActive = currentRoute != null &&
                    currentRoute?.originName.equals(preset.origin, ignoreCase = true) &&
                    currentRoute?.destinationName.equals(preset.destination, ignoreCase = true)

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
                                imageVector = when (preset.iconCategory) {
                                    "HOME" -> Icons.Default.Home
                                    "BUS" -> Icons.Default.DirectionsBus
                                    "TRAIN" -> Icons.Default.Train
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
                                    text = preset.title,
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
                                text = "${preset.origin} ➔ ${preset.destination}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF334155),
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Text(
                                text = "Departure: ${preset.departureTime}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF94A3B8),
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
                                    origin = preset.origin
                                    destination = preset.destination
                                    departureTime = preset.departureTime

                                    coroutineScope.launch {
                                        val (oLat, oLng) = RouteCoordinateResolver.resolveCoordinates(preset.origin)
                                        val (dLat, dLng) = RouteCoordinateResolver.resolveCoordinates(preset.destination)

                                        val updated = DriverRoute(
                                            id = "rte_${preset.id}",
                                            originName = preset.origin,
                                            destinationName = preset.destination,
                                            departureTimeDisplay = preset.departureTime,
                                            originLat = oLat,
                                            originLng = oLng,
                                            destLat = dLat,
                                            destLng = dLng
                                        )
                                        repository.updateRoute(updated)
                                        Toast.makeText(
                                            context,
                                            "${preset.title} activated! Co-passengers can now find your vehicle.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } else {
                                    // Deactivate to default
                                    coroutineScope.launch {
                                        val updated = DriverRoute(
                                            id = "rte_standby",
                                            originName = "Motipur",
                                            destinationName = "Baruraj",
                                            departureTimeDisplay = "08:30 AM",
                                            originLat = 26.1739,
                                            originLng = 85.1818,
                                            destLat = 26.1465,
                                            destLng = 85.0782
                                        )
                                        repository.updateRoute(updated)
                                        Toast.makeText(context, "${preset.title} deactivated.", Toast.LENGTH_SHORT).show()
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

                        if (preset.isDeletable) {
                            IconButton(onClick = { favoriteRoutes.remove(preset) }) {
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

    // Add Custom Favorite Route Dialog
    if (showAddFavoriteDialog) {
        AlertDialog(
            onDismissRequest = { showAddFavoriteDialog = false },
            title = {
                Text(
                    text = "Save as Favorite Route",
                    fontWeight = FontWeight.Bold,
                    color = GlipNavyPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Give this corridor a quick name (e.g. 'Ghar ka Route' or 'Market Special'):",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B))
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = customFavoriteName,
                        onValueChange = { customFavoriteName = it },
                        label = { Text("Route Label") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Path: ${origin.trim()} ➔ ${destination.trim()}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = GlipNavyPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (customFavoriteName.isNotBlank() && origin.isNotBlank() && destination.isNotBlank()) {
                            favoriteRoutes.add(
                                FavoriteRoutePreset(
                                    id = "fav_custom_${System.currentTimeMillis()}",
                                    title = customFavoriteName.trim(),
                                    iconCategory = "CAR",
                                    origin = origin.trim(),
                                    destination = destination.trim(),
                                    departureTime = departureTime.trim(),
                                    isDeletable = true
                                )
                            )
                            Toast.makeText(context, "Favorite route added!", Toast.LENGTH_SHORT).show()
                        }
                        showAddFavoriteDialog = false
                    }
                ) {
                    Text("Save", fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddFavoriteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
