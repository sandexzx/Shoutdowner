package com.example.shoutdowner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.ui.draw.scale
import com.example.shoutdowner.viewmodel.MainViewModel
import com.example.shoutdowner.viewmodel.UiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Simple drawable power icon implemented with Canvas — не зависит от внешних иконпаков.
 * Рисует дугу и вертикальную линию, похожую на значок питания.
 */
@Composable
fun PowerIcon(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onError, stroke: androidx.compose.ui.unit.Dp = 2.5.dp) {
    Canvas(modifier = modifier) {
        val strokePx = stroke.toPx()
        // draw arc (almost full circle, with a gap)
        drawArc(
            color = color,
            startAngle = -135f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = Offset(strokePx / 2f, strokePx / 2f),
            size = Size(size.width - strokePx, size.height - strokePx),
            style = Stroke(width = strokePx, cap = StrokeCap.Round)
        )

        // draw the vertical line at the top center
        drawLine(
            color = color,
            start = Offset(x = size.width / 2f, y = strokePx / 2f),
            end = Offset(x = size.width / 2f, y = size.height * 0.36f),
            strokeWidth = strokePx,
            cap = StrokeCap.Round
        )
    }
}

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
                .padding(top = 28.dp)
        ) {
            // Top bar: app title + settings
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Shoutdowner",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                OutlinedButton(
                    onClick = onOpenSettings,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Настройки")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Main action circle
            val gradient = Brush.linearGradient(
                colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(250.dp)
                        .shadow(12.dp, CircleShape)
                        .clip(CircleShape)
                        .background(gradient),
                    contentAlignment = Alignment.Center
                ) {
                    when (uiState) {
                        is UiState.Loading -> {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 4.dp,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        else -> {
                            // custom circular button with icon + micro-animation (scale) and ripple
                            val btnInteractionSource = remember { MutableInteractionSource() }
                            val pressed by btnInteractionSource.collectIsPressedAsState()
                            val scale by animateFloatAsState(if (pressed) 0.96f else 1f)

                            Box(
                                modifier = Modifier
                                    .size(180.dp)
                                    .scale(scale)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error)
                                    .clickable(
                                        interactionSource = btnInteractionSource,
                                        indication = LocalIndication.current
                                    ) { showConfirm = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    PowerIcon(
                                        modifier = Modifier.size(36.dp),
                                        color = MaterialTheme.colorScheme.onError,
                                        stroke = 3.dp
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = "Выключить",
                                        color = MaterialTheme.colorScheme.onError,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Status / hint text
            val statusText = when (uiState) {
                is UiState.Success -> (uiState as UiState.Success).message
                is UiState.Error -> (uiState as UiState.Error).message
                is UiState.Loading -> "Выполняется..."
                else -> "Готов к работе"
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 36.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Snackbar host
            SnackbarHost(hostState = snackbarHostState, modifier = Modifier.padding(16.dp))

            // Confirmation dialog
            if (showConfirm) {
                AlertDialog(
                    onDismissRequest = { showConfirm = false },
                    title = { Text("Подтвердите", fontWeight = FontWeight.SemiBold) },
                    text = { Text("Точно выключить удалённый компьютер? Это действие нельзя отменить.") },
                    confirmButton = {
                        Button(onClick = {
                            showConfirm = false
                            // run shutdown in background to avoid blocking UI
                            CoroutineScope(Dispatchers.IO).launch {
                                viewModel.shutdownRemote()
                            }
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
