package com.facultytimetable.pro.presentation.root

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.facultytimetable.pro.presentation.navigation.NavGraph
import com.facultytimetable.pro.presentation.theme.FacultyTimeTableTheme
import com.facultytimetable.pro.presentation.theme.HeaderGradientEnd
import com.facultytimetable.pro.presentation.theme.HeaderGradientStart

@Composable
fun AppRoot(viewModel: RootViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (state.themeMode) {
        "dark" -> true
        "light" -> false
        else -> systemDark
    }

    FacultyTimeTableTheme(darkTheme = darkTheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            var unlocked by rememberSaveable { mutableStateOf(false) }
            var backgroundedAt by rememberSaveable { mutableStateOf(0L) }
            val lifecycleOwner = LocalLifecycleOwner.current

            DisposableEffect(lifecycleOwner, state.isAppLockEnabled) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        // Short trips to the file picker should not force the user to re-enter
                        // their PIN. Anything longer than a minute locks the app again.
                        Lifecycle.Event.ON_STOP -> backgroundedAt = System.currentTimeMillis()
                        Lifecycle.Event.ON_START -> {
                            if (backgroundedAt > 0L &&
                                System.currentTimeMillis() - backgroundedAt > 60_000L
                            ) {
                                unlocked = false
                            }
                        }
                        else -> Unit
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            when {
                state.isLoading -> SplashScreen()
                state.isAppLockEnabled && state.hasPin && !unlocked -> AppLockScreen(
                    verifyPin = viewModel::verifyPin,
                    onUnlocked = { unlocked = true }
                )
                else -> NavGraph()
            }
        }
    }
}

@Composable
private fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(HeaderGradientStart, HeaderGradientEnd))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.School,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Faculty TimeTable Pro",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(20.dp))
            CircularProgressIndicator(color = Color.White.copy(alpha = 0.9f))
        }
    }
}
