package com.databridgepro.filemanager.data.model

data class AppBackupInfo(
    val appName: String,
    val packageName: String,
    val dataSize: Long,
    val iconUri: String?,
    val isSelected: Boolean = false
)
