package com.glip.driver.map

import android.content.Context
import android.location.Location
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.animation.ValueAnimator
import android.view.animation.DecelerateInterpolator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.io.File
import com.glip.driver.domain.model.DriverRoute
import com.glip.driver.domain.model.PassengerRequest
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

/**
 * Helper to resolve corridor coordinates whether in Motipur-Baruraj or Delhi or custom route.
 */
data class ResolvedCorridor(
    val originPoint: GeoPoint,
    val originName: String,
    val destinationPoint: GeoPoint,
    val destinationName: String,
    val intermediateWaypoints: List<Pair<GeoPoint, String>>,
    val allCorridorPoints: List<GeoPoint>
)

fun resolveCorridorPoints(route: DriverRoute?): ResolvedCorridor {
    val originName = route?.originName ?: "Motipur"
    val destName = route?.destinationName ?: "Baruraj"

    val (oLat, oLng) = RouteCoordinateResolver.resolveCoordinates(
        originName,
        route?.originLat ?: 26.1739,
        route?.originLng ?: 85.1818
    )
    val (dLat, dLng) = RouteCoordinateResolver.resolveCoordinates(
        destName,
        route?.destLat ?: 26.1465,
        route?.destLng ?: 85.0782
    )

    val oPoint = GeoPoint(oLat, oLng)
    val dPoint = GeoPoint(dLat, dLng)

    // Intermediate stops if explicitly configured
    val waypoints = route?.intermediateStops?.sortedBy { it.orderIndex }?.map {
        Pair(GeoPoint(it.latitude, it.longitude), it.name)
    } ?: emptyList()

    val allPoints = mutableListOf<GeoPoint>().apply {
        add(oPoint)
        addAll(waypoints.map { it.first })
        add(dPoint)
    }

    return ResolvedCorridor(oPoint, originName, dPoint, destName, waypoints, allPoints)
}

/**
 * Calculates initial bearing from (startLat, startLng) to (endLat, endLng) in degrees [0, 360).
 * 0° = North, 90° = East, 180° = South, 270° = West.
 */
fun calculateBearing(startLat: Double, startLng: Double, endLat: Double, endLng: Double): Float {
    val lat1 = Math.toRadians(startLat)
    val lng1 = Math.toRadians(startLng)
    val lat2 = Math.toRadians(endLat)
    val lng2 = Math.toRadians(endLng)

    val dLng = lng2 - lng1
    val y = Math.sin(dLng) * Math.cos(lat2)
    val x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLng)
    val bearing = Math.toDegrees(Math.atan2(y, x)).toFloat()
    return (bearing + 360f) % 360f
}

/**
 * Modern, High-Performance Native OpenStreetMap Component (Ola / Uber / Rapido Style).
 * - 100% Free & Legal: Powered by OpenStreetMap Mapnik tiles and free OSRM road geometry.
 * - Zero Google Maps API keys or subscriptions required.
 * - Snaps to real roads, turns, and streets via Open Source Routing Machine (OSRM).
 * - Custom driver vehicle puck with pulsing aura, route corridor polyline with casing, and transit pins.
 */
