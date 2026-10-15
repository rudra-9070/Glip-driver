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

    /**
     * Fetches exact turn-by-turn road geometry following actual streets and highways.
     * Powered by OpenStreetMap road network. 100% Free, zero API keys required.
     */
    suspend fun fetchRoadRoute(
        originLat: Double,
        originLng: Double,
        destLat: Double,
        destLng: Double
    ): RoadRouteResult? = withContext(Dispatchers.IO) {
        // Query endpoints with fast fallback
        for (baseEndpoint in ENDPOINTS) {
            try {
                val urlString = "$baseEndpoint$originLng,$originLat;$destLng,$destLat?overview=full&geometries=geojson&radiuses=1000;1000&continue_straight=true"
                val url = URL(urlString)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 7000
                    readTimeout = 7000
                    setRequestProperty("User-Agent", "GlipDriver/1.0 (Android; OpenStreetMap)")
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

                                return@withContext RoadRouteResult(
                                    points = geoPoints,
                                    distanceMeters = distanceMeters,
                                    durationSeconds = durationSeconds,
                                    summaryText = summary
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Try next endpoint in list
            }
        }
        null
    }
}
