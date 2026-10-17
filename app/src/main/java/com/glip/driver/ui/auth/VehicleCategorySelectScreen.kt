package com.glip.driver.ui.auth

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
    onRegistrationComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var isSubmitting by remember { mutableStateOf(false) }
    var selectedCategoryName by remember { mutableStateOf<String?>(null) }

    val vehicleCategories = remember {
        listOf(
            VehicleTypeItem(
                category = VehicleCategory.BIKE,
                title = "Bike / Motorcycle",
                capacity = 1,
                iconEmoji = "🏍️",
                description = "Solo passenger bike ride"
            ),
            VehicleTypeItem(
                category = VehicleCategory.AUTO_RICKSHAW,
                title = "Auto",
                capacity = 7,
                iconEmoji = "🛺",
                description = "Standard 7-seater route auto (₹27/km)"
            ),
            VehicleTypeItem(
                category = VehicleCategory.E_RICKSHAW,
                title = "E - Rickshaw",
                capacity = 5,
                iconEmoji = "🛺",
                description = "Electric battery rickshaw (5 passengers, ₹22/km)"
            ),
            VehicleTypeItem(
                category = VehicleCategory.CAR,
                title = "CAB",
                capacity = 4,
                iconEmoji = "🚗",
                description = "4 Passenger AC cab / car (₹31/km)"
            ),
            VehicleTypeItem(
                category = VehicleCategory.VAN,
                title = "Van",
                capacity = 7,
                iconEmoji = "🚐",
                description = "Omni / Eeco / Bolero / 7-seater MPV"
            ),
            VehicleTypeItem(
                category = VehicleCategory.MINI_BUS,
                title = "Mini bus",
                capacity = 20,
                iconEmoji = "🚌",
                description = "20 Seater shuttle or city mini-bus"
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

        // Step indicator & Top Bold Header
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

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Driver Registration",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = GlipNavyPrimary,
                fontSize = 26.sp
            )
        )

        Text(
            text = "Choose your vehicle type. Tap to complete registration.",
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFF64748B),
                fontSize = 13.sp
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Vehicle Category Cards (Tap on any card to proceed immediately)
        vehicleCategories.forEach { item ->
            val isBeingSelected = isSubmitting && selectedCategoryName == item.title

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable(enabled = !isSubmitting) {
                        isSubmitting = true
                        selectedCategoryName = item.title
                        coroutineScope.launch {
                            // 1. Construct Vehicle object with driver's Personal Name from Step 2
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
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isBeingSelected) Color(0xFFF0FDF4) else Color(0xFFF8FAFC)
                ),
                border = BorderStroke(
                    1.5.dp,
                    if (isBeingSelected) Color(0xFF16A34A) else Color(0xFFE2E8F0)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Emoji Badge
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White)
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = item.iconEmoji, fontSize = 26.sp)
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = item.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GlipNavyPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFDCFCE7),
                                    border = BorderStroke(1.dp, Color(0xFF86EFAC))
                                ) {
                                    Text(
                                        text = "${item.capacity} Seats",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF15803D),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.description,
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    if (isBeingSelected) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color(0xFF16A34A),
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Select",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
