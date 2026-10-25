package com.glip.driver.ui.incoming

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glip.driver.GlipDriverApplication
import com.glip.driver.MainActivity
import com.glip.driver.domain.model.PassengerRequest
import kotlinx.coroutines.launch

/**
 * Translucent Full-Screen Overlay Activity for Incoming Ride Requests.
 *
 * Provides a 100% reliable Rapido/Uber-style floating ride alert outside the app
 * across all Android OEMs (Xiaomi, Samsung, Vivo, Oppo, Realme) without requiring
 * drivers to manually navigate confusing system settings ("Display over other apps").
 */
class IncomingRideActivity : ComponentActivity() {

    companion object {
        const val EXTRA_REQUEST_ID = "extra_request_id"
        const val EXTRA_PASSENGER_NAME = "extra_passenger_name"
        const val EXTRA_FARE = "extra_fare"
        const val EXTRA_PICKUP = "extra_pickup"
        const val EXTRA_DROPOFF = "extra_dropoff"
        const val EXTRA_SEATS = "extra_seats"

        fun start(
            context: Context,
            request: PassengerRequest
        ) {
            val intent = Intent(context, IncomingRideActivity::class.java).apply {
                putExtra(EXTRA_REQUEST_ID, request.id)
                putExtra(EXTRA_PASSENGER_NAME, request.passengerAlias)
                putExtra(EXTRA_FARE, request.illustrativeFare)
                putExtra(EXTRA_PICKUP, request.pickupName)
                putExtra(EXTRA_DROPOFF, request.dropoffName)
                putExtra(EXTRA_SEATS, request.requestedSeats)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Wake screen and show over keyguard/lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val requestId = intent.getStringExtra(EXTRA_REQUEST_ID) ?: ""
        val passengerName = intent.getStringExtra(EXTRA_PASSENGER_NAME) ?: "Passenger"
        val fare = intent.getIntExtra(EXTRA_FARE, 0)
        val pickup = intent.getStringExtra(EXTRA_PICKUP) ?: "Pickup Location"
        val dropoff = intent.getStringExtra(EXTRA_DROPOFF) ?: "Drop Location"
        val seats = intent.getIntExtra(EXTRA_SEATS, 1)

        setContent {
            val incomingRequests by GlipDriverApplication.repository.incomingRequestsFlow.collectAsState()
            val coroutineScope = rememberCoroutineScope()

            // If request was accepted or cancelled elsewhere, auto-dismiss
            LaunchedEffect(incomingRequests) {
                if (incomingRequests.none { it.id == requestId }) {
                    finish()
                }
            }

            IncomingRideOverlayDialog(
                fare = fare,
                passengerName = passengerName,
                seats = seats,
                pickup = pickup,
                dropoff = dropoff,
                onAccept = {
                    coroutineScope.launch {
                        GlipDriverApplication.repository.acceptRequest(requestId)
                        val mainIntent = Intent(this@IncomingRideActivity, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                        startActivity(mainIntent)
                        finish()
                    }
                },
                onDecline = {
                    coroutineScope.launch {
                        GlipDriverApplication.repository.declineRequest(requestId)
                        finish()
                    }
                }
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}

@Composable
private fun IncomingRideOverlayDialog(
    fare: Int,
    passengerName: String,
    seats: Int,
    pickup: String,
    dropoff: String,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    // Dimmed background: click on outside declines or ignores
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.52f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Non-modal tap outside
            },
        contentAlignment = Alignment.TopCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 28.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header: Badge + Fare (No car icon)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFEF3C7), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "⚡ NEW RIDE REQUEST",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD97706)
                        )
                    }

                    Text(
                        text = "₹$fare",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Passenger details
                Text(
                    text = "$passengerName  •  $seats Seat${if (seats > 1) "s" else ""}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF334155)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Route: Pickup
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(0xFF10B981), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = pickup.ifBlank { "Pickup Location" },
                        fontSize = 13.sp,
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Route: Dropoff
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(0xFFEF4444), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = dropoff.ifBlank { "Drop Location" },
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDecline,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "✕ Decline",
                            color = Color(0xFF64748B),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = onAccept,
                        modifier = Modifier
                            .weight(1.4f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF5B838)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "✓ Accept Ride",
                            color = Color(0xFF0F172A),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
