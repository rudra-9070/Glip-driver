package com.glip.driver.ui.home

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glip.driver.data.repository.DemoDriverRepositoryImpl
import com.glip.driver.map.GlipOsmMapView
import com.glip.driver.ui.theme.GlipAccentTeal
import com.glip.driver.ui.theme.GlipNavyDark
import com.glip.driver.ui.theme.GlipNavyPrimary
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    repository: DemoDriverRepositoryImpl,
    onNavigateToProfile: () -> Unit = {},
    onNavigateToActivity: () -> Unit = {},
    onNavigateToRequests: () -> Unit = {},
    onEditVehicle: () -> Unit = {},
    onEditRoute: () -> Unit = {},
    onViewAllRequests: () -> Unit = {},
    onNavigateToDiagnostics: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val driverRoute by repository.routeFlow.collectAsState(initial = null)
    val vehicle by repository.vehicleFlow.collectAsState(initial = null)
    val isAvailable by repository.isAvailableFlow.collectAsState(initial = true)
    val requests by repository.requestsFlow.collectAsState(initial = emptyList())
    val profile by repository.profileFlow.collectAsState(initial = null)

    val origin = driverRoute?.originName ?: "Motipur"
    val destination = driverRoute?.destinationName ?: "Baruraj"

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = GlipNavyPrimary,
                drawerContentColor = Color.White,
                modifier = Modifier.width(300.dp)
            ) {
                // Drawer Profile Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GlipNavyDark)
                        .padding(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Driver Avatar",
                            tint = GlipAccentTeal,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = profile?.fullName ?: "Driver",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = profile?.maskedMobile ?: "Status: Active",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF94A3B8)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 1. Home
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Home",
                            tint = GlipAccentTeal
                        )
                    },
                    label = {
                        Text(
                            text = "Home",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    selected = true,
                    onClick = { coroutineScope.launch { drawerState.close() } },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = GlipNavyDark,
                        unselectedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                // 2. Profile
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Profile",
                            tint = Color(0xFFCBD5E1)
                        )
                    },
                    label = {
                        Text(
                            text = "Profile",
                            color = Color(0xFFE2E8F0)
                        )
                    },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        onNavigateToProfile()
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                // 3. Activity
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.FormatListBulleted,
                            contentDescription = "Activity",
                            tint = Color(0xFFCBD5E1)
                        )
                    },
                    label = {
                        Text(
                            text = "Activity",
                            color = Color(0xFFE2E8F0)
                        )
                    },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        onNavigateToActivity()
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                // 4. Requests
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = "Requests",
                            tint = Color(0xFFCBD5E1)
                        )
                    },
                    label = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Requests",
                                color = Color(0xFFE2E8F0)
                            )
                            if (requests.isNotEmpty()) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF16A34A)
                                ) {
                                    Text(
                                        text = "${requests.size}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        onNavigateToRequests()
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                // Diagnostics item at bottom of drawer
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = "Diagnostics",
                            tint = Color(0xFF94A3B8)
                        )
                    },
                    label = {
                        Text(
                            text = "Diagnostics & Logs",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        onNavigateToDiagnostics()
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
        ) {
            // Top Bar: Hamburger Menu | Online/Offline Toggle | Planned Route Setup
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Hamburger Menu Button
                Surface(
                    onClick = {
                        coroutineScope.launch { drawerState.open() }
                    },
                    shape = CircleShape,
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 2.dp
                ) {
                    Box(
                        modifier = Modifier.size(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu Bar",
                            tint = GlipNavyPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // 2. Online / Offline Availability Toggle
                Surface(
                    onClick = {
                        coroutineScope.launch {
                            repository.setAvailable(!isAvailable)
                        }
                    },
                    shape = RoundedCornerShape(22.dp),
                    color = if (isAvailable) Color(0xFF10B981) else Color(0xFF334155),
                    border = BorderStroke(
                        1.5.dp,
                        if (isAvailable) Color(0xFF059669) else Color(0xFF1E293B)
                    ),
                    shadowElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(if (isAvailable) Color.White else Color(0xFF94A3B8))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isAvailable) "ONLINE" else "OFFLINE",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // 3. Planned Route Setup Button (New AltRoute Icon)
                Surface(
                    onClick = onEditRoute,
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFF0284C7)),
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AltRoute,
                            contentDescription = "Planned Route Setup",
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Route Setup",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0284C7)
                        )
                    }
                }
            }

            // Route Corridor Bar: Motipur -> Baruraj | Change
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 3.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Origin dot
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = origin,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = GlipNavyPrimary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "to",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))

                        // Destination dot
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = destination,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = GlipNavyPrimary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }

                    // Change Action
                    Text(
                        text = "Change",
                        color = Color(0xFF0284C7),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clickable { onEditRoute() }
                            .padding(start = 8.dp)
                    )
                }
            }

            // Live Native OpenStreetMap View Container (Ola / Uber / Rapido Style)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    GlipOsmMapView(
                        route = driverRoute,
                        requests = requests,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Ride-Hailing Driver HUD Bar (Seats, Vehicle & Incoming Requests)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Seat capacity status
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = "Vehicle",
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            val availSeats = vehicle?.availableSeats ?: 2
                            val occupied = vehicle?.occupiedSeats ?: 1
                            Text(
                                text = "$availSeats Seat(s) Available",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "$occupied occupied • ${vehicle?.displayName ?: "Auto"}",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    // Right: Requests Action Button
                    Surface(
                        onClick = onViewAllRequests,
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = "Requests",
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Requests (${requests.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF16A34A)
                            )
                        }
                    }
                }
            }

            // Advertisement Space Box (Height increased at bottom as requested)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Advertisement Space",
                        color = Color(0xFF94A3B8),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
