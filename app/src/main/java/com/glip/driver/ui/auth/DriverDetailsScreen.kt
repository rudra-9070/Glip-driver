package com.glip.driver.ui.auth

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.IconButton
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
 * Auto-formats Indian vehicle registration plate with hyphens between segments:
 * e.g.
 * - CG - 08 - VH - 4565
 * - CG - 05 - V - 5308
 * - CG - 02 - 6548
 */
fun formatIndianVehiclePlate(input: String): String {
    val raw = input.uppercase().filter { it.isLetterOrDigit() }
    if (raw.isEmpty()) return ""

    // BH Series: e.g. 22 BH 1234 AA
    if (raw.length >= 2 && raw.take(2).all { it.isDigit() }) {
        val year = raw.take(2)
        var rest = raw.drop(2)
        if (rest.startsWith("BH")) {
            val bh = "BH"
            rest = rest.drop(2)
            val num = rest.takeWhile { it.isDigit() }.take(4)
            rest = rest.drop(num.length)
            val series = rest.takeWhile { it.isLetter() }.take(2)
            val parts = listOf(year, bh, num, series).filter { it.isNotEmpty() }
            return parts.joinToString(" - ")
        }
    }

    // Standard State code (first 2 letters, e.g. CG)
    if (raw.length <= 2) {
        return raw
    }

    val state = raw.take(2)
    var rest = raw.drop(2)

    val rtoDigits = rest.takeWhile { it.isDigit() }.take(2)
    if (rtoDigits.isEmpty()) {
        return if (rest.isNotEmpty()) "$state - $rest" else state
    }
    rest = rest.drop(rtoDigits.length)

    if (rest.isEmpty()) {
        return "$state - $rtoDigits"
    }

    val seriesLetters = rest.takeWhile { it.isLetter() }.take(2)
    if (seriesLetters.isNotEmpty()) {
        rest = rest.drop(seriesLetters.length)
        val numberDigits = rest.takeWhile { it.isDigit() }.take(4)
        return if (numberDigits.isNotEmpty()) {
            "$state - $rtoDigits - $seriesLetters - $numberDigits"
        } else {
            "$state - $rtoDigits - $seriesLetters"
        }
    } else {
        val numberDigits = rest.takeWhile { it.isDigit() }.take(4)
        return if (numberDigits.isNotEmpty()) {
            "$state - $rtoDigits - $numberDigits"
        } else {
            "$state - $rtoDigits"
        }
    }
}

/**
 * Validates Indian vehicle registration plate against the 3 user-specified accepted formats:
 * 1. CG - 08 - VH - 4565 (State + 1-2 digits RTO + 1-2 letters Series + 1-4 digits Number)
 * 2. CG - 05 - V - 5308 or CG - 05 - V 5308 (State + 1-2 digits RTO + 1 letter Series + 1-4 digits Number)
 * 3. CG - 02 - 6548 (State + 1-2 digits RTO + 1-4 digits Number)
 * Also supports BH series and handles user-entered spaces or hyphens gracefully.
 */
fun isValidIndianVehiclePlate(rawInput: String): Boolean {
    val clean = rawInput.replace(" ", "").replace("-", "").trim().uppercase()
    if (clean.length < 5 || clean.length > 10) return false

    val pWithSeries = Regex("^[A-Z]{2}\\d{1,2}[A-Z]{1,2}\\d{1,4}$")
    val pWithoutSeries = Regex("^[A-Z]{2}\\d{1,2}\\d{1,4}$")
    val pBharatSeries = Regex("^\\d{2}BH\\d{1,4}[A-Z]{1,2}$")

    return pWithSeries.matches(clean) || pWithoutSeries.matches(clean) || pBharatSeries.matches(clean)
}

@Composable
fun DriverDetailsScreen(
    initialName: String = "",
    onBack: () -> Unit,
    onNext: (fullName: String, vehiclePlate: String, licenseNumber: String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBack()
    }

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

        // Top Navigation & Step Indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBack,
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
                    text = "STEP 2 OF 3",
                    color = Color(0xFF0284C7),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
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
            onValueChange = { input ->
                val formatted = formatIndianVehiclePlate(input)
                vehiclePlate = formatted
                showPlateError = formatted.isNotBlank() && !isValidIndianVehiclePlate(formatted)
            },
            placeholder = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🚗 ", fontSize = 14.sp)
                    Text("e.g. CG - 08 - VH - 4565", color = Color(0xFF94A3B8), fontSize = 13.sp)
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
                text = "Allowed: CG - 08 - VH - 4565, CG - 05 - V - 5308, or CG - 02 - 6548",
                color = Color(0xFFDC2626),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        } else if (isPlateValid) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "✓ Valid Vehicle Registration Number ($vehiclePlate)",
                color = Color(0xFF16A34A),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        } else {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Supported: CG - 08 - VH - 4565, CG - 05 - V - 5308, CG - 02 - 6548",
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
