package com.glip.driver.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = GlipAccentTeal,
    onPrimary = GlipNavyDark,
    primaryContainer = GlipNavySurface,
    onPrimaryContainer = Color.White,
    secondary = GlipAccentGreen,
    onSecondary = Color.Black,
    background = GlipNavyDark,
    surface = GlipNavySurface,
    onBackground = Color.White,
    onSurface = Color.White,
    error = GlipErrorRed
)

private val LightColorScheme = lightColorScheme(
    primary = GlipNavyPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE2E8F0),
    onPrimaryContainer = GlipNavyPrimary,
    secondary = GlipAccentGreen,
    onSecondary = Color.White,
    tertiary = GlipAccentTeal,
    background = GlipBgLight,
    surface = GlipSurfaceLight,
    onBackground = GlipTextPrimary,
    onSurface = GlipTextPrimary,
    error = GlipErrorRed
)

@Composable
fun GlipDriverTheme(
    darkTheme: Boolean = false, // Driver UX uses high-contrast light theme with dark accents
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = GlipNavyPrimary.toArgb()
                window.navigationBarColor = GlipNavyPrimary.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
