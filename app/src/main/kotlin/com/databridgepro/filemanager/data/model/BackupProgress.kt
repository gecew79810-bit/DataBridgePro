package com.databridgepro.filemanager.data.model

data class BackupProgress(
    val currentFile: String = "",
    val totalFiles: Int = 0,
    val completedFiles: Int = 0,
    val percentage: Int = 0,
    val isRunning: Boolean = false
)
