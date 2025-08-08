package com.example.shoutdowner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import com.example.shoutdowner.data.SettingsRepository
import com.example.shoutdowner.ui.MainScreen
import com.example.shoutdowner.ui.SettingsScreen
import com.example.shoutdowner.ui.theme.ShoutdownerTheme
import com.example.shoutdowner.viewmodel.MainViewModelFactory
import com.example.shoutdowner.viewmodel.SettingsViewModelFactory
import com.example.shoutdowner.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val settingsRepo = SettingsRepository(this)
        val factory = MainViewModelFactory(settingsRepo)
        val viewModel = ViewModelProvider(this, factory)
            .get(com.example.shoutdowner.viewmodel.MainViewModel::class.java)

        setContent {
            ShoutdownerTheme {
                var showSettings by remember { mutableStateOf(false) }

                if (showSettings) {
                    val settingsFactory = SettingsViewModelFactory(settingsRepo)
                    val settingsVm = ViewModelProvider(this, settingsFactory)
                        .get(SettingsViewModel::class.java)
                    SettingsScreen(viewModel = settingsVm) { showSettings = false }
                } else {
                    MainScreen(viewModel = viewModel) { showSettings = true }
                }
            }
        }
    }
}
