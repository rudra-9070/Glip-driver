package com.glip.driver.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glip.driver.ui.theme.GlipNavyPrimary

/**
 * Validates Indian vehicle registration plate against the 3 user-specified accepted formats:
 * 1. CG 08 VH 5308 (State + 1-2 digits RTO + 1-2 letters Series + 4 digits Number)
 * 2. CG 08 V 53 08 (State + 1-2 digits RTO + 1-2 letters Series + 2 digits + 2 digits)
 * 3. CG 08 5308 (State + 1-2 digits RTO + 4 digits Number)
 */
fun isValidIndianVehiclePlate(rawInput: String): Boolean {
    val clean = rawInput.trim().uppercase()
    val p1 = Regex("^[A-Z]{2}\\s*\\d{1,2}\\s*[A-Z]{1,2}\\s*\\d{4}$")
    val p2 = Regex("^[A-Z]{2}\\s*\\d{1,2}\\s*[A-Z]{1,2}\\s*\\d{2}\\s*\\d{2}$")
    val p3 = Regex("^[A-Z]{2}\\s*\\d{1,2}\\s*\\d{4}$")
    return p1.matches(clean) || p2.matches(clean) || p3.matches(clean)
}

@Composable
fun DriverDetailsScreen(
    initialName: String = "",
    onBack: () -> Unit,
    onNext: (fullName: String, vehiclePlate: String, licenseNumber: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var fullName by remember { mutableStateOf(initialName) }
    var vehiclePlate by remember { mutableStateOf("") }
    var licenseNumber by remember { mutableStateOf("") }
    var showPlateError by remember { mutableStateOf(false) }

    val isPlateValid = isValidIndianVehiclePlate(vehiclePlate)
    val isFormValid = fullName.trim().length >= 2 && isPlateValid

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

        // Step indicator & Top Prominent Bold Title
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFEFF6FF),
            border = BorderStroke(1.dp, Color(0xFFBAE6FD))
        ) {
            Text(
                text = "STEP 2 OF 3",
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
            text = "Enter your personal and vehicle details to continue.",
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFF64748B),
                fontSize = 13.sp
            )
        )

        Spacer(modifier = Modifier.height(28.dp))

        // 1. Personal Name Input
        Text(
            text = "Personal Name *",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = Color(0xFF334155)
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            placeholder = { Text("e.g. Ramesh Verma", color = Color(0xFF94A3B8)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Personal Name",
                    tint = Color(0xFF64748B)
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF0284C7),
                unfocusedBorderColor = Color(0xFFCBD5E1),
                focusedContainerColor = Color(0xFFF8FAFC),
                unfocusedContainerColor = Color(0xFFF8FAFC)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 2. Vehicle Plate Number Input (Matches Screenshot 1 UI)
        Text(
            text = "Vehicle Plate Number *",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = Color(0xFF334155)
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = vehiclePlate,
            onValueChange = {
                vehiclePlate = it.uppercase()
                showPlateError = it.isNotBlank() && !isValidIndianVehiclePlate(it)
            },
            placeholder = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🚗 ", fontSize = 14.sp)
                    Text("Vehicle Plate Number (e.g. CG 08 VH 5308)", color = Color(0xFF94A3B8), fontSize = 13.sp)
                }
            },
            leadingIcon = {
                Text("🚗", fontSize = 18.sp, modifier = Modifier.padding(start = 12.dp, end = 4.dp))
            },
            trailingIcon = {
                if (isPlateValid) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Valid",
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            singleLine = true,
            isError = showPlateError,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (isPlateValid) Color(0xFF16A34A) else Color(0xFF0284C7),
                unfocusedBorderColor = if (isPlateValid) Color(0xFF16A34A) else Color(0xFFCBD5E1),
                focusedContainerColor = Color(0xFFF8FAFC),
                unfocusedContainerColor = Color(0xFFF8FAFC)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Validation Helper Text
        if (showPlateError) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Allowed formats: CG 08 VH 5308, CG 08 V 53 08, or CG 08 5308",
                color = Color(0xFFDC2626),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        } else if (isPlateValid) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "✓ Valid Vehicle Registration Number",
                color = Color(0xFF16A34A),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        } else {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Supported: CG 08 VH 5308, CG 08 V 53 08, CG 08 5308",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 3. License Number Input (Optional - Matches Screenshot 1 UI)
        Text(
            text = "License Number (Optional)",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = Color(0xFF334155)
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = licenseNumber,
            onValueChange = { licenseNumber = it.uppercase() },
            placeholder = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🪪 ", fontSize = 14.sp)
                    Text("License Number (Optional)", color = Color(0xFF94A3B8), fontSize = 13.sp)
                }
            },
            leadingIcon = {
                Text("🪪", fontSize = 18.sp, modifier = Modifier.padding(start = 12.dp, end = 4.dp))
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF0284C7),
                unfocusedBorderColor = Color(0xFFCBD5E1),
                focusedContainerColor = Color(0xFFF8FAFC),
                unfocusedContainerColor = Color(0xFFF8FAFC)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Bottom Action Buttons: Back + Next (Matches Screenshot 1 UI)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ↶ Back Button (Outlined, rounded, curved arrow)
            OutlinedButton(
                onClick = onBack,
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

            // Next Button (Filled, rounded)
            Button(
                onClick = {
                    if (isFormValid) {
                        onNext(fullName.trim(), vehiclePlate.trim().uppercase(), licenseNumber.trim())
                    } else if (!isPlateValid) {
                        showPlateError = true
                    }
                },
                enabled = isFormValid,
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
                Text(
                    text = "Next",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isFormValid) Color.White else Color(0xFF94A3B8)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
