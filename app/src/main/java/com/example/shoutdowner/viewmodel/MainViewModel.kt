package com.example.shoutdowner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shoutdowner.data.SettingsRepository
import com.example.shoutdowner.ssh.ExecutionResult
import com.example.shoutdowner.ssh.SSHManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    data class Success(val message: String) : UiState()
    data class Error(val message: String) : UiState()
}

class MainViewModel(private val settingsRepo: SettingsRepository) : ViewModel() {

    private val sshManager = SSHManager()

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    fun shutdownRemote() {
        val settings = settingsRepo.getSettings()
        if (settings.host.isBlank() || settings.username.isBlank()) {
            _uiState.value = UiState.Error("Заполните настройки в разделе Settings")
            return
        }

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            // prefer using sudo with full path
            val cmd = "sudo /sbin/shutdown -h now"
            when (val res = sshManager.executeCommand(settings, cmd)) {
                is ExecutionResult.Success -> _uiState.value = UiState.Success("Команда отправлена. Вывод: ${res.output}")
                is ExecutionResult.Error -> _uiState.value = UiState.Error(res.message)
            }
        }
    }

    fun resetState() {
        _uiState.value = UiState.Idle
    }
}
