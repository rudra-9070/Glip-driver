package com.glip.driver.ads

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Singleton State & Cache Manager for Glip Driver In-App Advertisements
 */
object AdManager {
    private const val TAG = "DriverAdManager"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _slotsState = MutableStateFlow<Map<String, PlacementSlot>>(emptyMap())
    val slotsState: StateFlow<Map<String, PlacementSlot>> = _slotsState.asStateFlow()

    // In-memory LRU Bitmap cache (max 25 entries or ~20MB)
    private val memoryCache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(25) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount / 1024
        }
    }

    // Deduplication set for impressions within the same display session
    private val recordedImpressions = mutableSetOf<String>()

    private var isInitialized = false
    private var appContext: Context? = null

    private const val PREFS_NAME = "glip_driver_ads_cache"
    private const val KEY_PLACEMENTS_JSON = "cached_placements_json"

    // Track timestamp of last successful placement sync
    @Volatile
    var lastSyncTimestamp: Long = 0L
        private set

    fun isStale(): Boolean = (System.currentTimeMillis() - lastSyncTimestamp) > 3000L

    // In-flight download tracker to avoid duplicate network fetches
    private val inFlightDownloads = java.util.concurrent.ConcurrentHashMap<String, Boolean>()

    /**
     * Initializes background sync for in-app ad placements
     * Synchronously restores cached placements from disk so UI renders instantly with 0ms blank frame.
     */
    fun initialize(context: Context) {
        val appCtx = context.applicationContext
        appContext = appCtx

        if (isInitialized) return
        isInitialized = true

        Log.d(TAG, "Initializing Driver AdManager with base URL: ${AdConfig.BASE_URL}")

        // 1. Immediately restore cached placements from SharedPreferences for instant UI display
        loadCachedPlacements(appCtx)

        // 2. Launch background sync coroutine loop
        scope.launch(Dispatchers.IO) {
            // First live sync immediately
            syncPlacements()
            while (isActive) {
                delay(AdConfig.REFRESH_INTERVAL_MILLIS)
                syncPlacements()
            }
        }
    }

    /**
     * Restores cached placements from SharedPreferences to show ads immediately at frame 0
     */
    private fun loadCachedPlacements(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(KEY_PLACEMENTS_JSON, null)
            if (!json.isNullOrBlank()) {
                val slots = AdApiClient.parsePlacementsResponse(json)
                if (slots.isNotEmpty()) {
                    _slotsState.value = slots.associateBy { it.slotId }
                    Log.d(TAG, "Restored ${slots.size} driver ad slots from disk cache immediately.")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to restore cached placements", e)
        }
    }

    /**
     * Manually triggers an immediate placements sync
     */
    fun refresh() {
        scope.launch(Dispatchers.IO) {
            syncPlacements()
        }
    }

    private suspend fun syncPlacements() {
        val result = AdApiClient.fetchPlacementsRaw()
        if (result.isSuccess) {
            val (list, rawJson) = result.getOrNull() ?: return
            val map = list.associateBy { it.slotId }
            _slotsState.value = map
            lastSyncTimestamp = System.currentTimeMillis()
            Log.d(TAG, "Driver placements updated successfully. Total slots: ${map.size}")

            // Persist raw placements JSON for instant offline / cold-start rendering
            appContext?.let { ctx ->
                try {
                    val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    prefs.edit().putString(KEY_PLACEMENTS_JSON, rawJson).apply()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to save placements to cache", e)
                }
            }

            // Pre-cache banners in PARALLEL for zero-lag instant rendering across all slider items
            list.flatMap { it.getActiveAdsList() }.forEach { ad ->
                val bannerUrl = ad.bannerImageUrl
                if (!bannerUrl.isNullOrBlank()) {
                    scope.launch(Dispatchers.IO) {
                        if (ad.getMediaType() == AdMediaType.GIF && appContext != null) {
                            loadGifDrawable(appContext!!, bannerUrl)
                        } else {
                            loadBitmap(bannerUrl)
                        }
                    }
                }
            }
        } else {
            Log.w(TAG, "Driver placements sync failed: ${result.exceptionOrNull()?.message}")
        }
    }

    private fun getDiskCacheFile(imageUrl: String): java.io.File? {
        val dir = appContext?.cacheDir ?: return null
        val adsDir = java.io.File(dir, "glip_driver_ads_img")
        if (!adsDir.exists()) {
            adsDir.mkdirs()
        }
        val safeName = "ad_img_" + imageUrl.hashCode().toString() + ".bin"
        return java.io.File(adsDir, safeName)
    }

    /**
     * Synchronously checks memory cache and disk cache.
     * Returns immediately (0-2ms) if available, eliminating blank frame delay in Compose.
     */
    fun getCachedBitmap(imageUrl: String): Bitmap? {
        if (imageUrl.isBlank()) return null

        // 1. Check in-memory LRU cache
        synchronized(memoryCache) {
            val memoryHit = memoryCache.get(imageUrl)
            if (memoryHit != null) return memoryHit
        }

        // 2. Check disk cache
        try {
            val file = getDiskCacheFile(imageUrl)
            if (file != null && file.exists() && file.length() > 0) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    synchronized(memoryCache) {
                        memoryCache.put(imageUrl, bitmap)
                    }
                    return bitmap
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to decode disk-cached bitmap", e)
        }

        return null
    }

    /**
     * Retrieves cached bitmap (memory -> disk) or downloads asynchronously with hardware acceleration
     */
    suspend fun loadBitmap(imageUrl: String): Bitmap? = withContext(Dispatchers.IO) {
        if (imageUrl.isBlank()) return@withContext null

        // Fast return if already cached in memory or on disk
        getCachedBitmap(imageUrl)?.let { return@withContext it }

        // Deduplicate in-flight downloads for the same image
        if (inFlightDownloads.putIfAbsent(imageUrl, true) != null) {
            // Another coroutine is already downloading this image, wait briefly
            for (i in 0 until 10) {
                delay(100)
                getCachedBitmap(imageUrl)?.let { return@withContext it }
            }
        }

        try {
            val url = URL(imageUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 12000
                doInput = true
                useCaches = true
                setRequestProperty("User-Agent", "GlipDriver-Android-App/1.0")
            }
            connection.connect()
            val bytes = connection.inputStream.buffered().use { it.readBytes() }

            if (bytes.isNotEmpty()) {
                val opts = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                if (bitmap != null) {
                    synchronized(memoryCache) {
                        memoryCache.put(imageUrl, bitmap)
                    }
                    // Save to disk cache for instant offline / next-launch display
                    try {
                        val file = getDiskCacheFile(imageUrl)
                        file?.writeBytes(bytes)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to write bitmap to disk cache", e)
                    }
                }
                bitmap
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download driver ad banner bitmap: $imageUrl", e)
            null
        } finally {
            inFlightDownloads.remove(imageUrl)
        }
    }

    /**
     * Downloads and caches animated GIF as native Drawable
     */
    suspend fun loadGifDrawable(context: Context, gifUrl: String): android.graphics.drawable.Drawable? = withContext(Dispatchers.IO) {
        if (gifUrl.isBlank()) return@withContext null
        try {
            val fileName = "ad_gif_" + gifUrl.hashCode().toString() + ".gif"
            val file = java.io.File(context.cacheDir, fileName)

            if (!file.exists() || file.length() == 0L) {
                val url = URL(gifUrl)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                    doInput = true
                    setRequestProperty("User-Agent", "GlipDriver-Android-App/1.0")
                }
                connection.inputStream.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val source = android.graphics.ImageDecoder.createSource(file)
                android.graphics.ImageDecoder.decodeDrawable(source)
            } else {
                android.graphics.drawable.BitmapDrawable(context.resources, file.absolutePath)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load GIF drawable: $gifUrl", e)
            null
        }
    }

    /**
     * Records impression telemetry once per ad display
     */
    fun onAdDisplayed(adId: String, slotId: String) {
        val key = "${adId}_$slotId"
        if (recordedImpressions.add(key)) {
            scope.launch(Dispatchers.IO) {
                AdApiClient.recordImpression(adId, slotId)
                Log.d(TAG, "Recorded impression for driver ad: $adId in slot: $slotId")
            }
        }
    }

    /**
     * Handles ad click: fires telemetry & launches destination URL (Website or WhatsApp)
     */
    fun onAdClicked(context: Context, ad: AppAd, slotId: String) {
        Log.d(TAG, "Driver ad clicked: ${ad.name} -> ${ad.destinationUrl}")

        // 1. Report click telemetry in background
        scope.launch(Dispatchers.IO) {
            AdApiClient.recordClick(ad.id, slotId)
        }

        // 2. Open destination URL
        try {
            val rawUrl = ad.destinationUrl.trim()
            val uri = if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
                Uri.parse(rawUrl)
            } else {
                Uri.parse("https://$rawUrl")
            }

            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch destination url: ${ad.destinationUrl}", e)
        }
    }
}
