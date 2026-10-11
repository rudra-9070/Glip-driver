package com.glip.driver.ui.requests

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.glip.driver.domain.model.CompatibilityLevel
import com.glip.driver.domain.model.PassengerRequest
import com.glip.driver.domain.repository.DriverRepository
import com.glip.driver.ui.components.GlipDemoBanner
import com.glip.driver.ui.theme.GlipAccentGreen
import com.glip.driver.ui.theme.GlipAccentTeal
import com.glip.driver.ui.theme.GlipErrorRed
import com.glip.driver.ui.theme.GlipNavyDark
import com.glip.driver.ui.theme.GlipNavyPrimary
import com.glip.driver.ui.theme.GlipWarningAmber
import kotlinx.coroutines.launch

@Composable
fun RequestsScreen(
    repository: DriverRepository,
    modifier: Modifier = Modifier
) {
    val requests by repository.requestsFlow.collectAsState(initial = emptyList())
    val vehicle by repository.vehicleFlow.collectAsState(initial = null)

    var selectedFilter by remember { mutableStateOf("All") }
    var selectedRequestForDetails by remember { mutableStateOf<PassengerRequest?>(null) }
    var requestToConfirmAccept by remember { mutableStateOf<PassengerRequest?>(null) }
    var actionMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    val filteredRequests = when (selectedFilter) {
        "Good Match" -> requests.filter { it.matchResult.level == CompatibilityLevel.GOOD_MATCH }
        "Partial" -> requests.filter { it.matchResult.level == CompatibilityLevel.PARTIAL_MATCH }
        "Available Seats" -> requests.filter { it.requestedSeats <= (vehicle?.availableSeats ?: 0) }
        else -> requests
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Potential Passenger Requests",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = GlipNavyPrimary
            )
        )
        Text(
            text = "Simulated passengers seeking seats along your route corridor",
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B))
        )

        Spacer(modifier = Modifier.height(10.dp))
        GlipDemoBanner(message = "Simulated matches. Accepting updates your demo seat count locally.")
        Spacer(modifier = Modifier.height(10.dp))

        // Filter Chips Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Good Match", "Partial", "Available Seats").forEach { filterName ->
                FilterChip(
                    selected = selectedFilter == filterName,
                    onClick = { selectedFilter = filterName },
                    label = { Text(filterName, fontSize = 12.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (actionMessage != null) {
            Surface(
                color = Color(0xFFEFF6FF),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                Text(
                    text = actionMessage!!,
                    color = Color(0xFF1D4ED8),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        // Requests List or Empty State
        if (filteredRequests.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No Requests Found",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "All demo requests have been reviewed or filter condition excluded them.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B))
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredRequests, key = { it.id }) { request ->
                    RequestCard(
                        request = request,
                        availableSeats = vehicle?.availableSeats ?: 0,
                        onViewDetails = { selectedRequestForDetails = request },
                        onAccept = { requestToConfirmAccept = request },
                        onDecline = {
                            coroutineScope.launch {
                                repository.declineRequest(request.id)
                                actionMessage = "Request ${request.id} declined in demo activity."
                            }
                        }
                    )
                }
            }
        }
    }

    // Confirmation Modal Before Accepting Request
    if (requestToConfirmAccept != null) {
        val req = requestToConfirmAccept!!
        AlertDialog(
            onDismissRequest = { requestToConfirmAccept = null },
            title = {
                Text(
                    text = "Confirm Demo Seat Allocation?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column {
                    Text("Simulated Passenger: ${req.passengerAlias}")
                    Text("Pickup: ${req.pickupName}")
                    Text("Drop-off: ${req.dropoffName}")
                    Text("Seats: ${req.requestedSeats} seat(s)")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Note: This is a demo match. No real passenger will be alerted and no actual fare will be processed.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B))
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val result = repository.acceptRequest(req.id)
                            if (result.isSuccess) {
                                actionMessage = "Demo match accepted! ${req.requestedSeats} seat(s) reserved."
                            } else {
                                actionMessage = result.exceptionOrNull()?.message ?: "Acceptance failed"
                            }
                            requestToConfirmAccept = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GlipNavyPrimary)
                ) {
                    Text("Confirm Demo Match")
                }
            },
            dismissButton = {
                TextButton(onClick = { requestToConfirmAccept = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Request Details Dialog
    if (selectedRequestForDetails != null) {
        val detail = selectedRequestForDetails!!
        AlertDialog(
            onDismissRequest = { selectedRequestForDetails = null },
            title = {
                Text("Request Analysis • ${detail.id}")
            },
            text = {
                Column {
                    Text("Alias: ${detail.passengerAlias}", fontWeight = FontWeight.SemiBold)
                    Text("Pickup Corridor: ${detail.pickupName}")
                    Text("Drop-off Corridor: ${detail.dropoffName}")
                    Text("Pickup Timing: ${detail.approximateTime}")
                    Text("Requested Seats: ${detail.requestedSeats}")
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Route Matching Reason:", fontWeight = FontWeight.Bold)
                    Text(detail.matchResult.explanation, color = Color(0xFF475569))
                    if (detail.matchResult.estimatedDetourMinutes != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Illustrative Detour Estimate: ~${detail.matchResult.estimatedDetourMinutes} mins",
                            color = GlipAccentTeal,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedRequestForDetails = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun RequestCard(
    request: PassengerRequest,
    availableSeats: Int,
    onViewDetails: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val isGoodMatch = request.matchResult.level == CompatibilityLevel.GOOD_MATCH
    val isPartial = request.matchResult.level == CompatibilityLevel.PARTIAL_MATCH
    val isCapacityOk = request.requestedSeats <= availableSeats

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Passenger alias, seats, Fare
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isGoodMatch -> GlipAccentGreen
                                    isPartial -> GlipWarningAmber
                                    else -> GlipErrorRed
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = request.passengerAlias,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "${request.requestedSeats} Seat(s)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "₹${request.illustrativeFare} (Demo Fare)",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = GlipNavyPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Origin -> Destination
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = request.pickupName,
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
                    text = request.dropoffName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Compatibility Badge & Explanation
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = when {
                    isGoodMatch -> Color(0xFFF0FDF4)
                    isPartial -> Color(0xFFFFFBEB)
                    else -> Color(0xFFFEF2F2)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = request.matchResult.level.displayName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isGoodMatch -> Color(0xFF166534)
                                isPartial -> Color(0xFF92400E)
                                else -> Color(0xFF991B1B)
                            }
                        )
                    )
                    Text(
                        text = request.matchResult.explanation,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF475569),
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDecline,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Decline", color = Color(0xFF64748B), fontSize = 13.sp)
                }

                Button(
                    onClick = onAccept,
                    enabled = (isGoodMatch || isPartial) && isCapacityOk,
                    modifier = Modifier.weight(1.5f),
                    colors = ButtonDefaults.buttonColors(containerColor = GlipNavyPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (!isCapacityOk) "No Capacity" else "Accept Demo Match",
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
