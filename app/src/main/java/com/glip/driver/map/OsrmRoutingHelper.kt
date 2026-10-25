package com.glip.driver.map

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class RoadRouteResult(
    val points: List<GeoPoint>,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val summaryText: String
)

object OsrmRoutingHelper {

    private val ENDPOINTS = listOf(
        "https://routing.openstreetmap.de/routed-car/route/v1/driving/",
        "https://router.project-osrm.org/route/v1/driving/"
    )

    private val routeCache = java.util.concurrent.ConcurrentHashMap<String, Pair<Long, RoadRouteResult>>()
    private const val CACHE_TTL_MS = 900_000L // 15 minutes cache
    private const val MAX_CACHE_ENTRIES = 500

    private fun makeCacheKey(oLat: Double, oLng: Double, dLat: Double, dLng: Double): String {
        // Quantize coordinates to 3 decimal places (~110m) to reuse routes despite GPS jitter
        val rOLat = (oLat * 1000).toInt()
        val rOLng = (oLng * 1000).toInt()
        val rDLat = (dLat * 1000).toInt()
        val rDLng = (dLng * 1000).toInt()
        return "${rOLat}_${rOLng}_${rDLat}_${rDLng}"
    }

    /**
     * Fetches exact turn-by-turn road geometry following actual streets and highways.
     * Powered by OpenStreetMap road network with in-memory caching and offline fallback.
     */
    suspend fun fetchRoadRoute(
        originLat: Double,
        originLng: Double,
        destLat: Double,
        destLng: Double
    ): RoadRouteResult? = withContext(Dispatchers.IO) {
        if (originLat == 0.0 || originLng == 0.0 || destLat == 0.0 || destLng == 0.0) return@withContext null

        val cacheKey = makeCacheKey(originLat, originLng, destLat, destLng)
        val cached = routeCache[cacheKey]
        if (cached != null && (System.currentTimeMillis() - cached.first) < CACHE_TTL_MS) {
            return@withContext cached.second
        }

        // Query endpoints with fast fallback
        for (baseEndpoint in ENDPOINTS) {
            try {
                val urlString = "$baseEndpoint$originLng,$originLat;$destLng,$destLat?overview=full&geometries=geojson&radiuses=1000;1000&continue_straight=true"
                val url = URL(urlString)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 6000
                    readTimeout = 6000
                    setRequestProperty("User-Agent", "GlipDriver/2.0 (Android; OpenStreetMap)")
                    setRequestProperty("Accept", "application/json")
                }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseBody = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                    val json = JSONObject(responseBody)
                    val code = json.optString("code")

                    if (code == "Ok") {
                        val routesArray = json.optJSONArray("routes")
                        if (routesArray != null && routesArray.length() > 0) {
                            val primaryRoute = routesArray.getJSONObject(0)
                            val distanceMeters = primaryRoute.optDouble("distance", 0.0)
                            val durationSeconds = primaryRoute.optDouble("duration", 0.0)

                            val geometryObj = primaryRoute.optJSONObject("geometry")
                            val coordinatesArray = geometryObj?.optJSONArray("coordinates")

                            if (coordinatesArray != null && coordinatesArray.length() > 0) {
                                val geoPoints = mutableListOf<GeoPoint>()
                                for (i in 0 until coordinatesArray.length()) {
                                    val pointPair = coordinatesArray.getJSONArray(i)
                                    val lng = pointPair.getDouble(0)
                                    val lat = pointPair.getDouble(1)
                                    geoPoints.add(GeoPoint(lat, lng))
                                }

                                val distanceKm = String.format("%.1f km", distanceMeters / 1000.0)
                                val durationMinutes = (durationSeconds / 60.0).toInt()
                                val summary = "$distanceKm • ~$durationMinutes mins via Real Roads"

                                val result = RoadRouteResult(
                                    points = geoPoints,
                                    distanceMeters = distanceMeters,
                                    durationSeconds = durationSeconds,
                                    summaryText = summary
                                )

                                // Bound cache size
                                if (routeCache.size > MAX_CACHE_ENTRIES) {
                                    routeCache.clear()
                                }
                                routeCache[cacheKey] = Pair(System.currentTimeMillis(), result)
                                return@withContext result
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Try next endpoint in list
            }
        }

        // Offline / Network Failure Fallback: calculate straight-line road approximation
        val results = FloatArray(1)
        android.location.Location.distanceBetween(originLat, originLng, destLat, destLng, results)
        val approxMeters = results[0].toDouble() * 1.3
        val distanceKm = String.format("%.1f km", approxMeters / 1000.0)
        val durationMinutes = ((approxMeters / 1000.0) / 25.0 * 60.0).toInt().coerceAtLeast(1)

        val fallbackResult = RoadRouteResult(
            points = listOf(GeoPoint(originLat, originLng), GeoPoint(destLat, destLng)),
            distanceMeters = approxMeters,
            durationSeconds = durationMinutes * 60.0,
            summaryText = "$distanceKm • ~$durationMinutes mins"
        )
        fallbackResult
    }
}
