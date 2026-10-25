# ProGuard / R8 rules for Glip Driver App
# Preserves stack traces, reflection, domain models, services, and overlays

# Preserve Line Numbers and Source File Attributes for accurate Play Console Crash Reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep Kotlin Coroutines & Reflection
-keepclassmembers class kotlinx.coroutines.** { *; }

# Keep Domain Models & Data Transfer Objects
-keep class com.glip.driver.domain.model.** { *; }
-keep class com.glip.driver.data.model.** { *; }
-keep class com.glip.driver.data.remote.** { *; }
-keep class com.glip.driver.util.** { *; }

# Keep Background Dispatch Service & Overlay Manager
-keep class com.glip.driver.service.** { *; }
-keep class com.glip.driver.location.** { *; }

# Keep In-App Crash Diagnostic Activity
-keep class com.glip.driver.ui.crash.** { *; }

# Keep OpenStreetMap (OSMDroid)
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# Keep MultiDex
-keep class androidx.multidex.** { *; }

# Keep Jetpack Compose & Navigation
-keep class androidx.compose.** { *; }
-keep class androidx.navigation.** { *; }

# Google Play Services & Maps (for seamless SDK integration)
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**
