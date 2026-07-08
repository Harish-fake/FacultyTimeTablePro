package com.facultytimetable.pro.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Blue40,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = Blue90,
    onPrimaryContainer = Blue10,
    secondary = Teal40,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = Teal90,
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF002020),
    tertiary = Orange40,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    tertiaryContainer = Orange90,
    onTertiaryContainer = androidx.compose.ui.graphics.Color(0xFF2E0500),
    error = Red40,
    onError = androidx.compose.ui.graphics.Color.White,
    errorContainer = Red90,
    onErrorContainer = androidx.compose.ui.graphics.Color(0xFF410002),
    background = SurfaceLight,
    onBackground = OnSurfaceLight,
    surface = androidx.compose.ui.graphics.Color.White,
    onSurface = OnSurfaceLight,
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFE7E0EC),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF49454F),
    outline = androidx.compose.ui.graphics.Color(0xFF79747E)
)

private val DarkColorScheme = darkColorScheme(
    primary = Blue80,
    onPrimary = Blue20,
    primaryContainer = Blue30,
    onPrimaryContainer = Blue90,
    secondary = Teal80,
    onSecondary = androidx.compose.ui.graphics.Color(0xFF003737),
    secondaryContainer = Teal40,
    onSecondaryContainer = Teal90,
    tertiary = Orange80,
    onTertiary = androidx.compose.ui.graphics.Color(0xFF4E1500),
    tertiaryContainer = Orange40,
    onTertiaryContainer = Orange90,
    error = Red80,
    onError = androidx.compose.ui.graphics.Color(0xFF601410),
    errorContainer = Red40,
    onErrorContainer = Red90,
    background = SurfaceDark,
    onBackground = OnSurfaceDark,
    surface = androidx.compose.ui.graphics.Color(0xFF1E1E1E),
    onSurface = OnSurfaceDark,
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF49454F),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFCAC4D0),
    outline = androidx.compose.ui.graphics.Color(0xFF938F99)
)

@Composable
fun FacultyTimeTableTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
