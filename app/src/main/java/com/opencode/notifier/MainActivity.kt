package com.opencode.notifier

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.opencode.notifier.data.SettingsRepository
import com.opencode.notifier.service.OpenCodeEventService
import com.opencode.notifier.ui.HomeScreen
import com.opencode.notifier.ui.SettingsScreen
import com.opencode.notifier.ui.theme.OpencodeNotifierTheme
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {

    private lateinit var settingsRepository: SettingsRepository
    private var isServiceStarted by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        settingsRepository = SettingsRepository(this)

        requestNotificationPermission()

        setContent {
            var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

            OpencodeNotifierTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    when (currentScreen) {
                        Screen.Home -> {
                            HomeScreen(
                                onNavigateToSettings = { currentScreen = Screen.Settings },
                                isServiceConnected = isServiceStarted,
                                settingsRepository = settingsRepository
                            )
                        }
                        Screen.Settings -> {
                            SettingsScreen(
                                onBack = {
                                    currentScreen = Screen.Home
                                    tryStartService()
                                },
                                settingsRepository = settingsRepository
                            )
                        }
                    }
                }
            }
        }

        tryStartService()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    100
                )
            }
        }
    }

    private fun tryStartService() {
        val settings = kotlinx.coroutines.runBlocking {
            settingsRepository.settings.first()
        }
        if (!settings.isConfigured) return

        val intent = Intent(this, OpenCodeEventService::class.java).apply {
            action = OpenCodeEventService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        isServiceStarted = true
    }

    private sealed class Screen {
        data object Home : Screen()
        data object Settings : Screen()
    }
}
