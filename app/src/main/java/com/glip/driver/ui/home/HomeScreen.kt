package com.glip.driver.ui.home

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
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glip.driver.data.repository.DemoDriverRepositoryImpl
import com.glip.driver.domain.model.CompatibilityLevel
import com.glip.driver.map.RouteCorridorMapPreview
import com.glip.driver.ui.components.GlipDemoBanner
import com.glip.driver.ui.components.GlipSafetyReminder
import com.glip.driver.ui.components.GlipSectionHeader
import com.glip.driver.ui.theme.GlipAccentGreen
import com.glip.driver.ui.theme.GlipAccentTeal
import com.glip.driver.ui.theme.GlipNavyDark
import com.glip.driver.ui.theme.GlipNavyPrimary
import com.glip.driver.ui.theme.GlipWarningAmber
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    repository: DemoDriverRepositoryImpl,
    onEditVehicle: () -> Unit,
    onEditRoute: () -> Unit,
    onViewAllRequests: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile by repository.profileFlow.collectAsState(initial = null)
    val vehicle by repository.vehicleFlow.collectAsState(initial = null)
    val route by repository.routeFlow.collectAsState(initial = null)
    val isAvailable by repository.isAvailableFlow.collectAsState(initial = true)
    val requests by repository.requestsFlow.collectAsState(initial = emptyList())

    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val compatibleCount = requests.count { it.matchResult.level == CompatibilityLevel.GOOD_MATCH }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Welcome, ${profile?.fullName ?: "Driver"}",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = GlipNavyPrimary
                    )
                )
                Text(
                    text = "Glip Driver • Route-Based Pooling",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B))
                )
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFE2E8F0)
            ) {
                Text(
                    text = "DEMO",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = GlipNavyPrimary
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        GlipDemoBanner()
        Spacer(modifier = Modifier.height(12.dp))

        // A. Prominent Availability Status Toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isAvailable) Color(0xFFF0FDF4) else Color(0xFFF1F5F9)
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                if (isAvailable) GlipAccentGreen else Color(0xFFCBD5E1)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(if (isAvailable) GlipAccentGreen else Color(0xFF94A3B8))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isAvailable) "Available for Route Sharing" else "Not Available (Offline)",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = GlipNavyPrimary
                            )
                        )
                        Text(
                            text = if (isAvailable) "Simulated local seat discovery is active" else "Corridor matching is paused locally",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF64748B),
                                fontSize = 11.sp
                            )
                        )
                    }
                }
                Switch(
                    checked = isAvailable,
                    onCheckedChange = { newStatus ->
                        coroutineScope.launch {
                            repository.setAvailable(newStatus)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = GlipAccentGreen
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // B. Current Vehicle & Capacity Snapshot
        GlipSectionHeader(
            title = "Current Vehicle & Seats",
            actionText = "Edit Vehicle",
            onActionClick = onEditVehicle
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = GlipNavyPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = vehicle?.displayName ?: "Vehicle",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${vehicle?.category?.displayName} • ${vehicle?.registrationNumberDisplay}",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B))
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Seat metrics row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    MetricBox(title = "Capacity", value = "${vehicle?.totalCapacity ?: 0}", color = GlipNavyPrimary)
                    MetricBox(title = "Occupied", value = "${vehicle?.occupiedSeats ?: 0}", color = Color(0xFF64748B))
                    MetricBox(title = "Available", value = "${vehicle?.availableSeats ?: 0}", color = GlipAccentGreen)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // C. Planned Route Corridor & Visual Map
        GlipSectionHeader(
            title = "Planned Route Corridor",
            actionText = "Edit Route",
            onActionClick = onEditRoute
        )

        route?.let { currentRoute ->
            RouteCorridorMapPreview(
                route = currentRoute,
                requests = requests
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // D. Compatible Passenger Requests Summary
        GlipSectionHeader(
            title = "Matched Requests Summary",
            actionText = "View All (${requests.size})",
            onActionClick = onViewAllRequests
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = GlipAccentTeal
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$compatibleCount Good Match(es) Available",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = GlipNavyPrimary
                            )
                        )
                    }
                    TextButton(onClick = onViewAllRequests) {
                        Text("Open Requests →", color = GlipAccentTeal, fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    text = "Passengers discovered travelling along compatible segments of your planned itinerary.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF64748B),
                        fontSize = 12.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        GlipSafetyReminder()
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun MetricBox(title: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color(0xFF64748B),
                fontSize = 11.sp
            )
        )
    }
}
