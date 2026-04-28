package com.databridgepro.filemanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.databridgepro.filemanager.data.repository.SettingsRepository
import com.databridgepro.filemanager.presentation.navigation.AppNavHost
import com.databridgepro.filemanager.presentation.navigation.DataBridgeProTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by settingsRepository.theme.collectAsState(initial = "system")
            val dynamicColor by settingsRepository.dynamicColor.collectAsState(initial = true)

            DataBridgeProTheme(
                themeMode = themeMode,
                dynamicColor = dynamicColor
            ) {
                AppNavHost()
            }
        }
    }
}
