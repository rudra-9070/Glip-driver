package com.glip.driver.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glip.driver.ui.theme.GlipAccentGreen
import com.glip.driver.ui.theme.GlipAccentTeal
import com.glip.driver.ui.theme.GlipBorder
import com.glip.driver.ui.theme.GlipNavyDark
import com.glip.driver.ui.theme.GlipNavyPrimary
import com.glip.driver.ui.theme.GlipWarningAmber

@Composable
fun GlipDemoBanner(
    modifier: Modifier = Modifier,
    message: String = ""
) {
    // Hidden - Demo banner removed per user request
}

@Composable
fun GlipSafetyReminder(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Drive Safely",
                tint = Color(0xFF1D4ED8),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Driver Road Safety Reminder",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color(0xFF1E40AF),
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "Never operate or interact with this app while operating a moving vehicle. Always pull over safely to review route requests.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF1E3A8A),
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

@Composable
fun GlipPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = GlipNavyPrimary,
            contentColor = Color.White,
            disabledContainerColor = Color(0xFF94A3B8),
            disabledContentColor = Color.White
        )
    ) {
        if (leadingIcon != null) {
            Icon(imageVector = leadingIcon, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

@Composable
fun GlipSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = GlipNavyPrimary
        ),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, GlipNavyPrimary)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

@Composable
fun GlipSectionHeader(
    title: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = GlipNavyPrimary
            )
        )
        if (actionText != null && onActionClick != null) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = GlipAccentTeal,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onActionClick() }
            )
        }
    }
}
