package com.glip.driver.service

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.glip.driver.domain.model.PassengerRequest

/**
 * Floating Rapido/Uber-style Overlay Card that pops up directly over other apps
 * when a new passenger ride request arrives and the driver is outside the app.
 */
object FloatingRideOverlayManager {

    private var overlayView: View? = null
    private var windowManager: WindowManager? = null

    fun isShowing(): Boolean = overlayView != null

    fun showOverlay(
        context: Context,
        request: PassengerRequest,
        onAccept: () -> Unit,
        onDecline: () -> Unit
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            return
        }

        dismissOverlay()

        try {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
            windowManager = wm

            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val density = context.resources.displayMetrics.density
            fun dp(value: Int): Int = (value * density).toInt()

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = dp(24)
            }

            // Root Container with Margins
            val root = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(0), dp(16), dp(0))
            }

            // Card Container
            val card = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(14), dp(16), dp(14))
                background = GradientDrawable().apply {
                    setColor(Color.WHITE)
                    cornerRadius = dp(18).toFloat()
                    setStroke(dp(1), Color.parseColor("#E2E8F0"))
                }
                elevation = dp(10).toFloat()
            }

            // Header: "🚕 NEW RIDE REQUEST" + Fare "₹{fare}"
            val header = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val titleTv = TextView(context).apply {
                text = "🚕 NEW RIDE REQUEST"
                textSize = 13f
                setTextColor(Color.parseColor("#D97706")) // Amber-700
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val fareTv = TextView(context).apply {
                text = "₹${request.illustrativeFare}"
                textSize = 20f
                setTextColor(Color.parseColor("#0F172A"))
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            header.addView(titleTv)
            header.addView(fareTv)
            card.addView(header)

            // Passenger Name and Seats
            val passTv = TextView(context).apply {
                text = "${request.passengerAlias}  •  ${request.requestedSeats} Seat${if (request.requestedSeats > 1) "s" else ""}"
                textSize = 14f
                setTextColor(Color.parseColor("#334155"))
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setPadding(0, dp(6), 0, dp(4))
            }
            card.addView(passTv)

            // Pickup Row: 🟢 Pickup Name
            val pickupTv = TextView(context).apply {
                text = "🟢 ${request.pickupName.ifBlank { "Pickup Location" }}"
                textSize = 12f
                setTextColor(Color.parseColor("#0F172A"))
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                setPadding(0, dp(2), 0, dp(2))
            }
            card.addView(pickupTv)

            // Drop Row: 🔴 Drop Name
            val dropTv = TextView(context).apply {
                text = "🔴 ${request.dropoffName.ifBlank { "Drop Location" }}"
                textSize = 12f
                setTextColor(Color.parseColor("#64748B"))
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                setPadding(0, dp(2), 0, dp(8))
            }
            card.addView(dropTv)

            // Action Buttons: Decline & Accept
            val btnRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val declineBtn = Button(context).apply {
                text = "✕ Decline"
                textSize = 13f
                setTextColor(Color.parseColor("#64748B"))
                background = GradientDrawable().apply {
                    setColor(Color.WHITE)
                    cornerRadius = dp(12).toFloat()
                    setStroke(dp(1), Color.parseColor("#CBD5E1"))
                }
                layoutParams = LinearLayout.LayoutParams(dp(110), dp(42)).apply {
                    marginEnd = dp(10)
                }
                setOnClickListener {
                    dismissOverlay()
                    onDecline()
                }
            }

            val acceptBtn = Button(context).apply {
                text = "✓ Accept Ride"
                textSize = 14f
                setTextColor(Color.parseColor("#0F172A"))
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#F5B838")) // Gold/Amber
                    cornerRadius = dp(12).toFloat()
                }
                layoutParams = LinearLayout.LayoutParams(0, dp(42), 1f)
                setOnClickListener {
                    dismissOverlay()
                    onAccept()
                }
            }

            btnRow.addView(declineBtn)
            btnRow.addView(acceptBtn)
            card.addView(btnRow)

            root.addView(card)

            wm.addView(root, params)
            overlayView = root
        } catch (e: Exception) {
            overlayView = null
        }
    }

    fun dismissOverlay() {
        try {
            overlayView?.let { view ->
                windowManager?.removeViewImmediate(view)
            }
        } catch (_: Exception) {}
        overlayView = null
        windowManager = null
    }
}
