package com.glip.driver.ui.activity

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glip.driver.domain.model.DemoTrip
import com.glip.driver.domain.model.DemoTripStatus
import com.glip.driver.domain.repository.DriverRepository
import com.glip.driver.ui.components.GlipDemoBanner
import com.glip.driver.ui.theme.GlipAccentGreen
import com.glip.driver.ui.theme.GlipAccentTeal
import com.glip.driver.ui.theme.GlipErrorRed
import com.glip.driver.ui.theme.GlipNavyPrimary
import com.glip.driver.ui.theme.GlipWarningAmber
import kotlinx.coroutines.launch

@Composable
fun ActivityScreen(
    repository: DriverRepository,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val trips by repository.tripsFlow.collectAsState(initial = emptyList())
    var selectedFilter by remember { mutableStateOf("All") }
    val coroutineScope = rememberCoroutineScope()

    val filteredTrips = when (selectedFilter) {
        "Accepted" -> trips.filter { it.status == DemoTripStatus.ACCEPTED }
        "Completed" -> trips.filter { it.status == DemoTripStatus.COMPLETED }
        "Declined" -> trips.filter { it.status == DemoTripStatus.DECLINED }
        "Cancelled" -> trips.filter { it.status == DemoTripStatus.CANCELLED }
        else -> trips
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(16.dp)
    ) {
        // Header with Back Button
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                    text = "Driver Activity",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = GlipNavyPrimary
                    )
                )
                Text(
                    text = "Log of accepted, completed and cancelled trips",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B))
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        GlipDemoBanner()
        Spacer(modifier = Modifier.height(12.dp))

        // Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("All", "Accepted", "Completed", "Declined", "Cancelled").forEach { statusName ->
                FilterChip(
                    selected = selectedFilter == statusName,
                    onClick = { selectedFilter = statusName },
                    label = { Text(statusName, fontSize = 11.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredTrips.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No Activity Records Yet",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Accept or decline corridor requests from the 'Requests' tab to generate activity entries.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B))
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredTrips, key = { it.id }) { trip ->
                    TripActivityCard(
                        trip = trip,
                        onMarkCompleted = {
                            coroutineScope.launch {
                                repository.updateTripStatus(trip.id, DemoTripStatus.COMPLETED)
                            }
                        },
                        onMarkCancelled = {
                            coroutineScope.launch {
                                repository.updateTripStatus(trip.id, DemoTripStatus.CANCELLED)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TripActivityCard(
    trip: DemoTrip,
    onMarkCompleted: () -> Unit,
    onMarkCancelled: () -> Unit
) {
    val statusColor = when (trip.status) {
        DemoTripStatus.ACCEPTED -> GlipAccentGreen
        DemoTripStatus.COMPLETED -> GlipAccentTeal
        DemoTripStatus.DECLINED -> Color(0xFF94A3B8)
        DemoTripStatus.CANCELLED -> GlipErrorRed
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = trip.status.label,
                        color = statusColor,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = "Illustrative Fare: ₹${trip.illustrativeFare}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF475569)
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Origin -> Dropoff
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = trip.pickupName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = trip.dropoffName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${trip.requestedSeats} seat(s) • Scheduled: ${trip.scheduledTimeDisplay}",
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B))
            )

            // If Accepted, allow driver to Mark as Completed or Cancelled in demo
            if (trip.status == DemoTripStatus.ACCEPTED) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onMarkCancelled,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Cancel Trip", color = GlipErrorRed, fontSize = 12.sp)
                    }
                    Button(
                        onClick = onMarkCompleted,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = GlipNavyPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Complete Trip", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
