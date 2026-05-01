package com.sleepsounds.app.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class ThemeMode { LIGHT, DARK, SYSTEM }

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FB7FF),
    onPrimary = Color(0xFF002D6E),
    primaryContainer = Color(0xFF0046A3),
    onPrimaryContainer = Color(0xFFD9E2FF),
    secondary = Color(0xFFC1C5DD),
    onSecondary = Color(0xFF293042),
    secondaryContainer = Color(0xFF3F4759),
    onSecondaryContainer = Color(0xFFDDE1F9),
    tertiary = Color(0xFFE2BBE0),
    onTertiary = Color(0xFF422745),
    background = Color(0xFF0E1330),
    onBackground = Color(0xFFE3E2EC),
    surface = Color(0xFF131A38),
    onSurface = Color(0xFFE3E2EC),
    surfaceVariant = Color(0xFF1A2245),
    onSurfaceVariant = Color(0xFFC4C6D0),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF305FA8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD9E2FF),
    onPrimaryContainer = Color(0xFF001A41),
    secondary = Color(0xFF575E71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDBE2F9),
    onSecondaryContainer = Color(0xFF141B2C),
    tertiary = Color(0xFF735471),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF8F9FF),
    onBackground = Color(0xFF1B1B1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1B1B1F),
    surfaceVariant = Color(0xFFE1E2EC),
    onSurfaceVariant = Color(0xFF44464F),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
)

@Composable
fun SleepSoundsTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemDark
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        useDark -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SleepSoundsTypography,
        content = content,
    )
}
