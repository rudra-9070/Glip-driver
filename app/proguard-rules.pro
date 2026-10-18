# Add project specific ProGuard rules here.

# Keep Kotlin Coroutines & Reflection
-keepclassmembers class kotlinx.coroutines.** { *; }

# Keep Domain Models & Data Transfer Objects
-keep class com.glip.driver.domain.model.** { *; }
-keep class com.glip.driver.data.model.** { *; }
-keep class com.glip.driver.data.remote.** { *; }
-keep class com.glip.driver.util.** { *; }

# Keep OpenStreetMap (OSMDroid)
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# Keep MultiDex
-keep class androidx.multidex.** { *; }

# Keep Jetpack Compose & Navigation
-keep class androidx.compose.** { *; }
-keep class androidx.navigation.** { *; }
