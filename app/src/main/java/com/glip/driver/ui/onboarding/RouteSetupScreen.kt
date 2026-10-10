package com.glip.driver.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glip.driver.domain.model.DriverRoute
import com.glip.driver.domain.model.RouteDirection
import com.glip.driver.domain.model.RouteStop
import com.glip.driver.domain.repository.DriverRepository
import com.glip.driver.domain.usecase.InputValidationUtil
import com.glip.driver.ui.components.GlipDemoBanner
import com.glip.driver.ui.components.GlipPrimaryButton
import com.glip.driver.ui.theme.GlipAccentTeal
import com.glip.driver.ui.theme.GlipErrorRed
import com.glip.driver.ui.theme.GlipNavyPrimary
import kotlinx.coroutines.launch

@Composable
fun RouteSetupScreen(
    repository: DriverRepository,
    onSavedOrSkipped: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentRoute by repository.routeFlow.collectAsState(initial = null)

    var origin by remember(currentRoute) {
        mutableStateOf(currentRoute?.originName ?: "Connaught Place (Inner Circle)")
    }
    var destination by remember(currentRoute) {
        mutableStateOf(currentRoute?.destinationName ?: "Noida Sector 18 (Atta Market)")
    }
    var departureTime by remember(currentRoute) {
        mutableStateOf(currentRoute?.departureTimeDisplay ?: "08:45 AM")
    }
    var selectedDirection by remember(currentRoute) {
        mutableStateOf(currentRoute?.direction ?: RouteDirection.EASTBOUND)
    }

    val intermediateStops = remember(currentRoute) {
        mutableStateListOf<String>().apply {
            currentRoute?.intermediateStops?.map { it.name }?.let { addAll(it) }
        }
    }
    var newStopInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(20.dp)
            .verticalScroll(scrollState)
    ) {
        GlipDemoBanner()
        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Step 2: Planned Route Setup",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = GlipNavyPrimary
                    )
                )
                Text(
                    text = "Define the corridor you are already travelling along",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B))
                )
            }
            TextButton(onClick = onSavedOrSkipped) {
                Text("Skip", color = Color(0xFF64748B))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Origin
        OutlinedTextField(
            value = origin,
            onValueChange = { origin = it; errorMessage = null },
            label = { Text("Starting Point (Origin) *") },
            placeholder = { Text("e.g. Connaught Place") },
            leadingIcon = { Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF10B981)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Destination
        OutlinedTextField(
            value = destination,
            onValueChange = { destination = it; errorMessage = null },
            label = { Text("Final Destination *") },
            placeholder = { Text("e.g. Noida Sector 18") },
            leadingIcon = { Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFFEF4444)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Departure Time
        OutlinedTextField(
            value = departureTime,
            onValueChange = { departureTime = it },
            label = { Text("Approximate Departure Time") },
            placeholder = { Text("e.g. 08:30 AM") },
            leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = GlipNavyPrimary) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Route Direction
        Text(
            text = "Corridor Direction",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(RouteDirection.EASTBOUND, RouteDirection.WESTBOUND, RouteDirection.NORTHBOUND).forEach { dir ->
                val isSelected = dir == selectedDirection
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedDirection = dir },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (isSelected) GlipAccentTeal else Color(0xFFE2E8F0)
                    )
                ) {
                    Text(
                        text = dir.label,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) GlipNavyPrimary else Color(0xFF64748B)
                        ),
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Intermediate Stops
        Text(
            text = "Intermediate Stops Along Route",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = "Add transit stops to enable passenger matching along the corridor",
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B), fontSize = 11.sp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        intermediateStops.forEachIndexed { index, stopName ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Stop ${index + 1}:",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B))
                    )
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text(
                        text = stopName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                    )
                }
                IconButton(onClick = { intermediateStops.removeAt(index) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove Stop", tint = Color(0xFF94A3B8))
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newStopInput,
                onValueChange = { newStopInput = it },
                placeholder = { Text("Add waypoint stop") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    if (newStopInput.isNotBlank()) {
                        intermediateStops.add(newStopInput.trim())
                        newStopInput = ""
                    }
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Stop", tint = GlipNavyPrimary)
            }
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                color = Color(0xFFFEE2E2),
                shape = RoundedCornerShape(6.dp),
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

        Spacer(modifier = Modifier.height(24.dp))

        GlipPrimaryButton(
            text = "Save Route & Open Dashboard",
            onClick = {
                val validation = InputValidationUtil.validateRoute(origin, destination)
                if (!validation.isValid) {
                    errorMessage = validation.errorMessage
                    return@GlipPrimaryButton
                }

                coroutineScope.launch {
                    val stops = intermediateStops.mapIndexed { idx, name ->
                        RouteStop(
                            id = "stop_$idx",
                            name = name,
                            orderIndex = idx + 1,
                            latitude = 28.61 + (idx * 0.01),
                            longitude = 77.25 + (idx * 0.01)
                        )
                    }
                    val updated = DriverRoute(
                        id = currentRoute?.id ?: "rte_demo_01",
                        originName = origin.trim(),
                        destinationName = destination.trim(),
                        intermediateStops = stops,
                        departureTimeDisplay = departureTime.trim(),
                        direction = selectedDirection
                    )
                    repository.updateRoute(updated)
                    onSavedOrSkipped()
                }
            }
        )
    }
}
