package com.glip.driver.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.res.painterResource
import com.glip.driver.R
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glip.driver.ui.components.GlipDemoBanner
import com.glip.driver.ui.components.GlipPrimaryButton
import com.glip.driver.ui.components.GlipSecondaryButton
import com.glip.driver.ui.theme.GlipAccentGreen
import com.glip.driver.ui.theme.GlipAccentTeal
import com.glip.driver.ui.theme.GlipNavyDark
import com.glip.driver.ui.theme.GlipNavyPrimary

@Composable
fun WelcomeScreen(
    onNavigateToRegister: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToDiagnostics: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            GlipDemoBanner()
            Spacer(modifier = Modifier.height(40.dp))

            // App Identity Brand Graphic
            Image(
                painter = painterResource(id = R.drawable.ic_glip_logo),
                contentDescription = "Haze Logo",
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(22.dp))
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Haze Driver",
                style = MaterialTheme.typography.headlineLarge.copy(
                    color = GlipNavyPrimary,
                    fontWeight = FontWeight.ExtraBold
                )
            )

            Text(
                text = "Route-Based Seat Sharing Partner",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = GlipAccentTeal,
                    fontWeight = FontWeight.SemiBold
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Already travelling on a route? Monetize your unoccupied seats with compatible local co-passengers. Not a personal cab booking app.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                ),
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(30.dp))

            // Value props
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ValuePropItem(
                        icon = Icons.Default.DirectionsCar,
                        title = "Your Route, Your Schedule",
                        desc = "You drive where you need to go; co-passengers join your corridor."
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ValuePropItem(
                        icon = Icons.Default.Lock,
                        title = "Full Driver Control",
                        desc = "Accept or decline corridor matches based on your available capacity."
                    )
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            GlipPrimaryButton(
                text = "Register as Driver",
                onClick = onNavigateToRegister
            )
            Spacer(modifier = Modifier.height(12.dp))
            GlipSecondaryButton(
                text = "I Already Have an Account",
                onClick = onNavigateToLogin
            )
            Spacer(modifier = Modifier.height(10.dp))
            TextButton(
                onClick = onNavigateToDiagnostics,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(Icons.Default.BugReport, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Diagnostics & Error Logs", color = Color(0xFF64748B), fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ValuePropItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = GlipAccentGreen,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = GlipNavyPrimary
                )
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF64748B),
                    fontSize = 12.sp
                )
            )
        }
    }
}
