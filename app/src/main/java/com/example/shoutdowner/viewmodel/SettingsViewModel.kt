package com.example.shoutdowner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shoutdowner.data.SshSettings
import com.example.shoutdowner.data.SettingsRepository
import com.example.shoutdowner.data.WolSettings
import com.example.shoutdowner.ssh.ExecutionResult
import com.example.shoutdowner.ssh.SSHManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class SettingsUiState {
    object Idle : SettingsUiState()
    object Saving : SettingsUiState()
    data class Saved(val message: String = "Saved") : SettingsUiState()
    data class Error(val message: String) : SettingsUiState()
}

sealed class TestResult {
    object NotTested : TestResult()
    object Testing : TestResult()
    data class Success(val message: String) : TestResult()
    data class Failure(val message: String) : TestResult()
}

class SettingsViewModel(private val repo: SettingsRepository) : ViewModel() {

    private val sshManager = SSHManager()

    private val _settings = MutableStateFlow<SshSettings>(repo.getSettings())
    val settings: StateFlow<SshSettings> = _settings

    private val _wolSettings = MutableStateFlow<WolSettings>(repo.getWolSettings())
    val wolSettings: StateFlow<WolSettings> = _wolSettings

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Idle)
    val uiState: StateFlow<SettingsUiState> = _uiState

    private val _testResult = MutableStateFlow<TestResult>(TestResult.NotTested)
    val testResult: StateFlow<TestResult> = _testResult

    fun load() {
        _settings.value = repo.getSettings()
        _wolSettings.value = repo.getWolSettings()
    }

    fun updateSettings(new: SshSettings) {
        _settings.value = new
    }

    fun updateWolSettings(new: WolSettings) {
        _wolSettings.value = new
    }

    fun save() {
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Saving
            try {
                repo.saveSettings(_settings.value)
                _uiState.value = SettingsUiState.Saved("Настройки сохранены")
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error("Ошибка сохранения: ${e.message}")
            }
        }
    }

    fun saveWol() {
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Saving
            try {
                repo.saveWolSettings(_wolSettings.value)
                _uiState.value = SettingsUiState.Saved("WOL настройки сохранены")
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error("Ошибка сохранения WOL: ${e.message}")
            }
        }
    }

    /**
     * Тестовое подключение — выполняем простую команду `echo ok`.
     */
    fun testConnection(timeoutMs: Int = 10_000) {
        viewModelScope.launch {
            _testResult.value = TestResult.Testing
            try {
                val s = _settings.value
                val cmd = "echo OK"
                val res = sshManager.executeCommand(s, cmd, timeoutMs)
                when (res) {
                    is ExecutionResult.Success -> _testResult.value = TestResult.Success("Успех: ${res.output}")
                    is ExecutionResult.Error -> _testResult.value = TestResult.Failure("Ошибка: ${res.message}")
                }
            } catch (e: Exception) {
                _testResult.value = TestResult.Failure("Ошибка: ${e.message}")
            }
        }
    }
}
