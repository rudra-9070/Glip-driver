package com.glip.driver

import android.app.Application
import android.content.Context
import androidx.multidex.MultiDex
import com.glip.driver.data.local.LocalDemoSessionManager
import com.glip.driver.data.repository.DemoDriverRepositoryImpl
import com.glip.driver.domain.usecase.RouteMatchingEngine
import com.glip.driver.util.CrashManager
import org.osmdroid.config.Configuration
import java.io.File

class GlipDriverApplication : Application() {

    companion object {
        lateinit var instance: GlipDriverApplication
            private set

        val repository: DemoDriverRepositoryImpl by lazy {
            val sessionManager = LocalDemoSessionManager(instance.applicationContext)
            val matchingEngine = RouteMatchingEngine()
            DemoDriverRepositoryImpl(
                sessionManager = sessionManager,
                matchingEngine = matchingEngine
            )
        }
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        try {
            MultiDex.install(this)
        } catch (_: Exception) {}
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        // Register global uncaught crash handler to trap all app crashes and present in-app error view
        try {
            CrashManager.install(this)
        } catch (_: Exception) {}

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
        } catch (e: Exception) {
            // Safe fallback
        }
    }
}
