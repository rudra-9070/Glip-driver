package com.glip.driver.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glip.driver.domain.model.DriverRoute
import com.glip.driver.domain.model.PassengerRequest
import com.glip.driver.ui.theme.GlipAccentGreen
import com.glip.driver.ui.theme.GlipAccentTeal
import com.glip.driver.ui.theme.GlipNavyCard
import com.glip.driver.ui.theme.GlipNavyDark
import com.glip.driver.ui.theme.GlipNavyPrimary
import com.glip.driver.ui.theme.GlipNavySurface

/**
 * Interactive Visual Corridor Map Preview.
 * Renders the driver's active route corridor, sequential stops, departure time,
 * and overlaid simulated passenger pickup points without requiring external Google Maps API keys or intrusive permissions.
 * Includes a full fallback stop-by-stop itinerary list.
 */
@Composable
fun RouteCorridorMapPreview(
    route: DriverRoute,
    requests: List<PassengerRequest> = emptyList(),
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GlipNavyDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Corridor",
                        tint = GlipAccentTeal,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LIVE ROUTE CORRIDOR (DEMO)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = GlipAccentTeal,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = GlipNavyCard
                ) {
                    Text(
                        text = route.direction.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFFE2E8F0),
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Visual corridor vector timeline
            CorridorTimeline(route = route)

            Spacer(modifier = Modifier.height(14.dp))

            // Simulated passenger pins overlay status
            if (requests.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = GlipNavySurface
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(GlipAccentGreen)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${requests.size} sample passenger request(s) projected along this corridor",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFCBD5E1),
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CorridorTimeline(route: DriverRoute) {
    val stops = route.fullStopSequence

    Column(modifier = Modifier.fillMaxWidth()) {
        stops.forEachIndexed { index, stopName ->
            val isFirst = index == 0
            val isLast = index == stops.lastIndex
            val isMid = !isFirst && !isLast

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Pin / Dot
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isFirst -> GlipAccentGreen
                                isLast -> GlipAccentTeal
                                else -> Color(0xFF94A3B8)
                            }
                        )
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Stop Name and Label
                Column {
                    Text(
                        text = stopName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White,
                            fontWeight = if (isFirst || isLast) FontWeight.Bold else FontWeight.Normal,
                            fontSize = if (isFirst || isLast) 14.sp else 13.sp
                        )
                    )
                    Text(
                        text = when {
                            isFirst -> "Origin • Departs ${route.departureTimeDisplay}"
                            isLast -> "Final Destination"
                            else -> "Corridor Transit Stop #$index"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    )
                }
            }

            // Connector line
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .width(2.dp)
                        .height(18.dp)
                        .background(Color(0xFF334155))
                )
            }
        }
    }
}

/**
 * Fallback Route Stop List if map rendering is toggled or unavailable.
 */
@Composable
fun RouteStopFallbackList(route: DriverRoute, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = GlipNavyPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Scheduled Stops Sequence",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            route.fullStopSequence.forEachIndexed { i, stop ->
                Text(
                    text = "${i + 1}. $stop",
                    style = MaterialTheme.typography.bodyMedium.copy(color = GlipNavyPrimary),
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}
