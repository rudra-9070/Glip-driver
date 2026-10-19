package com.glip.driver.ui.profile

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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glip.driver.domain.model.DriverProfile
import com.glip.driver.domain.repository.DriverRepository
import com.glip.driver.ui.components.GlipDemoBanner
import com.glip.driver.ui.components.GlipSectionHeader
import com.glip.driver.ui.theme.GlipAccentTeal
import com.glip.driver.ui.theme.GlipErrorRed
import com.glip.driver.ui.theme.GlipNavyPrimary
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    repository: DriverRepository,
    onBack: () -> Unit = {},
    onEditVehicle: () -> Unit,
    onEditRoute: () -> Unit,
    onNavigateToParcel: () -> Unit,
    onNavigateToSchool: () -> Unit,
    onNavigateToDiagnostics: () -> Unit,
    onLoggedOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile by repository.profileFlow.collectAsState(initial = null)
    val vehicle by repository.vehicleFlow.collectAsState(initial = null)
    val route by repository.routeFlow.collectAsState(initial = null)

    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showClearDataConfirm by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Back Button Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
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
            Text(
                text = "Driver Profile",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = GlipNavyPrimary
                )
            )
        }

        // Driver Identity Header Card (Normal Box)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEFF6FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Driver Avatar",
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile?.fullName ?: "Driver",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = GlipNavyPrimary
                        )
                    )
                    val rawMobile = profile?.mobileNumber?.trim() ?: ""
                    val cleanMobile = rawMobile.replace(Regex("[^0-9]"), "").takeLast(10)
                    val fullMobileDisplay = if (cleanMobile.length == 10) "+91 $cleanMobile" else if (rawMobile.isNotBlank()) rawMobile else "+91 9876543210"
                    Text(
                        text = fullMobileDisplay,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
                Surface(
                    color = Color(0xFFDCFCE7),
                    border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = profile?.status?.label ?: "Active",
                        color = Color(0xFF15803D),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        GlipDemoBanner()
        Spacer(modifier = Modifier.height(16.dp))

        // Vehicle & Route Management Options
        GlipSectionHeader(title = "Configurations")

        SettingsItemRow(
            icon = Icons.Default.Place,
            title = "Corridor Route",
            subtitle = "${route?.originName} → ${route?.destinationName}",
            onClick = onEditRoute
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Future Services / Another Services
        GlipSectionHeader(title = "Another Services")

        SettingsItemRow(
            icon = Icons.Default.DirectionsCar,
            title = "Parcel Delivery",
            subtitle = "Coming soon — extra earnings on regular trips",
            onClick = onNavigateToParcel
        )

        SettingsItemRow(
            icon = Icons.Default.Star,
            title = "School Transportation",
            subtitle = "Dedicated route pooling registration for students",
            onClick = onNavigateToSchool
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Information & Privacy
        GlipSectionHeader(title = "Support & Legal")

        SettingsItemRow(
            icon = Icons.Default.Info,
            title = "Help & Driver Support",
            subtitle = "Platform FAQ, pooling etiquette & contact",
            onClick = { showHelpDialog = true }
        )

        SettingsItemRow(
            icon = Icons.Default.BugReport,
            title = "Diagnostics & Error Logs",
            subtitle = "View crash logs, device environment & error monitor",
            onClick = onNavigateToDiagnostics
        )

        SettingsItemRow(
            icon = Icons.Default.Lock,
            title = "Privacy & Regulatory Notice",
            subtitle = "Data processing disclaimer for Indian mobility MVP",
            onClick = { showPrivacyDialog = true }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Account actions
        OutlinedButton(
            onClick = {
                coroutineScope.launch {
                    repository.logout()
                    onLoggedOut()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.ExitToApp, contentDescription = null, tint = GlipNavyPrimary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Logout", color = GlipNavyPrimary)
        }

        Spacer(modifier = Modifier.height(10.dp))

        TextButton(
            onClick = { showClearDataConfirm = true },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Icon(Icons.Default.Delete, contentDescription = null, tint = GlipErrorRed, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Reset & Clear All Data", color = GlipErrorRed)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Glip Driver v1.0.1 • Built with Jetpack Compose & M3",
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8), fontSize = 11.sp),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(20.dp))
    }

    // Privacy Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy & Regulatory Notice") },
            text = {
                Column {
                    Text(
                        "1. Local Scope: This application operates on device simulation. No live geolocation tracking, background monitoring, or payment processing occurs.\n\n" +
                        "2. Driver Verification: Commercial carpooling, ride-sharing, or pooling in India is subject to state motor vehicle regulations, permits, and verification protocols. Live passenger onboarding requires verified commercial documentation.\n\n" +
                        "3. Data Minimization: We collect only the profile name and mobile number locally. No SMS, contacts, or camera access is requested in this build.",
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp, color = Color(0xFF475569))
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) { Text("Acknowledge") }
            }
        )
    }

    // Help Dialog
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("Glip Driver Help & Support") },
            text = {
                Text(
                    "How Route Pooling Works:\n\n" +
                    "• Configure your planned journey corridor from Origin to Destination.\n" +
                    "• Keep your availability set to 'Available'.\n" +
                    "• Review matched co-passengers travelling along the same route.\n" +
                    "• Accept requests within your available capacity to pool rides safely.\n\n" +
                    "For assistance, contact support@glipgo.com",
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp, color = Color(0xFF475569))
                )
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) { Text("Got It") }
            }
        )
    }

    // Clear Data Confirmation Dialog
    if (showClearDataConfirm) {
        AlertDialog(
            onDismissRequest = { showClearDataConfirm = false },
            title = { Text("Clear All Data?") },
            text = {
                Text("This will wipe the current driver profile, reset vehicle seats, reseed route requests, and return to the welcome screen.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            repository.clearAllDemoData()
                            showClearDataConfirm = false
                            onLoggedOut()
                        }
                    }
                ) {
                    Text("Clear All Data", color = GlipErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingsItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = GlipNavyPrimary, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B), fontSize = 11.sp)
                    )
                }
            }
            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color(0xFF94A3B8))
        }
    }
}
