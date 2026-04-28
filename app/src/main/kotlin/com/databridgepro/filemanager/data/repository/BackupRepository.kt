package com.databridgepro.filemanager.data.repository

import com.databridgepro.filemanager.data.local.BackupDao
import com.databridgepro.filemanager.data.model.BackupItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepository @Inject constructor(
    private val backupDao: BackupDao
) {
    fun getAllBackups(): Flow<List<BackupItem>> = backupDao.getAllBackups()

    fun getRecentBackups(limit: Int = 10): Flow<List<BackupItem>> =
        backupDao.getRecentBackups(limit)

    suspend fun insertBackup(backup: BackupItem) = backupDao.insertBackup(backup)

    suspend fun deleteBackup(backup: BackupItem) = backupDao.deleteBackup(backup)

    suspend fun getBackupsForPackage(pkg: String): List<BackupItem> =
        backupDao.getBackupsForPackage(pkg)
}
