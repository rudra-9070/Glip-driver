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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glip.driver.domain.model.Vehicle
import com.glip.driver.domain.model.VehicleCategory
import com.glip.driver.domain.repository.DriverRepository
import com.glip.driver.ui.components.GlipDemoBanner
import com.glip.driver.ui.components.GlipPrimaryButton
import com.glip.driver.ui.theme.GlipNavyPrimary
import kotlinx.coroutines.launch

private data class SetupVehicleItem(
    val category: VehicleCategory,
    val title: String,
    val capacity: Int,
    val iconEmoji: String
)

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

    val setupCategories = remember {
        listOf(
            SetupVehicleItem(VehicleCategory.AUTO_RICKSHAW, "Auto", 5, "🛺"),
            SetupVehicleItem(VehicleCategory.E_RICKSHAW, "E - Rickshaw", 4, "🛺"),
            SetupVehicleItem(VehicleCategory.CAR, "CAB", 4, "🚗"),
            SetupVehicleItem(VehicleCategory.VAN, "Van", 7, "🚐"),
            SetupVehicleItem(VehicleCategory.MINI_BUS, "Bus", 20, "🚌")
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

        // Vehicle Category Selector (2-Column Grid matching Screenshot)
        Text(
            text = "Select Vehicle Type",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = GlipNavyPrimary
            )
        )
        Spacer(modifier = Modifier.height(12.dp))

        setupCategories.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                rowItems.forEach { item ->
                    val isSelected = item.category == selectedCategory

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(125.dp)
                            .clickable {
                                selectedCategory = item.category
                            },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFFF0F9FF) else Color.White
                        ),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) Color(0xFF0284C7) else Color(0xFFE2E8F0)
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (isSelected) 3.dp else 1.dp
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp, vertical = 10.dp)
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF0284C7)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .align(Alignment.Center)
                            ) {
                                Text(
                                    text = item.iconEmoji,
                                    fontSize = 38.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = item.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color(0xFF0284C7) else GlipNavyPrimary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${item.capacity} Seats",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF64748B),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        GlipPrimaryButton(
            text = "Save Vehicle & Continue",
            onClick = {
                coroutineScope.launch {
                    val updated = Vehicle(
                        id = currentVehicle?.id ?: "veh_${System.currentTimeMillis() % 100000}",
                        displayName = selectedCategory.displayName,
                        category = selectedCategory,
                        registrationNumberDisplay = currentVehicle?.registrationNumberDisplay ?: "CG-08-0000",
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
