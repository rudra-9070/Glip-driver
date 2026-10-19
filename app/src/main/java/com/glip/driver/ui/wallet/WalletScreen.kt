package com.glip.driver.ui.wallet

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import com.glip.driver.ui.theme.GlipNavyPrimary

data class WalletTransaction(
    val id: String,
    val title: String,
    val subtitle: String,
    val amount: String,
    val isCredit: Boolean,
    val date: String
)

@Composable
fun WalletScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var selectedFilter by remember { mutableStateOf("All") }
    var showMyAccountScreen by remember { mutableStateOf(false) }

    // Hardware back button handling
    BackHandler(enabled = showMyAccountScreen) {
        showMyAccountScreen = false
    }

    if (showMyAccountScreen) {
        // Dedicated "My Account" Screen (MY QR & My Glip ID)
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Top App Bar with Back Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showMyAccountScreen = false }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = GlipNavyPrimary
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "My Account",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = GlipNavyPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Card: MY QR and My Glip ID
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "MY QR",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Scan to pay or receive directly",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // QR vala BOX (thode bade size me QR aaye - 240dp)
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(BorderStroke(1.5.dp, Color(0xFFE2E8F0)), RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        DriverQrCodeCanvas(
                            glipId = "glip@564879",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // My glip ID Section
                    Text(
                        text = "My glip ID",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Random code box with copy option chota sa
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "glip@564879",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                letterSpacing = 0.5.sp
                            )

                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Glip ID", "glip@564879")
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Glip ID copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Glip ID",
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Accepted on all Glip Mobility Network payments",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
        return
    }

    val sampleTransactions = remember {
        listOf(
            WalletTransaction(
                id = "tx_1",
                title = "Ride Earning - Corridor Pooling",
                subtitle = "Completed trip • Cashless",
                amount = "+₹40.00",
                isCredit = true,
                date = "Today, 11:20 AM"
            ),
            WalletTransaction(
                id = "tx_2",
                title = "Ride Earning - Shared Commute",
                subtitle = "Completed trip • 2 passengers",
                amount = "+₹80.00",
                isCredit = true,
                date = "Today, 09:15 AM"
            ),
            WalletTransaction(
                id = "tx_3",
                title = "UPI Money Transfer",
                subtitle = "Transferred to Bank Account",
                amount = "-₹100.00",
                isCredit = false,
                date = "Yesterday, 08:45 PM"
            ),
            WalletTransaction(
                id = "tx_4",
                title = "Ride Earning - Express Ride",
                subtitle = "Completed trip",
                amount = "+₹45.00",
                isCredit = true,
                date = "Yesterday, 06:10 PM"
            ),
            WalletTransaction(
                id = "tx_5",
                title = "Ride Earning - Corridor Auto",
                subtitle = "Completed trip",
                amount = "+₹55.00",
                isCredit = true,
                date = "17 Oct, 05:30 PM"
            )
        )
    }

    val displayedTransactions = remember(selectedFilter, sampleTransactions) {
        when (selectedFilter) {
            "Received" -> sampleTransactions.filter { it.isCredit }
            "Sent" -> sampleTransactions.filter { !it.isCredit }
            else -> sampleTransactions
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(scrollState)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = GlipNavyPrimary
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Wallet",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = GlipNavyPrimary
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 1. Balance Amount Box (matches media_1792373969519_d0346168.png with My account feature)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Your Wallet Balance",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "₹120.00",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF15803D)
                    )
                }

                // Right side: "My account" feature button
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier.clickable { showMyAccountScreen = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "My Account",
                            tint = GlipNavyPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "My account",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlipNavyPrimary
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. Money Transfer Section Title (Left aligned)
        Text(
            text = "Money transfer",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Money Transfer Horizontal Options Box
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column {
                // Pay UPI ID
                MoneyTransferOptionRow(
                    title = "Pay Upi Id",
                    icon = Icons.Default.AccountBalance,
                    iconBgColor = Color(0xFFEFF6FF),
                    iconTint = Color(0xFF2563EB),
                    onClick = { /* click karne kuch na ho */ }
                )

                Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                // Pay QR
                MoneyTransferOptionRow(
                    title = "Pay QR",
                    icon = Icons.Default.QrCodeScanner,
                    iconBgColor = Color(0xFFF0FDF4),
                    iconTint = Color(0xFF16A34A),
                    onClick = { /* click karne kuch na ho */ }
                )

                Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                // Pay Number
                MoneyTransferOptionRow(
                    title = "PAY Number",
                    icon = Icons.Default.Phone,
                    iconBgColor = Color(0xFFFEF3C7),
                    iconTint = Color(0xFFD97706),
                    onClick = { /* click karne kuch na ho */ }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 4. Transaction History Header with Filter
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Transaction History",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                modifier = Modifier.clickable {
                    selectedFilter = when (selectedFilter) {
                        "All" -> "Received"
                        "Received" -> "Sent"
                        else -> "All"
                    }
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter",
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (selectedFilter == "All") "Filter" else selectedFilter,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF334155)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 5. Transaction List
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column {
                displayedTransactions.forEachIndexed { index, tx ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (tx.isCredit) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (tx.isCredit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = if (tx.isCredit) Color(0xFF16A34A) else Color(0xFFDC2626),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = tx.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1E293B)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = tx.date,
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        Text(
                            text = tx.amount,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (tx.isCredit) Color(0xFF15803D) else Color(0xFFDC2626)
                        )
                    }

                    if (index < displayedTransactions.size - 1) {
                        Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun MoneyTransferOptionRow(
    title: String,
    icon: ImageVector,
    iconBgColor: Color,
    iconTint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1E293B)
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun DriverQrCodeCanvas(
    glipId: String,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val sizePx = size.minDimension
        val numModules = 25
        val moduleSize = sizePx / numModules
        val darkColor = Color(0xFF0F172A)
        val lightColor = Color.White

        fun isFinderPattern(r: Int, c: Int): Boolean {
            if (r < 7 && c < 7) return true
            if (r < 7 && c >= numModules - 7) return true
            if (r >= numModules - 7 && c < 7) return true
            return false
        }

        fun getFinderModuleColor(r: Int, c: Int): Boolean {
            val localR = if (r < 7) r else r - (numModules - 7)
            val localC = if (c < 7) c else c - (numModules - 7)
            if (localR == 0 || localR == 6 || localC == 0 || localC == 6) return true
            if (localR == 1 || localR == 5 || localC == 1 || localC == 5) return false
            return true
        }

        drawRect(lightColor, size = size)

        for (r in 0 until numModules) {
            for (c in 0 until numModules) {
                val isDark = when {
                    isFinderPattern(r, c) -> getFinderModuleColor(r, c)
                    r == 6 -> c % 2 == 0
                    c == 6 -> r % 2 == 0
                    r in 16..20 && c in 16..20 -> {
                        val ar = r - 16
                        val ac = c - 16
                        ar == 0 || ar == 4 || ac == 0 || ac == 4 || (ar == 2 && ac == 2)
                    }
                    r in 10..14 && c in 10..14 -> false
                    else -> {
                        val h = ((r * 37 + c * 19) xor glipId.hashCode()) * 1103515245 + 12345
                        ((h ushr 16) and 1) == 1
                    }
                }

                if (isDark) {
                    drawRect(
                        color = darkColor,
                        topLeft = Offset(c * moduleSize, r * moduleSize),
                        size = Size(moduleSize + 0.5f, moduleSize + 0.5f)
                    )
                }
            }
        }

        // Center Glip logo badge
        val centerSize = moduleSize * 5.2f
        drawCircle(
            color = lightColor,
            radius = centerSize / 1.7f,
            center = Offset(sizePx / 2f, sizePx / 2f)
        )
        drawCircle(
            color = Color(0xFFF59E0B),
            radius = centerSize / 2.1f,
            center = Offset(sizePx / 2f, sizePx / 2f),
            style = Stroke(width = moduleSize * 0.7f)
        )
        drawCircle(
            color = Color(0xFF0F172A),
            radius = centerSize / 2.4f,
            center = Offset(sizePx / 2f, sizePx / 2f)
        )
    }
}
