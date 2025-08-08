package com.example.shoutdowner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Card
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.example.shoutdowner.data.SshSettings
import com.example.shoutdowner.data.WolSettings
import com.example.shoutdowner.wol.WOLManager
import com.example.shoutdowner.viewmodel.SettingsViewModel
import com.example.shoutdowner.viewmodel.TestResult
import com.example.shoutdowner.viewmodel.SettingsUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settingsState by viewModel.settings.collectAsState()
    val wolSettings by viewModel.wolSettings.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val testResult by viewModel.testResult.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var host by remember { mutableStateOf(settingsState.host) }
    var port by remember { mutableStateOf(settingsState.port.toString()) }
    var username by remember { mutableStateOf(settingsState.username) }
    var usePassword by remember { mutableStateOf(settingsState.usePassword) }
    var password by remember { mutableStateOf(settingsState.password) }
    var privateKey by remember { mutableStateOf(settingsState.privateKeyPem) }

    var wolMac by remember { mutableStateOf(wolSettings.mac) }
    var wolBroadcast by remember { mutableStateOf(wolSettings.broadcast) }
    var wolPort by remember { mutableStateOf(wolSettings.port.toString()) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        // скроллим форму, чтобы на маленьких экранах всё было доступно
        val scrollState = rememberScrollState()
        var showPassword by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Text(text = "Настройки SSH", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = { Text("Хост / IP") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = port,
                        onValueChange = { new -> port = new.filter { ch -> ch.isDigit() } },
                        label = { Text("Порт") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Пользователь") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Аутентификация паролем")
                        Spacer(modifier = Modifier.weight(1f))
                        Switch(checked = usePassword, onCheckedChange = { usePassword = it })
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (usePassword) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Пароль") },
                            singleLine = true,
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                Text(
                                    if (showPassword) "Скрыть" else "Показать",
                                    modifier = Modifier
                                        .clickable { showPassword = !showPassword }
                                        .padding(8.dp)
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        OutlinedTextField(
                            value = privateKey,
                            onValueChange = { privateKey = it },
                            label = { Text("Приватный ключ (PEM)") },
                            modifier = Modifier
                                .height(140.dp)
                                .fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(text = "Настройки WOL", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = wolMac,
                        onValueChange = { wolMac = it },
                        label = { Text("MAC-адрес (AA:BB:CC:DD:EE:FF)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = wolBroadcast,
                        onValueChange = { wolBroadcast = it },
                        label = { Text("Broadcast (напр. 192.168.8.255)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = wolPort,
                        onValueChange = { new -> wolPort = new.filter { ch -> ch.isDigit() } },
                        label = { Text("Порт (по умолчанию 9)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val coroutineScope = rememberCoroutineScope()

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    // save WOL settings
                    val w = com.example.shoutdowner.data.WolSettings(
                        mac = wolMac,
                        broadcast = wolBroadcast,
                        port = wolPort.toIntOrNull() ?: 9
                    )
                    viewModel.updateWolSettings(w)
                    viewModel.saveWol()
                }, modifier = Modifier.weight(1f)) {
                    Text("Сохранить WOL")
                }

                Button(onClick = {
                    // send magic packet and show snackbar result
                    val mac = wolMac
                    val broadcast = wolBroadcast
                    val portInt = wolPort.toIntOrNull() ?: 9
                    coroutineScope.launch {
                        val sent = withContext(Dispatchers.IO) {
                            com.example.shoutdowner.wol.WOLManager.sendWake(mac, if (broadcast.isBlank()) null else broadcast, portInt)
                        }
                        if (sent) {
                            snackbarHostState.showSnackbar("Magic packet отправлен")
                        } else {
                            snackbarHostState.showSnackbar("Не удалось отправить Magic packet")
                        }
                    }
                }, modifier = Modifier.weight(1f)) {
                    Text("Отправить Wake")
                }
                Button(onClick = {
                    // save
                    val s = SshSettings(
                        host = host,
                        port = port.toIntOrNull() ?: 22,
                        username = username,
                        usePassword = usePassword,
                        password = password,
                        privateKeyPem = privateKey
                    )
                    viewModel.updateSettings(s)
                    viewModel.save()
                }, modifier = Modifier.weight(1f)) {
                    Text("Сохранить")
                }

                TextButton(onClick = {
                    val s = SshSettings(
                        host = host,
                        port = port.toIntOrNull() ?: 22,
                        username = username,
                        usePassword = usePassword,
                        password = password,
                        privateKeyPem = privateKey
                    )
                    viewModel.updateSettings(s)
                    viewModel.testConnection()
                }, modifier = Modifier.weight(1f)) {
                    Text("Тест соединения")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // показ статуса
            when (uiState) {
                is SettingsUiState.Saved -> LaunchedEffect(uiState) {
                    snackbarHostState.showSnackbar((uiState as SettingsUiState.Saved).message)
                    viewModel.load()
                }
                is SettingsUiState.Error -> LaunchedEffect(uiState) {
                    snackbarHostState.showSnackbar((uiState as SettingsUiState.Error).message)
                }
                else -> {}
            }

            when (testResult) {
                is TestResult.Testing -> Text("Тестируем...", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
                is TestResult.Success -> Text("Успешно: ${(testResult as TestResult.Success).message}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.primary)
                is TestResult.Failure -> Text("Ошибка: ${(testResult as TestResult.Failure).message}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.error)
                else -> {}
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(onClick = { onBack() }, modifier = Modifier.fillMaxWidth()) {
                Text("Назад")
            }

            // Snackbar внизу
            Spacer(modifier = Modifier.height(8.dp))
            SnackbarHost(hostState = snackbarHostState)
        }
    }
}
