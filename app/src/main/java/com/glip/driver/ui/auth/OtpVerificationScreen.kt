package com.glip.driver.ui.auth

import android.widget.Toast
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glip.driver.domain.model.DriverProfile
import com.glip.driver.domain.model.DriverStatus
import com.glip.driver.domain.repository.DriverRepository
import com.glip.driver.ui.components.GlipPrimaryButton
import com.glip.driver.ui.theme.GlipAccentGreen
import com.glip.driver.ui.theme.GlipErrorRed
import com.glip.driver.ui.theme.GlipNavyPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * 6-Digit OTP Verification Screen with:
 * - 6 dedicated digit boxes with auto-focus advance and backspace handling
 * - 6-second demo OTP pop-up banner/dialog with "Copy OTP" and "Auto-Fill" options
 * - Support for both New Registration and Existing Account Login (Supabase sync ready)
 */
@Composable
fun OtpVerificationScreen(
    repository: DriverRepository,
    isLogin: Boolean,
    mobileNumber: String,
    fullName: String,
    expectedOtp: String,
    onVerificationSuccess: (isNewUser: Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var currentOtpTarget by remember { mutableStateOf(expectedOtp) }
    val otpDigits = remember { mutableStateListOf("", "", "", "", "", "") }
    val focusRequesters = remember { List(6) { FocusRequester() } }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var attemptsLeft by remember { mutableIntStateOf(3) }
    var showOtpPopup by remember { mutableStateOf(false) }
    var resendCountdown by remember { mutableIntStateOf(30) }

    // Auto-focus on first digit box on screen open
    LaunchedEffect(Unit) {
        delay(200)
        try {
            focusRequesters[0].requestFocus()
        } catch (_: Exception) {}
    }

    // Timer: Countdown for resend button
    LaunchedEffect(resendCountdown) {
        if (resendCountdown > 0) {
            delay(1000)
            resendCountdown -= 1
        }
    }

    // Feature requirement: Wait 6 seconds, then show OTP pop-up on screen with Copy/Auto-fill
    LaunchedEffect(Unit) {
        delay(6000)
        showOtpPopup = true
    }

    // Helper: auto fill digits
    fun fillOtp(code: String) {
        if (code.length == 6) {
            for (i in 0 until 6) {
                otpDigits[i] = code[i].toString()
            }
            errorMessage = null
            keyboardController?.hide()
        }
    }

    // OTP Pop-up Dialog
    if (showOtpPopup) {
        AlertDialog(
            onDismissRequest = { showOtpPopup = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDCFCE7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MarkEmailRead,
                            contentDescription = null,
                            tint = GlipAccentGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Glip OTP Received",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = GlipNavyPrimary
                        )
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Demo Verification Code for +91 $mobileNumber:",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Prominent OTP Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        currentOtpTarget.forEach { digit ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFEFF6FF),
                                border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = digit.toString(),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF0284C7)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Tap 'Copy OTP' to copy or 'Auto-Fill' to enter automatically.",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                GlipPrimaryButton(
                    text = "Auto-Fill OTP",
                    onClick = {
                        fillOtp(currentOtpTarget)
                        showOtpPopup = false
                        Toast.makeText(context, "OTP Auto-Filled!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(currentOtpTarget))
                        Toast.makeText(context, "OTP Copied to Clipboard: $currentOtpTarget", Toast.LENGTH_SHORT).show()
                        showOtpPopup = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Copy OTP",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0284C7)
                    )
                }
            },
            shape = RoundedCornerShape(18.dp),
            containerColor = Color.White
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .verticalScroll(scrollState)
    ) {
        // Back Button
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = GlipNavyPrimary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Screen Header
        Text(
            text = "OTP Verification",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = GlipNavyPrimary
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "We have sent a 6-digit verification code to",
            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF64748B))
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "+91 $mobileNumber",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = GlipNavyPrimary
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Edit",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0284C7)
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onBack() }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 6 Separate OTP Digit Input Boxes
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (index in 0 until 6) {
                val isFocused = otpDigits.getOrNull(index)?.isNotEmpty() == true
                val hasError = errorMessage != null

                Box(
                    modifier = Modifier
                        .size(width = 46.dp, height = 56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isFocused) Color(0xFFF8FAFC) else Color(0xFFF1F5F9))
                        .border(
                            width = if (isFocused) 2.dp else 1.dp,
                            color = when {
                                hasError -> GlipErrorRed
                                isFocused -> Color(0xFF0284C7)
                                else -> Color(0xFFCBD5E1)
                            },
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    BasicTextField(
                        value = otpDigits[index],
                        onValueChange = { newVal ->
                            if (newVal.length <= 1 && newVal.all { it.isDigit() }) {
                                otpDigits[index] = newVal
                                errorMessage = null
                                if (newVal.isNotEmpty()) {
                                    if (index < 5) {
                                        focusRequesters[index + 1].requestFocus()
                                    } else {
                                        keyboardController?.hide()
                                    }
                                }
                            } else if (newVal.length > 1 && newVal.all { it.isDigit() }) {
                                // Handles paste of 6 digits into any box
                                val pasted = newVal.take(6)
                                fillOtp(pasted)
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = GlipNavyPrimary,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 10.dp)
                            .focusRequester(focusRequesters[index])
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.key == Key.Backspace && otpDigits[index].isEmpty() && index > 0) {
                                    focusRequesters[index - 1].requestFocus()
                                    otpDigits[index - 1] = ""
                                    true
                                } else {
                                    false
                                }
                            }
                    )
                }
            }
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                color = Color(0xFFFEE2E2),
                shape = RoundedCornerShape(8.dp),
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

        // Verify Button
        GlipPrimaryButton(
            text = "Verify OTP & Continue",
            onClick = {
                val enteredCode = otpDigits.joinToString("")
                if (enteredCode.length < 6) {
                    errorMessage = "Please enter all 6 digits of the OTP."
                    return@GlipPrimaryButton
                }

                if (enteredCode == currentOtpTarget) {
                    coroutineScope.launch {
                        if (isLogin) {
                            // Existing account login flow
                            val existingProfile = repository.getProfile()
                            if (existingProfile != null && existingProfile.mobileNumber == mobileNumber) {
                                onVerificationSuccess(false)
                            } else {
                                // If profile not in current demo session, restore profile with existing details
                                val restored = DriverProfile(
                                    id = "drv_${mobileNumber.takeLast(4)}",
                                    fullName = if (fullName.isNotBlank()) fullName else "Glip Driver",
                                    mobileNumber = mobileNumber,
                                    email = null,
                                    status = DriverStatus.DEMO_ACTIVE
                                )
                                repository.saveProfile(restored)
                                onVerificationSuccess(false)
                            }
                        } else {
                            // New Registration flow
                            val newProfile = DriverProfile(
                                id = "drv_${System.currentTimeMillis() % 100000}",
                                fullName = fullName.trim(),
                                mobileNumber = mobileNumber.trim(),
                                email = null,
                                status = DriverStatus.DEMO_ACTIVE
                            )
                            repository.saveProfile(newProfile)
                            onVerificationSuccess(true)
                        }
                    }
                } else {
                    attemptsLeft -= 1
                    if (attemptsLeft <= 0) {
                        errorMessage = "Too many incorrect attempts. Please request a new OTP."
                    } else {
                        errorMessage = "Incorrect OTP. $attemptsLeft attempts remaining."
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Resend OTP / Manual Demo Pop-up Trigger
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = { showOtpPopup = true }
            ) {
                Text(
                    text = "Show Demo OTP",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0284C7),
                    fontSize = 13.sp
                )
            }

            if (resendCountdown > 0) {
                Text(
                    text = "Resend OTP in ${resendCountdown}s",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
            } else {
                TextButton(
                    onClick = {
                        val newOtp = (Random.nextInt(900000) + 100000).toString()
                        currentOtpTarget = newOtp
                        resendCountdown = 30
                        attemptsLeft = 3
                        for (i in 0 until 6) otpDigits[i] = ""
                        showOtpPopup = true
                        Toast.makeText(context, "New OTP Sent!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(
                        text = "Resend OTP",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16A34A),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
