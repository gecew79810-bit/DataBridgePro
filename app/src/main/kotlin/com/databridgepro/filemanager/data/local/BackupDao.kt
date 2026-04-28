package com.databridgepro.filemanager.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.databridgepro.filemanager.data.model.BackupItem
import kotlinx.coroutines.flow.Flow

@Dao
interface BackupDao {
    @Query("SELECT * FROM backups ORDER BY createdAt DESC")
    fun getAllBackups(): Flow<List<BackupItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBackup(backup: BackupItem)

    @Delete
    suspend fun deleteBackup(backup: BackupItem)

    @Query("SELECT * FROM backups WHERE packageName = :pkg")
    suspend fun getBackupsForPackage(pkg: String): List<BackupItem>

    @Query("SELECT * FROM backups ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentBackups(limit: Int): Flow<List<BackupItem>>
}
