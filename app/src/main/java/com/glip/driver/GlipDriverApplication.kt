package com.glip.driver

import android.app.Application
import android.content.Context
import com.glip.driver.util.CrashManager
import org.osmdroid.config.Configuration
import java.io.File

class GlipDriverApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Register global uncaught crash handler to trap all app crashes and present in-app error view
        CrashManager.install(this)

        // Initialize osmdroid configuration early with safe app cache dirs
        try {
            val osmConfig = Configuration.getInstance()
            val prefs = getSharedPreferences("glip_osmdroid_prefs", Context.MODE_PRIVATE)
            osmConfig.load(this, prefs)
            osmConfig.userAgentValue = packageName
            val osmBasePath = File(cacheDir, "osmdroid")
            osmBasePath.mkdirs()
            osmConfig.osmdroidBasePath = osmBasePath
            val osmTileCache = File(osmBasePath, "tiles")
            osmTileCache.mkdirs()
            osmConfig.osmdroidTileCache = osmTileCache
        } catch (_: Exception) {
            // Safe fallback
        }
    }
}
