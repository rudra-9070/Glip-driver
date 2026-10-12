package com.glip.driver.ui.crash

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glip.driver.MainActivity
import com.glip.driver.data.local.LocalDemoSessionManager
import com.glip.driver.ui.theme.GlipDriverTheme
import com.glip.driver.util.CrashManager

class CrashActivity : ComponentActivity() {

    companion object {
        const val EXTRA_ERROR_TYPE = "extra_error_type"
        const val EXTRA_ERROR_MESSAGE = "extra_error_message"
        const val EXTRA_STACK_TRACE = "extra_stack_trace"
        const val EXTRA_TIMESTAMP = "extra_timestamp"
        const val EXTRA_DEVICE_INFO = "extra_device_info"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val errorType = intent.getStringExtra(EXTRA_ERROR_TYPE)
            ?: CrashManager.getLastError(this)?.errorType
            ?: "Unexpected Application Error"

        val errorMessage = intent.getStringExtra(EXTRA_ERROR_MESSAGE)
            ?: CrashManager.getLastError(this)?.errorMessage
            ?: "An unexpected exception occurred."

        val stackTrace = intent.getStringExtra(EXTRA_STACK_TRACE)
            ?: CrashManager.getLastError(this)?.stackTrace
            ?: "No stack trace available."

        val timestamp = intent.getStringExtra(EXTRA_TIMESTAMP)
            ?: CrashManager.getLastError(this)?.timestamp
            ?: ""

        val deviceInfo = intent.getStringExtra(EXTRA_DEVICE_INFO)
            ?: CrashManager.getLastError(this)?.deviceInfo
            ?: ""

        setContent {
            GlipDriverTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F172A)
                ) {
                    CrashScreenContent(
                        errorType = errorType,
                        errorMessage = errorMessage,
                        stackTrace = stackTrace,
                        timestamp = timestamp,
                        deviceInfo = deviceInfo,
                        onRestart = { restartApp() },
                        onResetAndRestart = {
                            LocalDemoSessionManager(applicationContext).clearSession()
                            CrashManager.clearError(applicationContext)
                            restartApp()
                        }
                    )
                }
            }
        }
    }

    private fun restartApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
        finish()
    }
}

@Composable
fun CrashScreenContent(
    errorType: String,
    errorMessage: String,
    stackTrace: String,
    timestamp: String,
    deviceInfo: String,
    onRestart: () -> Unit,
    onResetAndRestart: () -> Unit
) {
    val context = LocalContext.current
    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()

    val fullReportText = buildString {
        append("=== GLIP DRIVER ERROR REPORT ===\n")
        append("Time: ").append(timestamp).append("\n")
        append("Error: ").append(errorType).append("\n")
        append("Message: ").append(errorMessage).append("\n\n")
        append("--- DEVICE INFO ---\n")
        append(deviceInfo).append("\n\n")
        append("--- STACK TRACE ---\n")
        append(stackTrace)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(verticalScroll)
    ) {
        // Red Alert Banner
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEF4444)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "App Crash Diagnostic",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Glip Driver caught an exception instead of closing.",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Exception summary Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "ERROR TYPE",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = errorType,
                    color = Color(0xFFF87171),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "REASON",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = errorMessage,
                    color = Color.White,
                    fontSize = 14.sp
                )

                if (timestamp.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Time: $timestamp",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Stack Trace Box
        Text(
            text = "Stack Trace Details",
            color = Color(0xFFE2E8F0),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF020617)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .padding(12.dp)
                    .horizontalScroll(horizontalScroll)
            ) {
                Text(
                    text = stackTrace,
                    color = Color(0xFF38BDF8),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Primary Action Buttons
        Button(
            onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Glip Driver Error Report", fullReportText)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Error details copied to clipboard!", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Copy Error Details", color = Color.Black, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Glip Driver Crash Report: $errorType")
                    putExtra(Intent.EXTRA_TEXT, fullReportText)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Error Report via"))
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Share Error Details", color = Color.White, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = onRestart,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Restart Glip Driver", color = Color.White, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onResetAndRestart,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Clear Session & Restart", color = Color(0xFFEF4444), fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
