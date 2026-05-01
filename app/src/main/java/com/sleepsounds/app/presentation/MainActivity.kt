package com.sleepsounds.app.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.sleepsounds.app.presentation.theme.SleepSoundsTheme
import com.sleepsounds.app.presentation.theme.ThemeMode
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep splash visible while we load the initial theme preference.
        var keepSplash = true
        splash.setKeepOnScreenCondition { keepSplash }

        setContent {
            val themeMode by mainViewModel.themeMode.collectAsState()
            keepSplash = themeMode == null

            SleepSoundsTheme(themeMode = themeMode ?: ThemeMode.SYSTEM) {
                SleepSoundsApp()
            }
        }
    }
}
