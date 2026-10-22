package com.glip.driver

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.glip.driver.ads.AdManager
import com.glip.driver.data.local.LocalDemoSessionManager
import com.glip.driver.data.repository.DemoDriverRepositoryImpl
import com.glip.driver.domain.usecase.RouteMatchingEngine
import com.glip.driver.navigation.GlipDriverNavHost
import com.glip.driver.ui.theme.GlipDriverTheme

class MainActivity : ComponentActivity() {

    companion object {
        var isAppInForeground: Boolean = false
            private set
    }

    override fun onResume() {
        super.onResume()
        isAppInForeground = true
        com.glip.driver.service.FloatingRideOverlayManager.dismissOverlay()
        // Refresh live driver ad placements on returning to foreground
        try {
            AdManager.refresh()
        } catch (_: Exception) {}
    }

    override fun onPause() {
        super.onPause()
        isAppInForeground = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Use shared driver repository instance across UI and Background Dispatch Service
        val repository = GlipDriverApplication.repository
        val sessionManager = LocalDemoSessionManager(applicationContext)

        if (sessionManager.isDriverAvailable() && com.glip.driver.location.DriverLocationManager.hasLocationPermission(this)) {
            com.glip.driver.service.GlipDriverDispatchService.start(this)
        }

        setContent {
            GlipDriverTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    GlipDriverNavHost(
                        navController = navController,
                        repository = repository
                    )
                }
            }
        }
    }
}
