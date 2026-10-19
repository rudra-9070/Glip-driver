package com.glip.driver.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

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
                put("driver_mobile", driverMobile.trim())
                put("vehicle_number", vehicleNumber.trim())
                put("home_address", homeAddress.trim())
                put("home_lat", homeLat)
                put("home_lng", homeLng)
                put("status", "REGISTERED")
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
}
