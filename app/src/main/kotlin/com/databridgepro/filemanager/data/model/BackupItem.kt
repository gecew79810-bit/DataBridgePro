package com.databridgepro.filemanager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "backups")
data class BackupItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val appName: String,
    val packageName: String,
    val backupPath: String,
    val sizeBytes: Long,
    val createdAt: Long,
    val isRestored: Boolean = false
)
