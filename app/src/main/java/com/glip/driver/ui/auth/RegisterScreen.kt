package com.glip.driver.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glip.driver.R
import com.glip.driver.data.remote.SupabaseClient
import com.glip.driver.domain.repository.DriverRepository
import com.glip.driver.domain.usecase.InputValidationUtil
import com.glip.driver.ui.components.GlipPrimaryButton
import com.glip.driver.ui.theme.GlipAccentTeal
import com.glip.driver.ui.theme.GlipErrorRed
import com.glip.driver.ui.theme.GlipNavyPrimary
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun RegisterScreen(
    repository: DriverRepository,
    onNavigateToOtp: (isLogin: Boolean, mobileNumber: String, fullName: String, generatedOtp: String) -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var mobileNumber by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isChecking by remember { mutableStateOf(false) }
    var alreadyRegistered by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        // Top App Logo
        Image(
            painter = painterResource(id = R.drawable.ic_glip_logo),
            contentDescription = "Haze Logo",
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(20.dp))
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Step 1 Indicator Pill
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFEFF6FF),
            border = BorderStroke(1.dp, Color(0xFFBAE6FD))
        ) {
            Text(
                text = "STEP 1 OF 3",
                color = Color(0xFF0284C7),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Top Header: "Driver Registration" (large & bold)
        Text(
            text = "Driver Registration",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = GlipNavyPrimary,
                fontSize = 26.sp
            )
        )

        Text(
            text = "Enter your mobile number to get verification OTP",
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                color = Color(0xFF64748B)
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Form Container
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Mobile Number *",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color(0xFF334155)
            )
            Spacer(modifier = Modifier.height(6.dp))

            // Mobile Number Input Box with +91
            OutlinedTextField(
                value = mobileNumber,
                onValueChange = {
                    if (it.length <= 10 && it.all { char -> char.isDigit() }) {
                        mobileNumber = it
                        errorMessage = null
                    }
                },
                placeholder = { Text("10-digit mobile number", color = Color(0xFF94A3B8)) },
                leadingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 12.dp, end = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "+91",
                            fontWeight = FontWeight.Bold,
                            color = GlipNavyPrimary,
                            fontSize = 14.sp
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF0284C7),
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedContainerColor = Color(0xFFF8FAFC),
                    unfocusedContainerColor = Color(0xFFF8FAFC)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = Color(0xFFFEE2E2),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFFECACA)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = errorMessage!!,
                            color = GlipErrorRed,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 18.sp
                            )
                        )
                        if (alreadyRegistered) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = onNavigateToLogin,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                            ) {
                                Text(
                                    text = "Log In to Your Account",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Get OTP Button
            Button(
                onClick = {
                    if (isChecking) return@Button
                    val mobileValid = InputValidationUtil.isValidIndianMobile(mobileNumber)
                    if (!mobileValid) {
                        errorMessage = "Please enter a valid 10-digit Indian mobile number."
                        alreadyRegistered = false
                        return@Button
                    }
                    errorMessage = null
                    alreadyRegistered = false
                    isChecking = true

                    coroutineScope.launch {
                        try {
                            val existing = SupabaseClient.getDriverByMobile(mobileNumber.trim())
                            if (existing != null) {
                                isChecking = false
                                alreadyRegistered = true
                                errorMessage = "Account already exists. Please log in."
                                return@launch
                            }
                            isChecking = false
                            val generatedOtp = (100000 + Random.nextInt(900000)).toString()
                            onNavigateToOtp(false, mobileNumber.trim(), "", generatedOtp)
                        } catch (e: Exception) {
                            isChecking = false
                            val generatedOtp = (100000 + Random.nextInt(900000)).toString()
                            onNavigateToOtp(false, mobileNumber.trim(), "", generatedOtp)
                        }
                    }
                },
                enabled = !isChecking,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GlipNavyPrimary,
                    disabledContainerColor = Color(0xFF94A3B8)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Checking Number...",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                } else {
                    Text(
                        text = "Get OTP",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Existing Account Login Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have a driver account? ",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B))
                )
                Text(
                    text = "Login",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0284C7)
                    ),
                    modifier = Modifier.clickable { onNavigateToLogin() }
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}
