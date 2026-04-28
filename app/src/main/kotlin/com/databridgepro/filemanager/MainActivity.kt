package com.databridgepro.filemanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.databridgepro.filemanager.presentation.navigation.AppNavHost
import com.databridgepro.filemanager.presentation.navigation.DataBridgeProTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DataBridgeProTheme {
                AppNavHost()
            }
        }
    }
}
