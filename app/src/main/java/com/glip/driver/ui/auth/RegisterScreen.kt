package com.glip.driver.ui.auth

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glip.driver.domain.model.DriverProfile
import com.glip.driver.domain.model.DriverStatus
import com.glip.driver.domain.repository.DriverRepository
import com.glip.driver.domain.usecase.InputValidationUtil
import com.glip.driver.ui.components.GlipDemoBanner
import com.glip.driver.ui.components.GlipPrimaryButton
import com.glip.driver.ui.theme.GlipAccentGreen
import com.glip.driver.ui.theme.GlipAccentTeal
import com.glip.driver.ui.theme.GlipErrorRed
import com.glip.driver.ui.theme.GlipNavyDark
import com.glip.driver.ui.theme.GlipNavyPrimary
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun RegisterScreen(
    repository: DriverRepository,
    onRegistrationSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var fullName by remember { mutableStateOf("") }
    var mobileNumber by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var enteredOtp by remember { mutableStateOf("") }

    var generatedDemoOtp by remember { mutableStateOf<String?>(null) }
    var isOtpRequested by remember { mutableStateOf(false) }
    var attemptsLeft by remember { mutableIntStateOf(3) }
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
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Driver Registration",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = GlipNavyPrimary
            )
        )
        Text(
            text = "Create your local driver demo profile",
            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF64748B))
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Full Name
        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it; errorMessage = null },
            label = { Text("Full Name *") },
            placeholder = { Text("e.g. Ramesh Kumar") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isOtpRequested
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Mobile Number
        OutlinedTextField(
            value = mobileNumber,
            onValueChange = {
                if (it.length <= 10 && it.all { char -> char.isDigit() }) {
                    mobileNumber = it
                    errorMessage = null
                }
            },
            label = { Text("Mobile Number (India +91) *") },
            placeholder = { Text("10-digit mobile number") },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isOtpRequested
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Email (Optional)
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email Address (Optional)") },
            placeholder = { Text("e.g. ramesh@example.com") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isOtpRequested
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (!isOtpRequested) {
            GlipPrimaryButton(
                text = "Request Demo OTP",
                onClick = {
                    val nameValidation = InputValidationUtil.validateFullName(fullName)
                    if (!nameValidation.isValid) {
                        errorMessage = nameValidation.errorMessage
                        return@GlipPrimaryButton
                    }
                    val mobileValidation = InputValidationUtil.validateMobileNumber(mobileNumber)
                    if (!mobileValidation.isValid) {
                        errorMessage = mobileValidation.errorMessage
                        return@GlipPrimaryButton
                    }

                    // Generate safe random 6-digit demo OTP
                    val randomOtp = (Random.nextInt(900000) + 100000).toString()
                    generatedDemoOtp = randomOtp
                    isOtpRequested = true
                    attemptsLeft = 3
                    errorMessage = null
                }
            )
        } else {
            // Demo OTP Display Area (Developer/Demo Inspection)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "DEMO OTP GENERATED (LOCAL SIMULATION)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = GlipNavyPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "No real SMS sent. For evaluation, use code:",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF475569))
                    )
                    Text(
                        text = generatedDemoOtp ?: "------",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = GlipAccentGreen,
                            letterSpacing = 4.sp
                        ),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Text(
                        text = "Verification attempts remaining: $attemptsLeft",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (attemptsLeft <= 1) GlipErrorRed else Color(0xFF64748B)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = enteredOtp,
                onValueChange = {
                    if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                        enteredOtp = it
                        errorMessage = null
                    }
                },
                label = { Text("Enter 6-digit Demo OTP") },
                leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            GlipPrimaryButton(
                text = "Verify OTP & Continue",
                onClick = {
                    val otpCheck = InputValidationUtil.validateOtp(enteredOtp)
                    if (!otpCheck.isValid) {
                        errorMessage = otpCheck.errorMessage
                        return@GlipPrimaryButton
                    }

                    if (enteredOtp == generatedDemoOtp) {
                        coroutineScope.launch {
                            val newProfile = DriverProfile(
                                fullName = fullName.trim(),
                                mobileNumber = mobileNumber.trim(),
                                email = email.trim().ifBlank { null },
                                status = DriverStatus.DEMO_ACTIVE
                            )
                            repository.saveProfile(newProfile)
                            onRegistrationSuccess()
                        }
                    } else {
                        attemptsLeft -= 1
                        if (attemptsLeft <= 0) {
                            isOtpRequested = false
                            enteredOtp = ""
                            generatedDemoOtp = null
                            errorMessage = "Maximum attempts exceeded. Please request a new OTP."
                        } else {
                            errorMessage = "Incorrect OTP. $attemptsLeft attempt(s) remaining."
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = {
                    isOtpRequested = false
                    enteredOtp = ""
                    errorMessage = null
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Edit Mobile Number / Request New OTP")
            }
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
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

        Row(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Already registered?",
                style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF64748B))
            )
            TextButton(onClick = onNavigateToLogin) {
                Text(
                    text = "Login to Demo",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = GlipAccentTeal,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}
