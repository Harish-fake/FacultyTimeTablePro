package com.facultytimetable.pro.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Blue40,
    onPrimary = Color.White,
    primaryContainer = Blue90,
    onPrimaryContainer = Blue10,
    secondary = Teal40,
    onSecondary = Color.White,
    secondaryContainer = Teal90,
    onSecondaryContainer = Teal10,
    tertiary = Orange40,
    onTertiary = Color.White,
    tertiaryContainer = Orange90,
    onTertiaryContainer = Color(0xFF3B1D00),
    error = Red40,
    onError = Color.White,
    errorContainer = Red90,
    onErrorContainer = Color(0xFF450A0A),
    background = SurfaceLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceContainerLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = Color(0xFFEEF2F7),
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceTint = Blue40,
    outline = OutlineLight,
    outlineVariant = Color(0xFFE2E8F0),
    inverseSurface = Color(0xFF1E293B),
    inverseOnSurface = Color(0xFFF8FAFC),
    inversePrimary = Blue80,
    scrim = Color(0xFF020617)
)

private val DarkColorScheme = darkColorScheme(
    primary = Blue80,
    onPrimary = Color(0xFF082F6B),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Teal80,
    onSecondary = Color(0xFF042F2E),
    secondaryContainer = Color(0xFF115E59),
    onSecondaryContainer = Color(0xFFCCFBF1),
    tertiary = Orange80,
    onTertiary = Color(0xFF3B1D00),
    tertiaryContainer = Color(0xFF92400E),
    onTertiaryContainer = Color(0xFFFEF3C7),
    error = Red80,
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
    background = SurfaceDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceContainerDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = Color(0xFF1C2940),
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceTint = Blue80,
    outline = OutlineDark,
    outlineVariant = Color(0xFF243147),
    inverseSurface = Color(0xFFE2E8F0),
    inverseOnSurface = Color(0xFF0F172A),
    inversePrimary = Blue20,
    scrim = Color.Black
)

/**
 * App theme. Dynamic colour is intentionally disabled so the product keeps a consistent
 * brand identity and timetable subject colours stay meaningful on every device.
 */
@Composable
fun FacultyTimeTableTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
