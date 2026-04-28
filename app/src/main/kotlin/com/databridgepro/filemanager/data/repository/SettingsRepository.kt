package com.databridgepro.filemanager.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val THEME_KEY = stringPreferencesKey("theme")
        val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color")
        val BIOMETRIC_KEY = booleanPreferencesKey("biometric_lock")
        val AUTO_BACKUP_KEY = booleanPreferencesKey("auto_backup")
        val BACKUP_SCHEDULE_KEY = stringPreferencesKey("backup_schedule")
        val BACKUP_FOLDER_KEY = stringPreferencesKey("backup_folder")
        val SAF_URI_KEY = stringPreferencesKey("saf_uri")
    }

    val theme: Flow<String> = context.dataStore.data.map { it[THEME_KEY] ?: "dark" }
    val dynamicColor: Flow<Boolean> = context.dataStore.data.map { it[DYNAMIC_COLOR_KEY] ?: true }
    val biometricLock: Flow<Boolean> = context.dataStore.data.map { it[BIOMETRIC_KEY] ?: false }
    val autoBackup: Flow<Boolean> = context.dataStore.data.map { it[AUTO_BACKUP_KEY] ?: false }
    val backupSchedule: Flow<String> = context.dataStore.data.map { it[BACKUP_SCHEDULE_KEY] ?: "weekly" }
    val backupFolder: Flow<String> = context.dataStore.data.map { it[BACKUP_FOLDER_KEY] ?: "" }
    val safUri: Flow<String> = context.dataStore.data.map { it[SAF_URI_KEY] ?: "" }

    suspend fun setTheme(value: String) { context.dataStore.edit { it[THEME_KEY] = value } }
    suspend fun setDynamicColor(value: Boolean) { context.dataStore.edit { it[DYNAMIC_COLOR_KEY] = value } }
    suspend fun setBiometricLock(value: Boolean) { context.dataStore.edit { it[BIOMETRIC_KEY] = value } }
    suspend fun setAutoBackup(value: Boolean) { context.dataStore.edit { it[AUTO_BACKUP_KEY] = value } }
    suspend fun setBackupSchedule(value: String) { context.dataStore.edit { it[BACKUP_SCHEDULE_KEY] = value } }
    suspend fun setBackupFolder(value: String) { context.dataStore.edit { it[BACKUP_FOLDER_KEY] = value } }
    suspend fun setSafUri(value: String) { context.dataStore.edit { it[SAF_URI_KEY] = value } }
}
