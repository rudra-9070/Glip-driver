package com.glip.driver.ui.onboarding

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glip.driver.domain.model.Vehicle
import com.glip.driver.domain.model.VehicleCategory
import com.glip.driver.domain.repository.DriverRepository
import com.glip.driver.ui.components.GlipDemoBanner
import com.glip.driver.ui.components.GlipPrimaryButton
import com.glip.driver.ui.theme.GlipNavyPrimary
import kotlinx.coroutines.launch

@Composable
fun VehicleSetupScreen(
    repository: DriverRepository,
    onSavedOrSkipped: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentVehicle by repository.vehicleFlow.collectAsState(initial = null)

    var selectedCategory by remember(currentVehicle) {
        mutableStateOf(currentVehicle?.category ?: VehicleCategory.AUTO_RICKSHAW)
    }

    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val categories = remember {
        listOf(
            VehicleCategory.AUTO_RICKSHAW,
            VehicleCategory.E_RICKSHAW,
            VehicleCategory.CAR,
            VehicleCategory.VAN,
            VehicleCategory.MINI_BUS,
            VehicleCategory.BIKE
        )
    }

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
                    text = "Vehicle Setup",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = GlipNavyPrimary
                    )
                )
                Text(
                    text = "Select your vehicle type",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B))
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Vehicle Category Selector
        Text(
            text = "Select Vehicle Type",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = GlipNavyPrimary
            )
        )
        Spacer(modifier = Modifier.height(10.dp))

        categories.forEach { category ->
            val isSelected = category == selectedCategory
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable {
                        selectedCategory = category
                    },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Color(0xFFF0FDF4) else Color(0xFFF8FAFC)
                ),
                border = BorderStroke(
                    1.5.dp,
                    if (isSelected) Color(0xFF10B981) else Color(0xFFE2E8F0)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = category.displayName,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = GlipNavyPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Standard capacity: ${category.defaultCapacity} passengers",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF64748B),
                                fontSize = 12.sp
                            )
                        )
                    }
                    if (isSelected) {
                        Text(
                            text = "✓ Selected",
                            color = Color(0xFF10B981),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        GlipPrimaryButton(
            text = "Save Vehicle & Continue",
            onClick = {
                coroutineScope.launch {
                    val updated = Vehicle(
                        id = currentVehicle?.id ?: "veh_${System.currentTimeMillis() % 100000}",
                        displayName = selectedCategory.displayName,
                        category = selectedCategory,
                        registrationNumberDisplay = currentVehicle?.registrationNumberDisplay ?: "DL-01-AB-1234",
                        totalCapacity = selectedCategory.defaultCapacity,
                        occupiedSeats = 0,
                        availableSeats = selectedCategory.defaultCapacity
                    )
                    repository.updateVehicle(updated)
                    val profile = repository.getProfile()
                    if (profile != null) {
                        com.glip.driver.data.remote.SupabaseClient.updateDriverSeats(
                            profile.id,
                            selectedCategory.defaultCapacity
                        )
                    }
                    onSavedOrSkipped()
                }
            }
        )
    }
}
