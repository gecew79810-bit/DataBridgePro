package com.databridgepro.filemanager.presentation.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.databridgepro.filemanager.data.model.BackupItem
import com.databridgepro.filemanager.data.model.FileItem
import com.databridgepro.filemanager.data.repository.BackupRepository
import com.databridgepro.filemanager.data.repository.StorageInfo
import com.databridgepro.filemanager.data.repository.StorageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Success(
        val storageInfo: StorageInfo,
        val recentBackups: List<BackupItem>,
        val recentFiles: List<FileItem> = emptyList(),
        val largeFiles: List<FileItem> = emptyList(),
        val duplicateGroups: List<List<FileItem>> = emptyList()
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val application: Application,
    private val backupRepository: BackupRepository,
    private val storageRepository: StorageRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            try {
                val storageInfo = storageRepository.getStorageInfo()
                backupRepository.getRecentBackups(10)
                    .catch { _uiState.value = HomeUiState.Error(it.message ?: "Unknown error") }
                    .collect { backups ->
                        val current = _uiState.value
                        if (current is HomeUiState.Success) {
                            _uiState.value = current.copy(
                                storageInfo = storageInfo,
                                recentBackups = backups
                            )
                        } else {
                            _uiState.value = HomeUiState.Success(
                                storageInfo = storageInfo,
                                recentBackups = backups
                            )
                            loadRecentFiles()
                        }
                    }
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Failed to load storage info")
            }
        }
    }

    private fun loadRecentFiles() {
        viewModelScope.launch {
            storageRepository.getRecentFiles(20)
                .catch { }
                .collect { recent ->
                    val current = _uiState.value
                    if (current is HomeUiState.Success) {
                        _uiState.value = current.copy(recentFiles = recent)
                    }
                }
        }
    }

    fun loadLargeFiles() {
        viewModelScope.launch {
            storageRepository.getLargeFiles(10, 50)
                .catch { }
                .collect { large ->
                    val current = _uiState.value
                    if (current is HomeUiState.Success) {
                        _uiState.value = current.copy(largeFiles = large)
                    }
                }
        }
    }

    fun loadDuplicates() {
        viewModelScope.launch {
            storageRepository.getDuplicateFiles(30)
                .catch { }
                .collect { dups ->
                    val current = _uiState.value
                    if (current is HomeUiState.Success) {
                        _uiState.value = current.copy(duplicateGroups = dups)
                    }
                }
        }
    }

    fun getQuickFolderPath(folder: String): String =
        storageRepository.getQuickFolderPath(folder)
}
