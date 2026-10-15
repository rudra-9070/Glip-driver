package com.glip.driver.map

import android.content.Context
import android.location.Geocoder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

data class GeocodeResult(
    val address: String,
    val latitude: Double,
    val longitude: Double
)

object OsmGeocoderHelper {
    private const val TAG = "OsmGeocoderHelper"

    /**
     * Search coordinates and full address from a search query using Nominatim OSM API,
     * with fallback to Android system Geocoder and RouteCoordinateResolver.
     */
    suspend fun searchLocation(context: Context, query: String): GeocodeResult? = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext null

        // 1. Try OpenStreetMap Nominatim Geocoding API (Fast, Free, accurate India & global data)
        try {
            val encodedQuery = URLEncoder.encode(trimmed, "UTF-8")
            val urlString = "https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=json&addressdetails=1&limit=1"
            val url = URL(urlString)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("User-Agent", "GlipDriverApp/1.0 (Android)")
                setRequestProperty("Accept", "application/json")
            }

            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                reader.close()

                val jsonArray = JSONArray(response)
                if (jsonArray.length() > 0) {
                    val obj = jsonArray.getJSONObject(0)
                    val lat = obj.getDouble("lat")
                    val lon = obj.getDouble("lon")
                    val displayName = obj.optString("display_name", trimmed)
                    return@withContext GeocodeResult(
                        address = displayName,
                        latitude = lat,
                        longitude = lon
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Nominatim search failed: ${e.message}")
        }

        // 2. Fallback to Android system Geocoder
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val results = geocoder.getFromLocationName(trimmed, 1)
            if (!results.isNullOrEmpty()) {
                val found = results[0]
                val addr = found.getAddressLine(0) ?: trimmed
                return@withContext GeocodeResult(
                    address = addr,
                    latitude = found.latitude,
                    longitude = found.longitude
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Android Geocoder search failed: ${e.message}")
        }

        // 3. Fallback to RouteCoordinateResolver
        val (lat, lng) = RouteCoordinateResolver.resolveCoordinates(trimmed)
        return@withContext GeocodeResult(
            address = trimmed,
            latitude = lat,
            longitude = lng
        )
    }

    /**
     * Reverse geocode a latitude/longitude coordinate to a human-readable street/city address.
     */
    suspend fun reverseGeocode(context: Context, lat: Double, lng: Double): String = withContext(Dispatchers.IO) {
        // 1. Try OpenStreetMap Nominatim Reverse API
        try {
            val urlString = "https://nominatim.openstreetmap.org/reverse?lat=$lat&lon=$lng&format=json&addressdetails=1"
            val url = URL(urlString)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("User-Agent", "GlipDriverApp/1.0 (Android)")
                setRequestProperty("Accept", "application/json")
            }

            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                reader.close()

                val obj = JSONObject(response)
                val displayName = obj.optString("display_name")
                if (!displayName.isNullOrBlank()) {
                    return@withContext displayName
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Nominatim reverse failed: ${e.message}")
        }

        // 2. Fallback to Android Geocoder
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                val found = addresses[0]
                val line = found.getAddressLine(0)
                if (!line.isNullOrBlank()) {
                    return@withContext line
                }
                val locality = found.locality ?: found.subLocality ?: found.featureName
                if (!locality.isNullOrBlank()) {
                    return@withContext locality
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Android Geocoder reverse failed: ${e.message}")
        }

        return@withContext "Location (${String.format(Locale.US, "%.5f, %.5f", lat, lng)})"
    }
}
