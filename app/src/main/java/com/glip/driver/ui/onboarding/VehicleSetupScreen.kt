package com.glip.driver.ui.onboarding

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsCar
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glip.driver.domain.model.Vehicle
import com.glip.driver.domain.model.VehicleCategory
import com.glip.driver.domain.repository.DriverRepository
import com.glip.driver.domain.usecase.InputValidationUtil
import com.glip.driver.ui.components.GlipDemoBanner
import com.glip.driver.ui.components.GlipPrimaryButton
import com.glip.driver.ui.theme.GlipAccentTeal
import com.glip.driver.ui.theme.GlipErrorRed
import com.glip.driver.ui.theme.GlipNavyPrimary
import kotlinx.coroutines.launch

@Composable
fun VehicleSetupScreen(
    repository: DriverRepository,
    onSavedOrSkipped: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentVehicle by repository.vehicleFlow.collectAsState(initial = null)

    var displayName by remember(currentVehicle) {
        mutableStateOf(currentVehicle?.displayName ?: "Bajaj RE Compact Auto")
    }
    var selectedCategory by remember(currentVehicle) {
        mutableStateOf(currentVehicle?.category ?: VehicleCategory.AUTO_RICKSHAW)
    }
    var capacity by remember(currentVehicle) {
        mutableIntStateOf(currentVehicle?.totalCapacity ?: 3)
    }
    var occupied by remember(currentVehicle) {
        mutableIntStateOf(currentVehicle?.occupiedSeats ?: 1)
    }
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
                    text = "Step 1: Vehicle Setup",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = GlipNavyPrimary
                    )
                )
                Text(
                    text = "Configure your vehicle & seats",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B))
                )
            }
            TextButton(onClick = onSavedOrSkipped) {
                Text("Skip", color = Color(0xFF64748B))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Vehicle Category Selector
        Text(
            text = "Select Vehicle Type",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(8.dp))

        VehicleCategory.values().forEach { category ->
            val isSelected = category == selectedCategory
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable {
                        selectedCategory = category
                        capacity = category.defaultCapacity
                        if (occupied > capacity) occupied = 0
                    },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Color(0xFFF0FDF4) else Color(0xFFF8FAFC)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    if (isSelected) Color(0xFF10B981) else Color(0xFFE2E8F0)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = category.displayName,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = GlipNavyPrimary
                            )
                        )
                        Text(
                            text = "Standard capacity: ${category.defaultCapacity} passengers",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF64748B),
                                fontSize = 11.sp
                            )
                        )
                    }
                    if (isSelected) {
                        Text(
                            text = "✓ Selected",
                            color = Color(0xFF10B981),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Vehicle Display Name
        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text("Vehicle Display Name / Model") },
            leadingIcon = { Icon(Icons.Default.DirectionsCar, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Seat Capacity Adjuster
        CounterAdjuster(
            title = "Total Passenger Capacity",
            subtitle = "Maximum passenger seats inside this vehicle",
            value = capacity,
            onIncrement = { capacity += 1 },
            onDecrement = { if (capacity > 1) { capacity -= 1; if (occupied > capacity) occupied = capacity } }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Currently Occupied Seats Adjuster
        CounterAdjuster(
            title = "Currently Occupied Passenger Seats",
            subtitle = "Seats already taken by other commuters or passengers",
            value = occupied,
            onIncrement = { if (occupied < capacity) occupied += 1 },
            onDecrement = { if (occupied > 0) occupied -= 1 }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Summary Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Available Seats For Route Pooling",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Formula: Capacity ($capacity) - Occupied ($occupied)",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B))
                    )
                }
                Text(
                    text = "${(capacity - occupied).coerceAtLeast(0)}",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = GlipAccentTeal
                    )
                )
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
            text = "Save Vehicle & Continue",
            onClick = {
                val validation = InputValidationUtil.validateVehicleSeats(capacity, occupied)
                if (!validation.isValid) {
                    errorMessage = validation.errorMessage
                    return@GlipPrimaryButton
                }

                coroutineScope.launch {
                    val updated = Vehicle(
                        id = currentVehicle?.id ?: "veh_demo_01",
                        displayName = displayName.trim().ifBlank { selectedCategory.displayName },
                        category = selectedCategory,
                        registrationNumberDisplay = "DL-01-AB-1234",
                        totalCapacity = capacity,
                        occupiedSeats = occupied
                    )
                    repository.updateVehicle(updated)
                    onSavedOrSkipped()
                }
            }
        )
    }
}

@Composable
private fun CounterAdjuster(
    title: String,
    subtitle: String,
    value: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = GlipNavyPrimary
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF64748B),
                        fontSize = 11.sp
                    )
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDecrement) {
                    Text("−", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = GlipNavyPrimary))
                }
                Text(
                    text = "$value",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(onClick = onIncrement) {
                    Icon(Icons.Default.Add, contentDescription = "Increment", tint = GlipNavyPrimary)
                }
            }
        }
    }
}
