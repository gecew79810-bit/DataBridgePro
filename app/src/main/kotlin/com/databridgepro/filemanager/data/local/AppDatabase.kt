package com.databridgepro.filemanager.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.databridgepro.filemanager.data.model.BackupItem

@Database(entities = [BackupItem::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun backupDao(): BackupDao
}
