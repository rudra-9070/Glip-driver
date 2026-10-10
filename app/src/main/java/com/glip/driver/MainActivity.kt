package com.glip.driver

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.glip.driver.data.local.LocalDemoSessionManager
import com.glip.driver.data.repository.DemoDriverRepositoryImpl
import com.glip.driver.domain.usecase.RouteMatchingEngine
import com.glip.driver.navigation.GlipDriverNavHost
import com.glip.driver.ui.theme.GlipDriverTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize local demo session manager and driver repository
        val sessionManager = LocalDemoSessionManager(applicationContext)
        val matchingEngine = RouteMatchingEngine()
        val repository = DemoDriverRepositoryImpl(
            sessionManager = sessionManager,
            matchingEngine = matchingEngine
        )

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
