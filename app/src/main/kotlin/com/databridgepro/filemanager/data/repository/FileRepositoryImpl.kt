package com.databridgepro.filemanager.data.repository

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.databridgepro.filemanager.data.model.FileItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : FileRepository {

    override fun getFiles(treeUri: Uri): Flow<List<FileItem>> = flow {
        val docFile = DocumentFile.fromTreeUri(context, treeUri)
        val files = docFile?.listFiles()?.map { it.toFileItem() } ?: emptyList()
        emit(files.sortedWith(compareByDescending<FileItem> { it.isDirectory }.thenBy { it.name.lowercase() }))
    }.flowOn(Dispatchers.IO)

    override fun searchFiles(treeUri: Uri, query: String): Flow<List<FileItem>> = flow {
        val results = mutableListOf<FileItem>()
        fun searchRecursive(folder: DocumentFile) {
            try {
                folder.listFiles().forEach { file ->
                    if (file.name?.contains(query, ignoreCase = true) == true) {
                        results.add(file.toFileItem())
                    }
                    if (file.isDirectory) searchRecursive(file)
                }
            } catch (_: Exception) { }
        }
        DocumentFile.fromTreeUri(context, treeUri)?.let { searchRecursive(it) }
        emit(results)
    }.flowOn(Dispatchers.IO)

    override suspend fun copyFile(
        source: DocumentFile, destFolder: DocumentFile
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val newFile = destFolder.createFile(
                source.type ?: "application/octet-stream", source.name ?: "file"
            ) ?: throw IOException("Cannot create destination file")
            context.contentResolver.openInputStream(source.uri)?.use { input ->
                context.contentResolver.openOutputStream(newFile.uri)?.use { output ->
                    input.copyTo(output)
                }
            }
            Unit
        }
    }

    override suspend fun moveFile(
        source: DocumentFile, destFolder: DocumentFile
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            copyFile(source, destFolder).getOrThrow()
            source.delete()
            Unit
        }
    }

    override suspend fun deleteFile(file: DocumentFile): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (!file.delete()) throw IOException("Delete failed: ${file.name}")
            }
        }

    override suspend fun renameFile(
        file: DocumentFile, newName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (!file.renameTo(newName)) throw IOException("Rename failed")
        }
    }

    override fun copyWithProgress(
        source: DocumentFile, dest: DocumentFile
    ): Flow<Int> = flow {
        val totalSize = source.length()
        if (totalSize <= 0L) {
            emit(100)
            return@flow
        }
        var copied = 0L
        val buffer = ByteArray(8192)
        context.contentResolver.openInputStream(source.uri)?.use { input ->
            context.contentResolver.openOutputStream(dest.uri)?.use { output ->
                var bytes: Int
                while (input.read(buffer).also { bytes = it } != -1) {
                    output.write(buffer, 0, bytes)
                    copied += bytes
                    emit(((copied * 100) / totalSize).toInt())
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun createFolder(parentUri: Uri, folderName: String): Result<DocumentFile> =
        withContext(Dispatchers.IO) {
            runCatching {
                val parent = DocumentFile.fromTreeUri(context, parentUri)
                    ?: throw IOException("Cannot open parent folder")
                parent.createDirectory(folderName)
                    ?: throw IOException("Cannot create folder: $folderName")
            }
        }

    private fun DocumentFile.toFileItem() = FileItem(
        uri = uri,
        name = name ?: "Unknown",
        size = length(),
        lastModified = lastModified(),
        mimeType = type,
        isDirectory = isDirectory,
        parentUri = parentFile?.uri
    )
}
