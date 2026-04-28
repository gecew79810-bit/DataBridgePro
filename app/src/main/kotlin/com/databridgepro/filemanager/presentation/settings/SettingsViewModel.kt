package com.databridgepro.filemanager.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.databridgepro.filemanager.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val theme: String = "system",
    val dynamicColor: Boolean = true,
    val biometricLock: Boolean = false,
    val autoBackup: Boolean = false,
    val backupSchedule: String = "weekly",
    val backupFolder: String = ""
)

sealed class SettingsUiState {
    data object Loading : SettingsUiState()
    data class Success(val settings: SettingsState) : SettingsUiState()
    data class Error(val message: String) : SettingsUiState()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            try {
                settingsRepository.theme.collect { theme ->
                    val current = (_uiState.value as? SettingsUiState.Success)?.settings ?: SettingsState()
                    _uiState.value = SettingsUiState.Success(current.copy(theme = theme))
                }
            } catch (_: Exception) { }
        }
        viewModelScope.launch {
            try {
                settingsRepository.dynamicColor.collect { dc ->
                    val current = (_uiState.value as? SettingsUiState.Success)?.settings ?: SettingsState()
                    _uiState.value = SettingsUiState.Success(current.copy(dynamicColor = dc))
                }
            } catch (_: Exception) { }
        }
        viewModelScope.launch {
            try {
                settingsRepository.biometricLock.collect { bio ->
                    val current = (_uiState.value as? SettingsUiState.Success)?.settings ?: SettingsState()
                    _uiState.value = SettingsUiState.Success(current.copy(biometricLock = bio))
                }
            } catch (_: Exception) { }
        }
        viewModelScope.launch {
            try {
                settingsRepository.autoBackup.collect { auto ->
                    val current = (_uiState.value as? SettingsUiState.Success)?.settings ?: SettingsState()
                    _uiState.value = SettingsUiState.Success(current.copy(autoBackup = auto))
                }
            } catch (_: Exception) { }
        }
        viewModelScope.launch {
            try {
                settingsRepository.backupSchedule.collect { schedule ->
                    val current = (_uiState.value as? SettingsUiState.Success)?.settings ?: SettingsState()
                    _uiState.value = SettingsUiState.Success(current.copy(backupSchedule = schedule))
                }
            } catch (_: Exception) { }
        }
        viewModelScope.launch {
            try {
                settingsRepository.backupFolder.collect { folder ->
                    val current = (_uiState.value as? SettingsUiState.Success)?.settings ?: SettingsState()
                    _uiState.value = SettingsUiState.Success(current.copy(backupFolder = folder))
                }
            } catch (_: Exception) { }
        }
    }

    fun setTheme(value: String) {
        viewModelScope.launch { settingsRepository.setTheme(value) }
    }

    fun setDynamicColor(value: Boolean) {
        viewModelScope.launch { settingsRepository.setDynamicColor(value) }
    }

    fun setBiometricLock(value: Boolean) {
        viewModelScope.launch { settingsRepository.setBiometricLock(value) }
    }

    fun setAutoBackup(value: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoBackup(value) }
    }

    fun setBackupSchedule(value: String) {
        viewModelScope.launch { settingsRepository.setBackupSchedule(value) }
    }

    fun setBackupFolder(value: String) {
        viewModelScope.launch { settingsRepository.setBackupFolder(value) }
    }
}
