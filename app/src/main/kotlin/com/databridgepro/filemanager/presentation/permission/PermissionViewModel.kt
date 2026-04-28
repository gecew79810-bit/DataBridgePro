package com.databridgepro.filemanager.presentation.permission

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import com.databridgepro.filemanager.data.repository.SettingsRepository
import com.databridgepro.filemanager.util.PermissionUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class PermissionViewModel @Inject constructor(
    private val application: Application,
    private val settingsRepository: SettingsRepository
) : AndroidViewModel(application) {

    private val _manageStorageGranted = MutableStateFlow(PermissionUtils.hasManageStoragePermission())
    val manageStorageGranted: StateFlow<Boolean> = _manageStorageGranted.asStateFlow()

    private val _safGranted = MutableStateFlow(
        PermissionUtils.hasSafPermission(application, PermissionUtils.getAndroidDataTreeUri())
    )
    val safGranted: StateFlow<Boolean> = _safGranted.asStateFlow()

    fun refreshPermissions() {
        _manageStorageGranted.value = PermissionUtils.hasManageStoragePermission()
        _safGranted.value = PermissionUtils.hasSafPermission(
            application, PermissionUtils.getAndroidDataTreeUri()
        )
    }

    suspend fun onSafUriGranted(uri: Uri) {
        PermissionUtils.persistSafPermission(application, uri)
        settingsRepository.setSafUri(uri.toString())
        _safGranted.value = true
    }
}
