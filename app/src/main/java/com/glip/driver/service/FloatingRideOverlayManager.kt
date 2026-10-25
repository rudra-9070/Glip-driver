package com.glip.driver.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.glip.driver.MainActivity
import com.glip.driver.domain.model.PassengerRequest

/**
 * Floating Rapido/Uber-style Overlay System for Haze Driver.
 * 1. Persistent Draggable Floating Pill ("HAZE Online") showing active ONLINE status outside the app.
 * 2. Instant Expandable Ride Request Card directly over any app with Accept/Decline actions.
 * Strictly NO car symbols or emojis used.
 */
object FloatingRideOverlayManager {

    private var cardOverlayView: View? = null
    private var bubbleView: View? = null
    private var windowManager: WindowManager? = null

    fun isShowing(): Boolean = cardOverlayView != null || bubbleView != null
    fun isRideCardShowing(): Boolean = cardOverlayView != null

    fun canDrawOverlays(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
    }

    /**
     * Shows a draggable Floating Pill widget displaying:
     *   HAZE
     *   🟢 Online
     * when driver is ONLINE and outside the app (e.g. browsing WhatsApp, Home screen).
     * Strictly NO car symbols used. Clicking opens Haze Driver app.
     */
    @SuppressLint("ClickableViewAccessibility")
    fun showOnlineBubble(context: Context) {
        if (!canDrawOverlays(context)) {
            return
        }
        // Do not show bubble if app is in foreground or ride card is already showing
        if (MainActivity.isAppInForeground || cardOverlayView != null || bubbleView != null) {
            return
        }

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

            val pillWidth = dp(94)
            val pillHeight = dp(46)
            val screenWidth = context.resources.displayMetrics.widthPixels

            val params = WindowManager.LayoutParams(
                pillWidth,
                pillHeight,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = screenWidth - pillWidth - dp(12) // Initial right edge
                y = dp(160) // Ergonomic thumb zone
            }

            // Rounded Pill Container
            val rootPill = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(8), dp(4), dp(8), dp(4))
                layoutParams = FrameLayout.LayoutParams(pillWidth, pillHeight)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(23).toFloat()
                    setColor(Color.parseColor("#0F172A")) // Deep Slate Navy
                    setStroke(dp(2), Color.parseColor("#10B981")) // Emerald Green Border
                }
                elevation = dp(10).toFloat()
            }

            // Line 1: HAZE (Brand name in bold uppercase)
            val brandText = TextView(context).apply {
                text = "HAZE"
                textSize = 12f
                setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                letterSpacing = 0.12f
            }
            rootPill.addView(brandText)

            // Line 2: 🟢 Online (Status row with glowing green dot and text)
            val statusRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding(0, dp(1), 0, 0)
            }

            val greenDot = View(context).apply {
                val dotSize = dp(6)
                layoutParams = LinearLayout.LayoutParams(dotSize, dotSize).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    setMargins(0, 0, dp(4), 0)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#10B981")) // Vibrant Emerald Green
                }
            }

            val statusText = TextView(context).apply {
                text = "Online"
                textSize = 10f
                setTextColor(Color.parseColor("#34D399")) // Bright Mint Green
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER_VERTICAL
            }

            statusRow.addView(greenDot)
            statusRow.addView(statusText)
            rootPill.addView(statusRow)

            // Draggable Touch & Click Listener
            var initialX = 0
            var initialY = 0
            var initialTouchX = 0f
            var initialTouchY = 0f

            rootPill.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        try {
                            wm.updateViewLayout(rootPill, params)
                        } catch (_: Exception) {}
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        val diffX = Math.abs(event.rawX - initialTouchX)
                        val diffY = Math.abs(event.rawY - initialTouchY)
                        if (diffX < dp(8) && diffY < dp(8)) {
                            // Click detected -> Instantly bring Haze Driver to foreground
                            try {
                                val intent = Intent(context, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }
                        true
                    }
                    else -> false
                }
            }

            wm.addView(rootPill, params)
            bubbleView = rootPill
        } catch (_: Exception) {
            bubbleView = null
        }
    }

    fun dismissBubble() {
        try {
            bubbleView?.let { view ->
                windowManager?.removeViewImmediate(view)
            }
        } catch (_: Exception) {}
        bubbleView = null
    }

    /**
     * Pops up the interactive Ride Request Card directly over any app
     * when a new passenger ride request arrives.
     * Returns true if overlay was successfully shown via WindowManager.
     */
    fun showOverlay(
        context: Context,
        request: PassengerRequest,
        onAccept: () -> Unit,
        onDecline: () -> Unit
    ): Boolean {
        if (!canDrawOverlays(context)) {
            return false
        }

        // Hide bubble while full ride card is showing
        dismissBubble()
        dismissOverlay()

        try {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return false
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
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
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
                setPadding(dp(18), dp(16), dp(18), dp(16))
                background = GradientDrawable().apply {
                    setColor(Color.WHITE)
                    cornerRadius = dp(20).toFloat()
                    setStroke(dp(1), Color.parseColor("#E2E8F0"))
                }
                elevation = dp(14).toFloat()
            }

            // Header: "⚡ NEW RIDE REQUEST" + Fare "₹{fare}" (No car symbol)
            val header = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val titleTv = TextView(context).apply {
                text = "⚡ NEW RIDE REQUEST"
                textSize = 13f
                setTextColor(Color.parseColor("#D97706")) // Amber-700
                typeface = Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val fareTv = TextView(context).apply {
                text = "₹${request.illustrativeFare}"
                textSize = 22f
                setTextColor(Color.parseColor("#0F172A"))
                typeface = Typeface.DEFAULT_BOLD
            }
            header.addView(titleTv)
            header.addView(fareTv)
            card.addView(header)

            // Passenger Name and Seats
            val passTv = TextView(context).apply {
                text = "${request.passengerAlias}  •  ${request.requestedSeats} Seat${if (request.requestedSeats > 1) "s" else ""}"
                textSize = 14f
                setTextColor(Color.parseColor("#334155"))
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, dp(6), 0, dp(6))
            }
            card.addView(passTv)

            // Pickup Row: 🟢 Pickup Name
            val pickupTv = TextView(context).apply {
                text = "🟢 ${request.pickupName.ifBlank { "Pickup Location" }}"
                textSize = 13f
                setTextColor(Color.parseColor("#0F172A"))
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                setPadding(0, dp(2), 0, dp(3))
            }
            card.addView(pickupTv)

            // Drop Row: 🔴 Drop Name
            val dropTv = TextView(context).apply {
                text = "🔴 ${request.dropoffName.ifBlank { "Drop Location" }}"
                textSize = 13f
                setTextColor(Color.parseColor("#64748B"))
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                setPadding(0, dp(2), 0, dp(10))
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
                layoutParams = LinearLayout.LayoutParams(dp(110), dp(44)).apply {
                    marginEnd = dp(10)
                }
                setOnClickListener {
                    dismissOverlay()
                    onDecline()
                    if (!MainActivity.isAppInForeground) {
                        showOnlineBubble(context)
                    }
                }
            }

            val acceptBtn = Button(context).apply {
                text = "✓ Accept Ride"
                textSize = 14f
                setTextColor(Color.parseColor("#0F172A"))
                typeface = Typeface.DEFAULT_BOLD
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#F5B838")) // Gold/Amber
                    cornerRadius = dp(12).toFloat()
                }
                layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f)
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
            cardOverlayView = root
            return true
        } catch (_: Exception) {
            cardOverlayView = null
            return false
        }
    }

    fun dismissOverlay() {
        try {
            cardOverlayView?.let { view ->
                windowManager?.removeViewImmediate(view)
            }
        } catch (_: Exception) {}
        cardOverlayView = null
    }

    fun dismissAll() {
        dismissOverlay()
        dismissBubble()
        windowManager = null
    }
}