@Composable
fun GlipOsmMapView(
    route: DriverRoute?,
    requests: List<PassengerRequest> = emptyList(),
    driverLiveLocation: Location? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Initialize osmdroid configuration with safe internal app storage (Android 10+ scoped storage fix)
    remember {
        val osmConfig = Configuration.getInstance()
        osmConfig.userAgentValue = "GlipDriverApp/1.0 (Android; info@glip.com)"
        val baseDir = File(context.filesDir, "osmdroid")
        baseDir.mkdirs()
        osmConfig.osmdroidBasePath = baseDir
        val tileCache = File(context.cacheDir, "osmdroid_tiles")
        tileCache.mkdirs()
        osmConfig.osmdroidTileCache = tileCache
        osmConfig.tileDownloadThreads = 4
        osmConfig.cacheMapTileCount = 100
        true
    }

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var casingLineRef by remember { mutableStateOf<Polyline?>(null) }
    var coreLineRef by remember { mutableStateOf<Polyline?>(null) }
    var driverPuckRef by remember { mutableStateOf<Marker?>(null) }
    var originMarkerRef by remember { mutableStateOf<Marker?>(null) }
    var destMarkerRef by remember { mutableStateOf<Marker?>(null) }

    val corridor = remember(route) { resolveCorridorPoints(route) }
    val isRouteActive = route != null && route.isActive && route.destinationName.isNotBlank()

    // Live driver position from real device GPS, fallback to corridor start
    val driverGeoPoint = remember(driverLiveLocation, corridor.originPoint) {
        if (driverLiveLocation != null && driverLiveLocation.latitude != 0.0 && driverLiveLocation.longitude != 0.0) {
            GeoPoint(driverLiveLocation.latitude, driverLiveLocation.longitude)
        } else {
            corridor.originPoint
        }
    }

    // Determine accurate real road destination when route is active
    val effectiveDestPoint = remember(route, driverGeoPoint, isRouteActive) {
        if (!isRouteActive || route == null) {
            corridor.destinationPoint
        } else if (route.destLat != 0.0 && route.destLng != 0.0) {
            GeoPoint(route.destLat, route.destLng)
        } else {
            val (resolvedLat, resolvedLng) = RouteCoordinateResolver.resolveCoordinates(
                route.destinationName,
                driverGeoPoint.latitude,
                driverGeoPoint.longitude
            )
            GeoPoint(resolvedLat, resolvedLng)
        }
    }

    var hasCenteredOnDriver by remember { mutableStateOf(false) }
    var lastAnimatedLat by remember { mutableStateOf(driverGeoPoint.latitude) }
    var lastAnimatedLng by remember { mutableStateOf(driverGeoPoint.longitude) }
    var currentBearing by remember { mutableStateOf(0f) }
    var puckAnimator by remember { mutableStateOf<ValueAnimator?>(null) }

    // Keep vehicle puck synced with live GPS movement, calculate heading, animate smoothly, and auto-center
    LaunchedEffect(driverGeoPoint, driverLiveLocation, effectiveDestPoint, isRouteActive) {
        val targetBearing = when {
            driverLiveLocation != null && driverLiveLocation.hasBearing() && driverLiveLocation.bearing != 0f -> {
                driverLiveLocation.bearing
            }
            lastAnimatedLat != 0.0 && (Math.abs(driverGeoPoint.latitude - lastAnimatedLat) > 0.00002 || Math.abs(driverGeoPoint.longitude - lastAnimatedLng) > 0.00002) -> {
                calculateBearing(lastAnimatedLat, lastAnimatedLng, driverGeoPoint.latitude, driverGeoPoint.longitude)
            }
            isRouteActive -> {
                calculateBearing(driverGeoPoint.latitude, driverGeoPoint.longitude, effectiveDestPoint.latitude, effectiveDestPoint.longitude)
            }
            else -> currentBearing
        }

        val fromLat = lastAnimatedLat
        val fromLng = lastAnimatedLng
        val toLat = driverGeoPoint.latitude
        val toLng = driverGeoPoint.longitude
        val fromAngle = currentBearing

        var deltaAngle = (targetBearing - fromAngle) % 360f
        if (deltaAngle > 180f) deltaAngle -= 360f
        if (deltaAngle < -180f) deltaAngle += 360f

        puckAnimator?.cancel()

        if (fromLat == 0.0 || (fromLat == toLat && fromLng == toLng)) {
            driverPuckRef?.position = driverGeoPoint
            driverPuckRef?.rotation = targetBearing
            currentBearing = targetBearing
            lastAnimatedLat = toLat
            lastAnimatedLng = toLng
        } else {
            val anim = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 800L
                interpolator = DecelerateInterpolator()
                addUpdateListener { va ->
                    val frac = va.animatedFraction
                    val interpLat = fromLat + (toLat - fromLat) * frac
                    val interpLng = fromLng + (toLng - fromLng) * frac
                    val interpAngle = (fromAngle + deltaAngle * frac + 360f) % 360f

                    driverPuckRef?.let { puck ->
                        puck.position = GeoPoint(interpLat, interpLng)
                        puck.rotation = interpAngle
                    }
                    mapViewRef?.invalidate()
                }
            }
            puckAnimator = anim
            anim.start()
            currentBearing = targetBearing
            lastAnimatedLat = toLat
            lastAnimatedLng = toLng
        }

        if (driverLiveLocation != null && driverLiveLocation.latitude != 0.0) {
            mapViewRef?.let { mv ->
                if (!hasCenteredOnDriver) {
                    mv.controller.setCenter(driverGeoPoint)
                    mv.controller.setZoom(17.0)
                    hasCenteredOnDriver = true
                }
            }
        }
        mapViewRef?.invalidate()
    }

    // Lifecycle observer to guarantee tile download threads run when app is in foreground
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapViewRef?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapViewRef?.onPause()
                Lifecycle.Event.ON_DESTROY -> mapViewRef?.onDetach()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            puckAnimator?.cancel()
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapViewRef?.onPause()
            mapViewRef?.onDetach()
        }
    }

    // Fetch turn-by-turn road geometry ONLY when driver explicitly activates a route
    LaunchedEffect(isRouteActive, driverGeoPoint.latitude, driverGeoPoint.longitude, effectiveDestPoint.latitude, effectiveDestPoint.longitude) {
        if (isRouteActive) {
            originMarkerRef?.isEnabled = true
            destMarkerRef?.isEnabled = true
            destMarkerRef?.position = effectiveDestPoint

            val roadResult = OsrmRoutingHelper.fetchRoadRoute(
                originLat = driverGeoPoint.latitude,
                originLng = driverGeoPoint.longitude,
                destLat = effectiveDestPoint.latitude,
                destLng = effectiveDestPoint.longitude
            )

            if (roadResult != null && roadResult.points.isNotEmpty()) {
                casingLineRef?.setPoints(roadResult.points)
                coreLineRef?.setPoints(roadResult.points)

                mapViewRef?.let { mv ->
                    mv.controller.apply {
                        setZoom(17.0)
                        animateTo(driverGeoPoint)
                    }
                    mv.invalidate()
                }
            }
        } else {
            // Remove demo polyline when no route is active
            casingLineRef?.setPoints(emptyList())
            coreLineRef?.setPoints(emptyList())
            originMarkerRef?.isEnabled = false
            destMarkerRef?.isEnabled = false
            mapViewRef?.controller?.apply {
                setZoom(17.0)
                setCenter(driverGeoPoint)
            }
            mapViewRef?.invalidate()
        }
    }

    Box(modifier = modifier) {
        // Native OpenStreetMap Render Surface (Free, Legal, Clean Google/Ola style)
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    isTilesScaledToDpi = true
                    isFlingEnabled = true
                    minZoomLevel = 4.0
                    maxZoomLevel = 20.0
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

                    // Ensure touch pinch-to-zoom and pan events are not intercepted by parent views
                    setOnTouchListener { v, _ ->
                        v.parent?.requestDisallowInterceptTouchEvent(true)
                        false
                    }

                    controller.setZoom(17.0)
                    controller.setCenter(driverGeoPoint)

                    // Polyline: Solid Jet-Black road curve matching Screenshot 2
                    val casingLine = Polyline(this).apply {
                        outlinePaint.color = AndroidColor.parseColor("#334155") // Subtle dark slate border
                        outlinePaint.strokeWidth = 14f
                        outlinePaint.strokeCap = Paint.Cap.ROUND
                        outlinePaint.strokeJoin = Paint.Join.ROUND
                        outlinePaint.isAntiAlias = true
                    }
                    overlays.add(casingLine)
                    casingLineRef = casingLine

                    val coreLine = Polyline(this).apply {
                        outlinePaint.color = AndroidColor.parseColor("#000000") // Solid jet-black road polyline
                        outlinePaint.strokeWidth = 10f
                        outlinePaint.strokeCap = Paint.Cap.ROUND
                        outlinePaint.strokeJoin = Paint.Join.ROUND
                        outlinePaint.isAntiAlias = true
                    }
                    overlays.add(coreLine)
                    coreLineRef = coreLine

                    // Origin Pin (Green 'A' badge)
                    val originMarker = Marker(this).apply {
                        position = driverGeoPoint
                        icon = createPinMarkerDrawable(ctx, label = "A", pinColorHex = "#10B981")
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "Pickup / Start: ${corridor.originName}"
                        isEnabled = isRouteActive
                    }
                    overlays.add(originMarker)
                    originMarkerRef = originMarker

                    // Destination Pin (Red 'B' badge)
                    val destMarker = Marker(this).apply {
                        position = effectiveDestPoint
                        icon = createPinMarkerDrawable(ctx, label = "B", pinColorHex = "#EF4444")
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "Drop / End: ${corridor.destinationName}"
                        isEnabled = isRouteActive
                    }
                    overlays.add(destMarker)
                    destMarkerRef = destMarker

                    // Driver Location Puck (Navigation icon pointing in vehicle heading)
                    val driverPuck = Marker(this).apply {
                        position = driverGeoPoint
                        rotation = currentBearing
                        icon = createDriverVehiclePuck(ctx)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        title = if (driverLiveLocation != null) "Your Vehicle (Live GPS)" else "Your Vehicle"
                    }
                    overlays.add(driverPuck)
                    driverPuckRef = driverPuck

                    // Passenger Pooling Request Pins
                    requests.take(3).forEach { req ->
                        if (req.pickupLat != 0.0 && req.pickupLng != 0.0) {
                            val passMarker = Marker(this).apply {
                                position = GeoPoint(req.pickupLat, req.pickupLng)
                                icon = createPassengerPuck(ctx)
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                title = "Passenger: ${req.passengerAlias} (${req.requestedSeats} seat)"
                                snippet = "${req.pickupName} ➔ ${req.dropoffName}"
                            }
                            overlays.add(passMarker)
                        }
                    }

                    // Start tile downloading immediately
                    onResume()

                    post {
                        controller.setCenter(driverGeoPoint)
                        controller.setZoom(17.0)
                    }

                    mapViewRef = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Floating Action Buttons (Top-End): Recenter to Driver + Route Overview
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 10.dp, end = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    mapViewRef?.controller?.apply {
                        setZoom(17.0)
                        animateTo(driverGeoPoint)
                    }
                },
                modifier = Modifier.size(40.dp),
                containerColor = Color.White,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Recenter Map",
                    tint = Color(0xFF0284C7),
                    modifier = Modifier.size(18.dp)
                )
            }

            if (isRouteActive) {
                FloatingActionButton(
                    onClick = {
                        val minLat = minOf(driverGeoPoint.latitude, effectiveDestPoint.latitude)
                        val maxLat = maxOf(driverGeoPoint.latitude, effectiveDestPoint.latitude)
                        val minLng = minOf(driverGeoPoint.longitude, effectiveDestPoint.longitude)
                        val maxLng = maxOf(driverGeoPoint.longitude, effectiveDestPoint.longitude)
                        val box = BoundingBox(maxLat + 0.008, maxLng + 0.008, minLat - 0.008, minLng - 0.008)
                        mapViewRef?.zoomToBoundingBox(box, true, 80)
                    },
                    modifier = Modifier.size(40.dp),
                    containerColor = Color.White,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AltRoute,
                        contentDescription = "Fit Route Overview",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// PROGRAMMATIC SHARP VECTOR DRAWABLE FACTORY
// ==========================================

/**
 * Driver vehicle puck inspired by Ola/Uber/Rapido:
 * Pulsing radial aura, crisp white boundary, vibrant core, and forward heading indicator.
 */
fun createDriverVehiclePuck(context: Context): Drawable {
    val density = context.resources.displayMetrics.density
    val sizePx = (48 * density).toInt()
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = sizePx / 2f
    val cy = sizePx / 2f

    // 1. Soft pulsing blue radar halo (Ola/Uber accuracy aura)
    val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#38BDF8")
        alpha = 55
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, 23 * density, haloPaint)

    // 2. White outer circle
    val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, 15 * density, whitePaint)

    // 3. Vibrant Ride-hailing Cyan/Teal core
    val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#0284C7")
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, 12 * density, corePaint)

    // 4. White direction/arrow pointer (pointing forward in driving direction)
    val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.FILL
    }
    val path = Path().apply {
        moveTo(cx, cy - (7 * density))
        lineTo(cx + (5 * density), cy + (5 * density))
        lineTo(cx, cy + (3 * density))
        lineTo(cx - (5 * density), cy + (5 * density))
        close()
    }
    canvas.drawPath(path, arrowPaint)

    return BitmapDrawable(context.resources, bitmap)
}

