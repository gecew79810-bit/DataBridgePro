package com.databridgepro.filemanager.domain.usecase

import android.content.Context
import android.net.Uri
import com.databridgepro.filemanager.data.model.BackupItem
import com.databridgepro.filemanager.data.repository.BackupRepository
import com.databridgepro.filemanager.util.ZipUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class BackupUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupRepository: BackupRepository
) {
    suspend fun backupApp(
        appName: String,
        packageName: String,
        sourceUri: Uri,
        destUri: Uri,
        onProgress: (Int) -> Unit
    ): Result<Unit> = runCatching {
        ZipUtils.zipFolder(context, sourceUri, destUri, onProgress)
        val sizeBytes = ZipUtils.calculateFolderSize(context, sourceUri)
        backupRepository.insertBackup(
            BackupItem(
                appName = appName,
                packageName = packageName,
                backupPath = destUri.toString(),
                sizeBytes = sizeBytes,
                createdAt = System.currentTimeMillis()
            )
        )
    }
}
