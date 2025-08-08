package com.example.shoutdowner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.example.shoutdowner.viewmodel.MainViewModel
import com.example.shoutdowner.viewmodel.UiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel, onOpenSettings: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showConfirm by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val gradient = Brush.linearGradient(
                colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
            )

            Box(
                modifier = Modifier
                    .size(220.dp)
                    .clip(CircleShape)
                    .background(gradient)
                    .clickable { showConfirm = true },
                contentAlignment = Alignment.Center
            ) {
                when (uiState) {
                    is UiState.Loading -> {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                    }
                    else -> {
                        Button(
                            onClick = { showConfirm = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            contentPadding = PaddingValues(24.dp),
                            shape = CircleShape,
                            modifier = Modifier.size(180.dp)
                        ) {
                            Text(text = "Выключить", color = MaterialTheme.colorScheme.onError)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(onClick = { onOpenSettings() }) {
                Text("Настройки")
            }

            // Snackbar host
            SnackbarHost(hostState = snackbarHostState, modifier = Modifier.padding(top = 12.dp))

            if (showConfirm) {
                AlertDialog(
                    onDismissRequest = { showConfirm = false },
                    title = { Text("Подтвердите") },
                    text = { Text("Точно выключить удалённый компьютер?") },
                    confirmButton = {
                        Button(onClick = {
                            showConfirm = false
                            viewModel.shutdownRemote()
                        }) {
                            Text("Да")
                        }
                    },
                    dismissButton = {
                        Button(onClick = { showConfirm = false }) {
                            Text("Отмена")
                        }
                    }
                )
            }

            // React to UI state changes (success / error)
            LaunchedEffect(uiState) {
                when (uiState) {
                    is UiState.Success -> {
                        snackbarHostState.showSnackbar((uiState as UiState.Success).message)
                        // reset state after showing
                        viewModel.resetState()
                    }
                    is UiState.Error -> {
                        snackbarHostState.showSnackbar((uiState as UiState.Error).message)
                        viewModel.resetState()
                    }
                    else -> {
                        // no-op
                    }
                }
            }
        }
    }
}
