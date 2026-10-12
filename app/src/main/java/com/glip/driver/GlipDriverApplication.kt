package com.glip.driver

import android.app.Application
import com.glip.driver.util.CrashManager

class GlipDriverApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Register global uncaught crash handler to trap all app crashes and present in-app error view
        CrashManager.install(this)
    }
}
