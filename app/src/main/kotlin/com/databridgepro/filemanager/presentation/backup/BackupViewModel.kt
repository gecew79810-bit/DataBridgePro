package com.databridgepro.filemanager.presentation.backup

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.databridgepro.filemanager.data.model.AppBackupInfo
import com.databridgepro.filemanager.data.model.BackupItem
import com.databridgepro.filemanager.data.model.BackupProgress
import com.databridgepro.filemanager.data.repository.BackupRepository
import com.databridgepro.filemanager.util.ZipUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class BackupUiState {
    data object Loading : BackupUiState()
    data class Success(
        val apps: List<AppBackupInfo>,
        val backups: List<BackupItem>
    ) : BackupUiState()
    data class Error(val message: String) : BackupUiState()
}

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val application: Application,
    private val backupRepository: BackupRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<BackupUiState>(BackupUiState.Loading)
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    private val _selectedApps = MutableStateFlow<Set<String>>(emptySet())
    val selectedApps: StateFlow<Set<String>> = _selectedApps.asStateFlow()

    private val _backupProgress = MutableStateFlow(BackupProgress())
    val backupProgress: StateFlow<BackupProgress> = _backupProgress.asStateFlow()

    init {
        loadApps()
    }

    fun loadApps() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.value = BackupUiState.Loading
                val pm = application.packageManager
                val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                    .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
                    .map { appInfo ->
                        val appName = try {
                            pm.getApplicationLabel(appInfo).toString()
                        } catch (_: Exception) {
                            appInfo.packageName
                        }
                        val dataUri = DocumentsContract.buildTreeDocumentUri(
                            "com.android.externalstorage.documents",
                            "primary:Android/data/${appInfo.packageName}"
                        )
                        val dataSize = try {
                            ZipUtils.calculateFolderSize(application, dataUri)
                        } catch (_: Exception) { 0L }

                        AppBackupInfo(
                            appName = appName,
                            packageName = appInfo.packageName,
                            dataSize = dataSize,
                            iconUri = null
                        )
                    }
                    .filter { it.dataSize > 0 }
                    .sortedByDescending { it.dataSize }

                backupRepository.getAllBackups().collect { backups ->
                    _uiState.value = BackupUiState.Success(apps = installedApps, backups = backups)
                }
            } catch (e: Exception) {
                _uiState.value = BackupUiState.Error(e.message ?: "Failed to load apps")
            }
        }
    }

    fun toggleAppSelection(packageName: String) {
        val current = _selectedApps.value.toMutableSet()
        if (current.contains(packageName)) current.remove(packageName) else current.add(packageName)
        _selectedApps.value = current
    }

    fun backupSelected(destTreeUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _uiState.value
            if (state !is BackupUiState.Success) return@launch

            val appsToBackup = state.apps.filter { _selectedApps.value.contains(it.packageName) }
            if (appsToBackup.isEmpty()) return@launch

            _backupProgress.value = BackupProgress(
                isRunning = true,
                totalFiles = appsToBackup.size
            )

            val destDoc = DocumentFile.fromTreeUri(application, destTreeUri) ?: return@launch

            appsToBackup.forEachIndexed { index, app ->
                try {
                    _backupProgress.value = _backupProgress.value.copy(
                        currentFile = app.appName,
                        completedFiles = index
                    )

                    val sourceUri = DocumentsContract.buildTreeDocumentUri(
                        "com.android.externalstorage.documents",
                        "primary:Android/data/${app.packageName}"
                    )

                    val zipFile = destDoc.createFile(
                        "application/zip",
                        "${app.packageName}_${System.currentTimeMillis()}.zip"
                    ) ?: continue

                    ZipUtils.zipFolder(application, sourceUri, zipFile.uri) { progress ->
                        _backupProgress.value = _backupProgress.value.copy(
                            percentage = ((index * 100 + progress) / appsToBackup.size)
                        )
                    }

                    backupRepository.insertBackup(
                        BackupItem(
                            appName = app.appName,
                            packageName = app.packageName,
                            backupPath = zipFile.uri.toString(),
                            sizeBytes = app.dataSize,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                } catch (_: Exception) { }
            }

            _backupProgress.value = BackupProgress(
                isRunning = false,
                completedFiles = appsToBackup.size,
                totalFiles = appsToBackup.size,
                percentage = 100
            )
            _selectedApps.value = emptySet()
        }
    }

    fun deleteBackup(backup: BackupItem) {
        viewModelScope.launch {
            try {
                val doc = DocumentFile.fromSingleUri(application, Uri.parse(backup.backupPath))
                doc?.delete()
            } catch (_: Exception) { }
            backupRepository.deleteBackup(backup)
        }
    }
}
