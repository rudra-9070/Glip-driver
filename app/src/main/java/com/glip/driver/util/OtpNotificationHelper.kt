package com.glip.driver.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat

object OtpNotificationHelper {
    private const val TAG = "DriverOtpNotificationHelper"
    const val CHANNEL_ID = "glip_driver_auth_otp_channel"
    private const val CHANNEL_NAME = "Verification Codes"
    private const val NOTIFICATION_ID = 993322

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Account verification codes and login security alerts"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 200, 250)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun sendOtpNotification(context: Context, otpCode: String) {
        try {
            createNotificationChannel(context)

            // Formatted HTML text with vibrant blue bold OTP as requested:
            // "Your OTP is 858292. Enter this OTP to verify your account."
            val htmlText = HtmlCompat.fromHtml(
                "Your OTP is <font color='#1D4ED8'><b>$otpCode</b></font>. Enter this OTP to verify your account.",
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )

            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            // ONLY Text, NO Title as requested: "Notification Ka Format Me Only Text: aaye, Title na aaye"
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentText("Your OTP is $otpCode. Enter this OTP to verify your account.")
                .setStyle(NotificationCompat.BigTextStyle().bigText(htmlText))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSound(soundUri)
                .setVibrate(longArrayOf(0, 250, 200, 250))
                .setAutoCancel(true)

            val manager = NotificationManagerCompat.from(context)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            ) {
                manager.notify(NOTIFICATION_ID, builder.build())
            }
        } catch (e: Exception) {
            Log.e(TAG, "sendOtpNotification failed: ${e.message}")
        }
    }
}
