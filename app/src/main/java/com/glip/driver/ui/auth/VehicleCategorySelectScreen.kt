package com.glip.driver.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.glip.driver.data.remote.SupabaseClient
import com.glip.driver.domain.model.DriverProfile
import com.glip.driver.domain.model.DriverStatus
import com.glip.driver.domain.model.Vehicle
import com.glip.driver.domain.model.VehicleCategory
import com.glip.driver.domain.repository.DriverRepository
import com.glip.driver.ui.theme.GlipNavyPrimary
import kotlinx.coroutines.launch

data class VehicleTypeItem(
    val category: VehicleCategory,
    val title: String,
    val capacity: Int,
    val iconEmoji: String,
    val description: String
)

@Composable
fun VehicleCategorySelectScreen(
    repository: DriverRepository,
    mobileNumber: String,
    fullName: String,
    vehiclePlate: String,
    licenseNumber: String,
    onBack: () -> Unit,
    onRegistrationComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBack()
    }

    val coroutineScope = rememberCoroutineScope()
    var isSubmitting by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<VehicleTypeItem?>(null) }

    val vehicleCategories = remember {
        listOf(
            // Row 1: Van | Auto
            VehicleTypeItem(
                category = VehicleCategory.VAN,
                title = "Van",
                capacity = 7,
                iconEmoji = "🚐",
                description = "Omni / Eeco / Bolero"
            ),
            VehicleTypeItem(
                category = VehicleCategory.AUTO_RICKSHAW,
                title = "Auto",
                capacity = 7,
                iconEmoji = "🛺",
                description = "Standard route auto"
            ),
            // Row 2: Bus | Bike
            VehicleTypeItem(
                category = VehicleCategory.MINI_BUS,
                title = "Bus",
                capacity = 20,
                iconEmoji = "🚌",
                description = "Mini bus or shuttle"
            ),
            VehicleTypeItem(
                category = VehicleCategory.BIKE,
                title = "Bike",
                capacity = 1,
                iconEmoji = "🛵",
                description = "Solo passenger ride"
            ),
            // Row 3: Car | E - Rickshaw
            VehicleTypeItem(
                category = VehicleCategory.CAR,
                title = "Car",
                capacity = 4,
                iconEmoji = "🚗",
                description = "4 Seater AC cab"
            ),
            VehicleTypeItem(
                category = VehicleCategory.E_RICKSHAW,
                title = "E - Rickshaw",
                capacity = 5,
                iconEmoji = "🛺",
                description = "5 Seater battery rickshaw"
            )
        )
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Top Navigation & Step Indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBack,
                enabled = !isSubmitting,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = GlipNavyPrimary
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFEFF6FF),
                border = BorderStroke(1.dp, Color(0xFFBAE6FD))
            ) {
                Text(
                    text = "STEP 3 OF 3",
                    color = Color(0xFF0284C7),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Choose Your Vehicle",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = GlipNavyPrimary,
                fontSize = 26.sp
            )
        )

        Text(
            text = "Select your vehicle type to complete registration.",
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFF64748B),
                fontSize = 13.sp
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 2-Column Compact Grid of Vehicles (Matching Screenshot)
        vehicleCategories.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                rowItems.forEach { item ->
                    val isSelected = selectedItem?.title == item.title

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(125.dp)
                            .clickable(enabled = !isSubmitting) {
                                selectedItem = item
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
                            // Top Right Selected Check Badge
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

                            // Centered Icon + Title + Capacity
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

        Spacer(modifier = Modifier.height(26.dp))

        // Bottom Action Buttons: Back + Save & Finish
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onBack,
                enabled = !isSubmitting,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, Color(0xFF475569)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF475569)
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "↶ ", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Back",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            Button(
                onClick = {
                    val item = selectedItem ?: return@Button
                    isSubmitting = true
                    coroutineScope.launch {
                        // 1. Construct Vehicle object with driver's details
                        val vehicle = Vehicle(
                            id = "veh_${System.currentTimeMillis()}",
                            displayName = fullName.ifBlank { item.title },
                            category = item.category,
                            registrationNumberDisplay = vehiclePlate.ifBlank { "CG 08 0000" },
                            totalCapacity = item.capacity,
                            occupiedSeats = 0,
                            availableSeats = item.capacity
                        )

                        // 2. Construct DriverProfile
                        val profile = DriverProfile(
                            id = "drv_${mobileNumber.takeLast(4)}",
                            fullName = fullName,
                            mobileNumber = mobileNumber,
                            licenseNumber = licenseNumber.ifBlank { null },
                            vehiclePlate = vehiclePlate,
                            status = DriverStatus.DEMO_ACTIVE
                        )

                        // 3. Save locally in repository
                        repository.saveProfile(profile)
                        repository.updateVehicle(vehicle)
                        repository.setAvailable(true)

                        // 4. Real-time push to Supabase backend
                        try {
                            SupabaseClient.upsertDriverProfile(
                                profile = profile,
                                vehicle = vehicle,
                                isOnline = true
                            )
                        } catch (e: Exception) {}

                        // 5. Navigate directly to Home screen
                        onRegistrationComplete()
                    }
                },
                enabled = selectedItem != null && !isSubmitting,
                modifier = Modifier
                    .weight(1.4f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GlipNavyPrimary,
                    disabledContainerColor = Color(0xFFE2E8F0),
                    disabledContentColor = Color(0xFF94A3B8)
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        text = "Save & Finish",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (selectedItem != null) Color.White else Color(0xFF94A3B8)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
