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

    /**
     * Fetches exact turn-by-turn road geometry following actual streets and highways.
     * Powered by Open Source Routing Machine (OSRM) and OpenStreetMap road network.
     * 100% Free, zero API keys required.
     */
    suspend fun fetchRoadRoute(
        originLat: Double,
        originLng: Double,
        destLat: Double,
        destLng: Double
    ): RoadRouteResult? = withContext(Dispatchers.IO) {
        try {
            // OSRM expects coordinates in {longitude},{latitude} format
            val urlString = "https://router.project-osrm.org/route/v1/driving/$originLng,$originLat;$destLng,$destLat?overview=full&geometries=geojson"
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("User-Agent", "GlipDriverApp/1.0 (Android; OpenStreetMap)")
                setRequestProperty("Accept", "application/json")
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext null
            }

            val responseBody = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
            val json = JSONObject(responseBody)
            val code = json.optString("code")

            if (code != "Ok") {
                return@withContext null
            }

            val routesArray = json.getJSONArray("routes")
            if (routesArray.length() == 0) {
                return@withContext null
            }

            val primaryRoute = routesArray.getJSONObject(0)
            val distanceMeters = primaryRoute.optDouble("distance", 0.0)
            val durationSeconds = primaryRoute.optDouble("duration", 0.0)

            val geometryObj = primaryRoute.getJSONObject("geometry")
            val coordinatesArray = geometryObj.getJSONArray("coordinates")

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

            RoadRouteResult(
                points = geoPoints,
                distanceMeters = distanceMeters,
                durationSeconds = durationSeconds,
                summaryText = summary
            )
        } catch (e: Exception) {
            // Fallback gracefully without throwing
            null
        }
    }
}
