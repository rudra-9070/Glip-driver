package com.glip.driver.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.glip.driver.GlipDriverApplication
import com.glip.driver.MainActivity
import com.glip.driver.R
import com.glip.driver.data.remote.SupabaseClient
import com.glip.driver.domain.model.PassengerRequest
import com.glip.driver.location.DriverLocationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground Dispatch Service for Glip Driver.
 * Keeps driver ONLINE in the background (even when the app is closed or minimized),
 * tracks and syncs live GPS to Supabase, and displays Heads-Up alerts and Floating
 * Ride Request cards when passengers request a ride (Uber / Rapido / Ola style).
 */
class GlipDriverDispatchService : Service() {

    companion object {
        const val ACTION_START = "com.glip.driver.service.ACTION_START"
        const val ACTION_STOP = "com.glip.driver.service.ACTION_STOP"
        const val ACTION_ACCEPT_RIDE = "com.glip.driver.service.ACTION_ACCEPT_RIDE"
        const val ACTION_DECLINE_RIDE = "com.glip.driver.service.ACTION_DECLINE_RIDE"
        const val EXTRA_REQUEST_ID = "extra_request_id"

        private const val STATUS_NOTIFICATION_ID = 1001
        private const val RIDE_ALERT_NOTIFICATION_ID = 2002

        private const val CHANNEL_STATUS_ID = "glip_driver_status_channel"
        private const val CHANNEL_ALERT_ID = "glip_driver_ride_alert_channel"

        var isRunning: Boolean = false
            private set

        fun start(context: Context) {
            if (!DriverLocationManager.hasLocationPermission(context)) {
                return
            }
            try {
                val intent = Intent(context, GlipDriverDispatchService::class.java).apply {
                    action = ACTION_START
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                android.util.Log.e("GlipDispatch", "Failed to start service: ${e.message}")
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, GlipDriverDispatchService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
            } catch (_: Exception) {}
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var dispatchJob: Job? = null
    private var locationManager: DriverLocationManager? = null

    private var alertRingtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var lastAlertedRequestId: String? = null

    // GPS & Network Throttling metrics for 50,000 DAU scalability
    private var lastSentLat = 0.0
    private var lastSentLng = 0.0
    private var lastSentTime = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        locationManager = DriverLocationManager(applicationContext)
        locationManager?.startTracking()
        initNotificationChannels()
        initAudioAndVibrator()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                if (!DriverLocationManager.hasLocationPermission(this)) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                try {
                    isRunning = true
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(
                            STATUS_NOTIFICATION_ID,
                            buildStatusNotification(),
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                        )
                    } else {
                        startForeground(STATUS_NOTIFICATION_ID, buildStatusNotification())
                    }
                    startDispatchLoop()
                } catch (e: Exception) {
                    android.util.Log.e("GlipDispatch", "startForeground failed: ${e.message}")
                    stopSelf()
                    return START_NOT_STICKY
                }
            }
            ACTION_STOP -> {
                isRunning = false
                stopAlert()
                FloatingRideOverlayManager.dismissAll()
                stopForeground(true)
                // Instantly notify Supabase that driver is offline
                serviceScope.launch {
                    try {
                        val profile = GlipDriverApplication.repository.profileFlow.firstOrNull()
                        val vehicle = GlipDriverApplication.repository.vehicleFlow.firstOrNull()
                        if (profile != null) {
                            SupabaseClient.updateLiveLocation(
                                mobileNumber = profile.mobileNumber,
                                driverId = profile.id,
                                lat = lastSentLat,
                                lng = lastSentLng,
                                bearing = 0f,
                                isOnline = false,
                                vehicleCategory = vehicle?.category
                            )
                        }
                    } catch (_: Exception) {}
                    stopSelf()
                }
            }
            ACTION_ACCEPT_RIDE -> {
                val reqId = intent.getStringExtra(EXTRA_REQUEST_ID)
                if (!reqId.isNullOrBlank()) {
                    handleAcceptRide(reqId)
                }
            }
            ACTION_DECLINE_RIDE -> {
                val reqId = intent.getStringExtra(EXTRA_REQUEST_ID)
                if (!reqId.isNullOrBlank()) {
                    handleDeclineRide(reqId)
                }
            }
        }
        return START_STICKY
    }

    private fun startDispatchLoop() {
        dispatchJob?.cancel()
        dispatchJob = serviceScope.launch {
            val repository = GlipDriverApplication.repository
            val locManager = locationManager ?: DriverLocationManager(applicationContext)
            var currentBackoffMs = 1800L

            while (isActive && isRunning) {
                var hasActiveContext = false
                try {
                    val profile = repository.profileFlow.firstOrNull()
                    val vehicle = repository.vehicleFlow.firstOrNull()
                    val isOnline = repository.isAvailableFlow.firstOrNull() ?: true

                    if (!isOnline) {
                        isRunning = false
                        stopAlert()
                        stopForeground(true)
                        try {
                            if (profile != null) {
                                SupabaseClient.updateLiveLocation(
                                    mobileNumber = profile.mobileNumber,
                                    driverId = profile.id,
                                    lat = lastSentLat,
                                    lng = lastSentLng,
                                    bearing = 0f,
                                    isOnline = false,
                                    vehicleCategory = vehicle?.category
                                )
                            }
                        } catch (_: Exception) {}
                        stopSelf()
                        break
                    }

                    val gps = locManager.currentLocation.value ?: locManager.getBestLastKnownLocation()
                    val lat = gps?.latitude ?: 0.0
                    val lng = gps?.longitude ?: 0.0
                    val bearing = gps?.bearing ?: 0f
                    val now = System.currentTimeMillis()

                    // 1. Scalable GPS Location Throttling (Don't spam network when driver is stationary)
                    if (profile != null && lat != 0.0 && lng != 0.0) {
                        val distanceMoved = FloatArray(1)
                        if (lastSentLat != 0.0 && lastSentLng != 0.0) {
                            android.location.Location.distanceBetween(lastSentLat, lastSentLng, lat, lng, distanceMoved)
                        } else {
                            distanceMoved[0] = 999f
                        }

                        // Send GPS update only if moved > 5 meters OR at least 5 seconds have elapsed
                        if (distanceMoved[0] >= 5.0f || (now - lastSentTime) >= 5000L) {
                            SupabaseClient.updateLiveLocation(
                                mobileNumber = profile.mobileNumber,
                                driverId = profile.id,
                                lat = lat,
                                lng = lng,
                                bearing = bearing,
                                isOnline = true,
                                vehicleCategory = vehicle?.category
                            )
                            lastSentLat = lat
                            lastSentLng = lng
                            lastSentTime = now
                        }
                    }

                    // 2. Fetch pending ride requests with proximity & waterfall dispatch filters
                    val activeRoute = repository.getRoute()
                    val routeDestLat = if (activeRoute.isActive) activeRoute.destLat else 0.0
                    val routeDestLng = if (activeRoute.isActive) activeRoute.destLng else 0.0

                    val requests = SupabaseClient.fetchPendingRideRequests(
                        driverUuid = profile?.id,
                        driverMobile = profile?.mobileNumber,
                        driverVehicleCategory = vehicle?.category,
                        driverCapacity = vehicle?.totalCapacity,
                        driverLat = lat,
                        driverLng = lng,
                        routeDestLat = routeDestLat,
                        routeDestLng = routeDestLng,
                        driverAvailableSeats = vehicle?.availableSeats ?: (vehicle?.totalCapacity ?: 4)
                    )

                    repository.setIncomingRequests(requests)

                    // 3. Centralized check: If passenger cancelled an active trip, auto-update status
                    val currentTrips = repository.tripsFlow.firstOrNull() ?: emptyList()
                    val activeTrips = currentTrips.filter { it.status == com.glip.driver.domain.model.DemoTripStatus.ACCEPTED }
                    val activeReqIds = activeTrips.map { it.requestId }.filter { it.isNotBlank() }
                    if (activeReqIds.isNotEmpty()) {
                        try {
                            val cancelledIds = SupabaseClient.checkCancelledRequests(activeReqIds)
                            if (cancelledIds.isNotEmpty()) {
                                cancelledIds.forEach { cId ->
                                    val tripToCancel = activeTrips.find { it.requestId == cId }
                                    if (tripToCancel != null) {
                                        repository.updateTripStatus(tripToCancel.id, com.glip.driver.domain.model.DemoTripStatus.CANCELLED)
                                    }
                                }
                            }
                        } catch (_: Exception) {}
                    }

                    // 4. Alert Driver if there is a pending request
                    if (requests.isNotEmpty()) {
                        val topReq = requests.first()
                        if (topReq.id != lastAlertedRequestId) {
                            lastAlertedRequestId = topReq.id
                            triggerRideRequestAlert(topReq)
                        } else if (!MainActivity.isAppInForeground && !FloatingRideOverlayManager.isShowing()) {
                            // Driver switched to another app / minimized while ride request is still pending
                            triggerRideRequestAlert(topReq)
                        }
                    } else {
                        if (lastAlertedRequestId != null) {
                            stopAlert()
                            lastAlertedRequestId = null
                        }
                        // Ensure round "H" Online Bubble is showing when idle outside app
                        if (!MainActivity.isAppInForeground && isRunning) {
                            FloatingRideOverlayManager.showOnlineBubble(applicationContext)
                        }
                    }

                    hasActiveContext = requests.isNotEmpty() || activeTrips.isNotEmpty()
                    // Fast 1.8s polling during active trip or incoming request; 3.5s adaptive polling when idle
                    currentBackoffMs = if (hasActiveContext) 1800L else 3500L
                } catch (_: Exception) {
                    // Exponential backoff on network failure (1.8s -> 3.5s -> 7s -> max 10s)
                    currentBackoffMs = (currentBackoffMs * 1.5).toLong().coerceAtMost(10000L)
                }

                delay(currentBackoffMs)
            }
        }
    }

    private fun triggerRideRequestAlert(request: PassengerRequest) {
        if (!MainActivity.isAppInForeground) {
            startSoundAndVibration()
            showHeadsUpNotification(request)
            showFloatingOverlay(request)
        }
    }

    private fun showHeadsUpNotification(request: PassengerRequest) {
        val acceptIntent = Intent(this, GlipDriverDispatchService::class.java).apply {
            action = ACTION_ACCEPT_RIDE
            putExtra(EXTRA_REQUEST_ID, request.id)
        }
        val acceptPending = PendingIntent.getService(
            this,
            request.id.hashCode() + 1,
            acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val declineIntent = Intent(this, GlipDriverDispatchService::class.java).apply {
            action = ACTION_DECLINE_RIDE
            putExtra(EXTRA_REQUEST_ID, request.id)
        }
        val declinePending = PendingIntent.getService(
            this,
            request.id.hashCode() + 2,
            declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPending = PendingIntent.getActivity(
            this,
            request.id.hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ALERT_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🚕 New Ride: ₹${request.illustrativeFare} - ${request.passengerAlias}")
            .setContentText("📍 ${request.pickupName} ➔ 🏁 ${request.dropoffName}")
            .setSubText("${request.requestedSeats} Seat${if (request.requestedSeats > 1) "s" else ""}")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setOngoing(true)
            .setContentIntent(openAppPending)
            .setFullScreenIntent(openAppPending, true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Decline", declinePending)
            .addAction(android.R.drawable.ic_input_add, "ACCEPT (₹${request.illustrativeFare})", acceptPending)
            .build()

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.notify(RIDE_ALERT_NOTIFICATION_ID, notification)
    }

    private fun showFloatingOverlay(request: PassengerRequest) {
        try {
            FloatingRideOverlayManager.showOverlay(
                context = this,
                request = request,
                onAccept = {
                    handleAcceptRide(request.id)
                },
                onDecline = {
                    handleDeclineRide(request.id)
                }
            )
        } catch (_: Exception) {}
    }

    private fun handleAcceptRide(requestId: String) {
        stopAlert()
        serviceScope.launch {
            GlipDriverApplication.repository.acceptRequest(requestId)
        }
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
    }

    private fun handleDeclineRide(requestId: String) {
        stopAlert()
        serviceScope.launch {
            GlipDriverApplication.repository.declineRequest(requestId)
        }
    }

    private fun startSoundAndVibration() {
        try {
            if (alertRingtone == null) {
                val notificationUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                alertRingtone = RingtoneManager.getRingtone(applicationContext, notificationUri)
            }
            alertRingtone?.play()

            val pattern = longArrayOf(0, 500, 200, 500, 200, 800)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, -1)
            }
        } catch (_: Exception) {}
    }

    private fun stopAlert() {
        try {
            if (alertRingtone?.isPlaying == true) {
                alertRingtone?.stop()
            }
            vibrator?.cancel()
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.cancel(RIDE_ALERT_NOTIFICATION_ID)
            FloatingRideOverlayManager.dismissOverlay()
            if (!MainActivity.isAppInForeground && isRunning) {
                FloatingRideOverlayManager.showOnlineBubble(applicationContext)
            }
        } catch (_: Exception) {}
    }

    private fun buildStatusNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_STATUS_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🟢 Haze Driver - Online")
            .setContentText("Searching for nearby passenger ride requests...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun initNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            // 1. Silent persistent status channel
            val statusChannel = NotificationChannel(
                CHANNEL_STATUS_ID,
                "Driver Online Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active online status for Haze Driver background service"
                setShowBadge(false)
            }
            nm.createNotificationChannel(statusChannel)

            // 2. High priority alert channel
            val alertChannel = NotificationChannel(
                CHANNEL_ALERT_ID,
                "Ride Request Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Heads-up alert notifications when new passenger ride requests arrive"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 800)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            nm.createNotificationChannel(alertChannel)
        }
    }

    private fun initAudioAndVibrator() {
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        isRunning = false
        stopAlert()
        FloatingRideOverlayManager.dismissAll()
        locationManager?.stopTracking()
        dispatchJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }
}
