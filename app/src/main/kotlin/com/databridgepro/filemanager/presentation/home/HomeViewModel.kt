package com.databridgepro.filemanager.presentation.home

import android.app.Application
import android.app.usage.StorageStatsManager
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.databridgepro.filemanager.data.model.BackupItem
import com.databridgepro.filemanager.data.repository.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StorageInfo(
    val totalBytes: Long = 0L,
    val usedBytes: Long = 0L,
    val freeBytes: Long = 0L
)

sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Success(
        val storageInfo: StorageInfo,
        val recentBackups: List<BackupItem>
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val application: Application,
    private val backupRepository: BackupRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val storageInfo = getStorageInfo()
                backupRepository.getRecentBackups(10)
                    .catch { _uiState.value = HomeUiState.Error(it.message ?: "Unknown error") }
                    .collect { backups ->
                        _uiState.value = HomeUiState.Success(
                            storageInfo = storageInfo,
                            recentBackups = backups
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Failed to load storage info")
            }
        }
    }

    private fun getStorageInfo(): StorageInfo {
        return try {
            val path = Environment.getExternalStorageDirectory()
            val stat = StatFs(path.absolutePath)
            val totalBytes = stat.blockSizeLong * stat.blockCountLong
            val freeBytes = stat.blockSizeLong * stat.availableBlocksLong
            StorageInfo(
                totalBytes = totalBytes,
                usedBytes = totalBytes - freeBytes,
                freeBytes = freeBytes
            )
        } catch (_: Exception) {
            StorageInfo()
        }
    }
}
