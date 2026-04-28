package com.databridgepro.filemanager.data.repository

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.databridgepro.filemanager.data.model.FileItem
import kotlinx.coroutines.flow.Flow

interface FileRepository {
    fun getFiles(treeUri: Uri): Flow<List<FileItem>>
    fun searchFiles(treeUri: Uri, query: String): Flow<List<FileItem>>
    suspend fun copyFile(source: DocumentFile, destFolder: DocumentFile): Result<Unit>
    suspend fun moveFile(source: DocumentFile, destFolder: DocumentFile): Result<Unit>
    suspend fun deleteFile(file: DocumentFile): Result<Unit>
    suspend fun renameFile(file: DocumentFile, newName: String): Result<Unit>
    fun copyWithProgress(source: DocumentFile, dest: DocumentFile): Flow<Int>
    suspend fun createFolder(parentUri: Uri, folderName: String): Result<DocumentFile>
}
