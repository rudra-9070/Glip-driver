package com.glip.driver.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class SchoolApplicationData(
    val driverName: String,
    val driverMobile: String,
    val vehicleNumber: String,
    val homeAddress: String,
    val homeLat: Double = 0.0,
    val homeLng: Double = 0.0,
    val status: String = "PENDING",
    val driverSchoolId: String? = null,
    val createdAt: String? = null
)

/**
 * Dedicated Supabase REST client for School Transportation service registrations.
 * Uses the dedicated project provided by the user.
 */
object SchoolSupabaseClient {

    private const val TAG = "SchoolSupabaseClient"

    private const val BASE_URL = "https://uugzolriwmbhssgfjbap.supabase.co/rest/v1"
    private const val API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InV1Z3pvbHJpd21iaHNzZ2ZqYmFwIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTIzNTYzMTQsImV4cCI6MjEwNzkzMjMxNH0.rVBGf5CDhLSevaXbZrMptk0-w1xU4YOl2UIsr5FzeP8"

    suspend fun registerSchoolTransportation(
        driverName: String,
        driverMobile: String,
        vehicleNumber: String,
        homeAddress: String,
        homeLat: Double,
        homeLng: Double
    ): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        try {
            val cleanMobile = driverMobile.replace(Regex("[^0-9]"), "").takeLast(10)
            val endpoint = "$BASE_URL/school_transportation"
            val url = URL(endpoint)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 12000
                readTimeout = 12000
                setRequestProperty("apikey", API_KEY)
                setRequestProperty("Authorization", "Bearer $API_KEY")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Prefer", "return=minimal")
                doOutput = true
            }

            val payload = JSONObject().apply {
                put("driver_name", driverName.trim())
                put("driver_mobile", cleanMobile)
                put("vehicle_number", vehicleNumber.trim())
                put("home_address", homeAddress.trim())
                put("home_lat", homeLat)
                put("home_lng", homeLng)
                put("status", "PENDING")
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val code = conn.responseCode
            if (code in 200..299) {
                Pair(true, null)
            } else {
                val errorStream = conn.errorStream ?: conn.inputStream
                val errResp = errorStream?.let {
                    BufferedReader(InputStreamReader(it)).use { r -> r.readText() }
                } ?: "HTTP $code"
                Log.w(TAG, "School transportation save error ($code): $errResp")
                Pair(false, errResp)
            }
        } catch (e: Exception) {
            Log.e(TAG, "registerSchoolTransportation failed: ${e.message}", e)
            Pair(false, e.localizedMessage ?: "Network error")
        }
    }

    suspend fun getSchoolTransportationApplication(driverMobile: String): SchoolApplicationData? = withContext(Dispatchers.IO) {
        try {
            val clean = driverMobile.replace(Regex("[^0-9]"), "").takeLast(10)
            if (clean.isBlank()) return@withContext null
            val endpoint = "$BASE_URL/school_transportation?driver_mobile=eq.$clean&select=*&order=created_at.desc&limit=1"
            val url = URL(endpoint)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("apikey", API_KEY)
                setRequestProperty("Authorization", "Bearer $API_KEY")
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode in 200..299) {
                val resp = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val arr = JSONArray(resp)
                if (arr.length() > 0) {
                    val obj = arr.getJSONObject(0)
                    return@withContext SchoolApplicationData(
                        driverName = obj.optString("driver_name", ""),
                        driverMobile = obj.optString("driver_mobile", clean),
                        vehicleNumber = obj.optString("vehicle_number", ""),
                        homeAddress = obj.optString("home_address", ""),
                        homeLat = obj.optDouble("home_lat", 0.0),
                        homeLng = obj.optDouble("home_lng", 0.0),
                        status = obj.optString("status", "PENDING"),
                        driverSchoolId = if (obj.isNull("driver_school_id")) null else obj.optString("driver_school_id", null),
                        createdAt = if (obj.isNull("created_at")) null else obj.optString("created_at", null)
                    )
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "getSchoolTransportationApplication failed: ${e.message}")
            null
        }
    }
}
