package com.glip.driver.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
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

    // Check if route has explicit non-zero coordinates
    if (route != null && (route.originLat != 0.0 || route.destLat != 0.0)) {
        val oPoint = GeoPoint(route.originLat, route.originLng)
        val dPoint = GeoPoint(route.destLat, route.destLng)
        val waypoints = route.intermediateStops.sortedBy { it.orderIndex }.map {
            Pair(GeoPoint(it.latitude, it.longitude), it.name)
        }
        val allPoints = mutableListOf<GeoPoint>().apply {
            add(oPoint)
            addAll(waypoints.map { it.first })
            add(dPoint)
        }
        return ResolvedCorridor(oPoint, originName, dPoint, destName, waypoints, allPoints)
    }

    // Default corridor: Motipur to Baruraj (Muzaffarpur, Bihar)
    val oPoint = GeoPoint(26.1739, 85.1818) // Motipur
    val midPoint = GeoPoint(26.1600, 85.1300) // Tajpur Mor / Bypass
    val dPoint = GeoPoint(26.1465, 85.0782) // Baruraj

    val waypoints = listOf(Pair(midPoint, "Tajpur Mor / Bypass"))
    val allPoints = listOf(oPoint, midPoint, dPoint)

    return ResolvedCorridor(oPoint, originName, dPoint, destName, waypoints, allPoints)
}

/**
 * Modern, High-Performance Native OpenStreetMap Component (Ola / Uber / Rapido Style).
 * - 100% Free & Legal: Powered by OpenStreetMap & CartoDB tiles under ODbL with legal attribution.
 * - Zero Google Maps API keys or subscriptions required.
 * - Hardware accelerated with offline disk caching for high performance.
 * - Custom driver vehicle puck, route corridor polyline with casing, and transit pins.
 */
@Composable
fun GlipOsmMapView(
    route: DriverRoute?,
    requests: List<PassengerRequest> = emptyList(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    val corridor = remember(route) { resolveCorridorPoints(route) }

    DisposableEffect(Unit) {
        onDispose {
            mapViewRef?.onDetach()
        }
    }

    Box(modifier = modifier) {
        // Native OpenStreetMap Render Surface
        AndroidView(
            factory = { ctx ->
                // Configure osmdroid user agent & preferences safely
                val prefs = ctx.getSharedPreferences("glip_osmdroid_prefs", Context.MODE_PRIVATE)
                Configuration.getInstance().load(ctx, prefs)
                Configuration.getInstance().userAgentValue = "GlipDriverApp/1.0 (Android; OpenStreetMap)"
                Configuration.getInstance().cacheMapTileCount = 100
                Configuration.getInstance().tileDownloadThreads = 4

                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    isTilesScaledToDpi = true
                    isFlingEnabled = true
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

                    controller.setZoom(14.5)
                    controller.setCenter(corridor.originPoint)

                    // Draw Corridor Polyline (Double casing effect like Ola/Uber)
                    val casingLine = Polyline(this).apply {
                        outlinePaint.color = AndroidColor.parseColor("#0F172A")
                        outlinePaint.strokeWidth = 14f
                        outlinePaint.strokeCap = Paint.Cap.ROUND
                        outlinePaint.strokeJoin = Paint.Join.ROUND
                        outlinePaint.isAntiAlias = true
                        setPoints(corridor.allCorridorPoints)
                    }
                    overlays.add(casingLine)

                    val coreLine = Polyline(this).apply {
                        outlinePaint.color = AndroidColor.parseColor("#0284C7") // Electric cyan
                        outlinePaint.strokeWidth = 9f
                        outlinePaint.strokeCap = Paint.Cap.ROUND
                        outlinePaint.strokeJoin = Paint.Join.ROUND
                        outlinePaint.isAntiAlias = true
                        setPoints(corridor.allCorridorPoints)
                    }
                    overlays.add(coreLine)

                    // Origin Pin (Green 'A')
                    val originMarker = Marker(this).apply {
                        position = corridor.originPoint
                        icon = createPinMarkerDrawable(ctx, label = "A", pinColorHex = "#10B981")
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "Pickup / Start: ${corridor.originName}"
                        snippet = "Planned departure corridor"
                    }
                    overlays.add(originMarker)

                    // Intermediate Stops (Numbered Waypoint Dots)
                    corridor.intermediateWaypoints.forEachIndexed { idx, (point, stopName) ->
                        val stopMarker = Marker(this).apply {
                            position = point
                            icon = createStopWaypointDrawable(ctx, number = idx + 1)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            title = "Stop ${idx + 1}: $stopName"
                            snippet = "Sequential corridor pickup stop"
                        }
                        overlays.add(stopMarker)
                    }

                    // Destination Pin (Red 'B')
                    val destMarker = Marker(this).apply {
                        position = corridor.destinationPoint
                        icon = createPinMarkerDrawable(ctx, label = "B", pinColorHex = "#EF4444")
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "Drop / End: ${corridor.destinationName}"
                        snippet = "Corridor final destination"
                    }
                    overlays.add(destMarker)

                    // Driver Location Puck (Ola/Uber navigation aura)
                    val driverPuck = Marker(this).apply {
                        position = corridor.originPoint
                        icon = createDriverVehiclePuck(ctx)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        title = "Your Vehicle (Online)"
                        snippet = "Active for route sharing"
                    }
                    overlays.add(driverPuck)

                    // Projected Passenger Pooling Request Pins
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

                    // Legal OpenStreetMap attribution overlay
                    val copyrightOverlay = CopyrightOverlay(ctx).apply {
                        setOffset(12, 12)
                        setTextSize(8)
                        setTextColor(AndroidColor.parseColor("#475569"))
                    }
                    overlays.add(copyrightOverlay)

                    // Smooth initial fit to whole corridor after layout
                    post {
                        if (corridor.allCorridorPoints.isNotEmpty()) {
                            try {
                                val bbox = BoundingBox.fromGeoPoints(corridor.allCorridorPoints)
                                zoomToBoundingBox(bbox, true, 90)
                            } catch (_: Exception) {
                                controller.setCenter(corridor.originPoint)
                            }
                        }
                    }

                    mapViewRef = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Recenter to Driver Location FAB (Right Hand / Thumb-Friendly for Drivers)
        FloatingActionButton(
            onClick = {
                mapViewRef?.controller?.animateTo(corridor.originPoint)
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 12.dp)
                .size(42.dp),
            containerColor = Color.White,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "Recenter Map",
                tint = Color(0xFF0284C7),
                modifier = Modifier.size(20.dp)
            )
        }

        // Legal Compliance Attribution Pill (Free & Safe)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 10.dp, bottom = 10.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xCC0F172A)
        ) {
            Text(
                text = "© OpenStreetMap contributors",
                color = Color(0xFFE2E8F0),
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
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