/**
 * Origin ('A' - Green) or Destination ('B' - Red) Pin.
 */
fun createPinMarkerDrawable(context: Context, label: String, pinColorHex: String): Drawable {
    val density = context.resources.displayMetrics.density
    val w = (36 * density).toInt()
    val h = (46 * density).toInt()
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val cx = w / 2f
    val headRadius = 14 * density

    // Pin body
    val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor(pinColorHex)
        style = Paint.Style.FILL
    }
    val path = Path().apply {
        arcTo(
            cx - headRadius, 2 * density,
            cx + headRadius, 2 * density + (headRadius * 2),
            180f, 180f, true
        )
        lineTo(cx, h - (2 * density))
        close()
    }
    canvas.drawPath(path, pinPaint)

    // White circle inside
    val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, 2 * density + headRadius, 10 * density, circlePaint)

    // Letter label (A / B)
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor(pinColorHex)
        textSize = 12 * density
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    val textY = 2 * density + headRadius - ((textPaint.descent() + textPaint.ascent()) / 2)
    canvas.drawText(label, cx, textY, textPaint)

    return BitmapDrawable(context.resources, bitmap)
}

/**
 * Sequential Stop Waypoint Dot (1, 2, 3...).
 */
fun createStopWaypointDrawable(context: Context, number: Int): Drawable {
    val density = context.resources.displayMetrics.density
    val size = (26 * density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = size / 2f
    val cy = size / 2f

    // Navy border
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#0F172A")
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, 11 * density, borderPaint)

    // White fill
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, 9 * density, fillPaint)

    // Order number
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#0F172A")
        textSize = 10 * density
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    val textY = cy - ((textPaint.descent() + textPaint.ascent()) / 2)
    canvas.drawText(number.toString(), cx, textY, textPaint)

    return BitmapDrawable(context.resources, bitmap)
}

/**
 * Passenger Pooling Request Pickup Pin (Amber Dot).
 */
fun createPassengerPuck(context: Context): Drawable {
    val density = context.resources.displayMetrics.density
    val size = (28 * density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = size / 2f
    val cy = size / 2f

    val amberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#F59E0B")
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, 12 * density, amberPaint)

    val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, 10 * density, whitePaint)

    val innerAmber = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#D97706")
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, 6 * density, innerAmber)

    return BitmapDrawable(context.resources, bitmap)
}
