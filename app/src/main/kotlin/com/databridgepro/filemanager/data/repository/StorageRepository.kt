package com.databridgepro.filemanager.data.repository

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.databridgepro.filemanager.data.model.FileItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

data class StorageInfo(
    val totalBytes: Long = 0L,
    val usedBytes: Long = 0L,
    val freeBytes: Long = 0L
)

@Singleton
class StorageRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val externalStorage: File = Environment.getExternalStorageDirectory()

    fun getStorageInfo(): StorageInfo {
        return try {
            val stat = StatFs(externalStorage.absolutePath)
            val total = stat.blockSizeLong * stat.blockCountLong
            val free = stat.blockSizeLong * stat.availableBlocksLong
            StorageInfo(total, total - free, free)
        } catch (_: Exception) {
            StorageInfo()
        }
    }

    fun listFiles(dirPath: String): Flow<List<FileItem>> = flow {
        val dir = File(dirPath)
        if (!dir.exists() || !dir.isDirectory) {
            emit(emptyList())
            return@flow
        }
        val files = dir.listFiles()?.map { it.toFileItem() } ?: emptyList()
        emit(files)
    }.flowOn(Dispatchers.IO)

    fun getQuickFolderPath(folder: String): String {
        return when (folder) {
            "Downloads" -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
            "DCIM" -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath
            "Documents" -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath
            "WhatsApp" -> File(externalStorage, "WhatsApp").absolutePath
            "Pictures" -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath
            "Music" -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).absolutePath
            "Movies" -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).absolutePath
            "Android/data" -> File(externalStorage, "Android/data").absolutePath
            else -> externalStorage.absolutePath
        }
    }

    fun getRootPath(): String = externalStorage.absolutePath

    fun searchFiles(rootPath: String, query: String, maxResults: Int = 500): Flow<List<FileItem>> = flow {
        val results = mutableListOf<FileItem>()
        fun searchRecursive(dir: File, depth: Int) {
            if (depth > 10 || results.size >= maxResults) return
            try {
                dir.listFiles()?.forEach { file ->
                    if (results.size >= maxResults) return
                    if (file.name.contains(query, ignoreCase = true)) {
                        results.add(file.toFileItem())
                    }
                    if (file.isDirectory && file.canRead()) {
                        searchRecursive(file, depth + 1)
                    }
                }
            } catch (_: Exception) { }
        }
        searchRecursive(File(rootPath), 0)
        emit(results)
    }.flowOn(Dispatchers.IO)

    fun searchByType(type: String, maxResults: Int = 1000): Flow<List<FileItem>> = flow {
        val results = mutableListOf<FileItem>()
        fun scan(dir: File, depth: Int) {
            if (depth > 8 || results.size >= maxResults) return
            try {
                dir.listFiles()?.forEach { file ->
                    if (results.size >= maxResults) return
                    if (file.isFile) {
                        val item = file.toFileItem()
                        val matches = when (type) {
                            "image" -> item.isImage
                            "video" -> item.isVideo
                            "audio" -> item.isAudio
                            "document" -> item.isDocument
                            "apk" -> item.isApk
                            else -> false
                        }
                        if (matches) results.add(item)
                    } else if (file.isDirectory && file.canRead()) {
                        scan(file, depth + 1)
                    }
                }
            } catch (_: Exception) { }
        }
        scan(externalStorage, 0)
        emit(results.sortedByDescending { it.lastModified })
    }.flowOn(Dispatchers.IO)

    fun getRecentFiles(limit: Int = 50): Flow<List<FileItem>> = flow {
        val results = mutableListOf<FileItem>()
        val cutoff = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
        fun scan(dir: File, depth: Int) {
            if (depth > 6 || results.size >= limit * 3) return
            try {
                dir.listFiles()?.forEach { file ->
                    if (file.isFile && file.lastModified() > cutoff) {
                        results.add(file.toFileItem())
                    } else if (file.isDirectory && file.canRead() && !file.name.startsWith(".")) {
                        scan(file, depth + 1)
                    }
                }
            } catch (_: Exception) { }
        }
        scan(externalStorage, 0)
        emit(results.sortedByDescending { it.lastModified }.take(limit))
    }.flowOn(Dispatchers.IO)

    fun getLargeFiles(minSizeMb: Long = 50, limit: Int = 50): Flow<List<FileItem>> = flow {
        val results = mutableListOf<FileItem>()
        val minBytes = minSizeMb * 1024 * 1024
        fun scan(dir: File, depth: Int) {
            if (depth > 8) return
            try {
                dir.listFiles()?.forEach { file ->
                    if (file.isFile && file.length() >= minBytes) {
                        results.add(file.toFileItem())
                    } else if (file.isDirectory && file.canRead()) {
                        scan(file, depth + 1)
                    }
                }
            } catch (_: Exception) { }
        }
        scan(externalStorage, 0)
        emit(results.sortedByDescending { it.size }.take(limit))
    }.flowOn(Dispatchers.IO)

    fun getDuplicateFiles(limit: Int = 100): Flow<List<List<FileItem>>> = flow {
        val sizeMap = mutableMapOf<Long, MutableList<FileItem>>()
        fun scan(dir: File, depth: Int) {
            if (depth > 6) return
            try {
                dir.listFiles()?.forEach { file ->
                    if (file.isFile && file.length() > 1024) {
                        sizeMap.getOrPut(file.length()) { mutableListOf() }.add(file.toFileItem())
                    } else if (file.isDirectory && file.canRead() && !file.name.startsWith(".")) {
                        scan(file, depth + 1)
                    }
                }
            } catch (_: Exception) { }
        }
        scan(externalStorage, 0)
        val duplicates = sizeMap.values
            .filter { it.size > 1 }
            .sortedByDescending { it.first().size }
            .take(limit)
        emit(duplicates)
    }.flowOn(Dispatchers.IO)

    suspend fun copyFile(source: File, destDir: File): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            if (!destDir.exists()) destDir.mkdirs()
            var destFile = File(destDir, source.name)
            var counter = 1
            while (destFile.exists()) {
                val nameWithoutExt = source.nameWithoutExtension
                val ext = source.extension
                destFile = if (ext.isNotEmpty()) {
                    File(destDir, "${nameWithoutExt}_($counter).$ext")
                } else {
                    File(destDir, "${source.name}_($counter)")
                }
                counter++
            }
            if (source.isDirectory) {
                source.copyRecursively(destFile, overwrite = false)
            } else {
                source.copyTo(destFile, overwrite = false)
            }
            destFile
        }
    }

    suspend fun moveFile(source: File, destDir: File): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val result = copyFile(source, destDir).getOrThrow()
            if (source.isDirectory) source.deleteRecursively() else source.delete()
            result
        }
    }

    suspend fun deleteFile(file: File): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val success = if (file.isDirectory) file.deleteRecursively() else file.delete()
            if (!success) throw IOException("Failed to delete: ${file.name}")
        }
    }

    suspend fun renameFile(file: File, newName: String): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val dest = File(file.parentFile, newName)
            if (dest.exists()) throw IOException("A file with that name already exists")
            if (!file.renameTo(dest)) throw IOException("Rename failed")
            dest
        }
    }

    suspend fun createFolder(parentPath: String, name: String): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(parentPath, name)
            if (dir.exists()) throw IOException("Folder already exists")
            if (!dir.mkdirs()) throw IOException("Failed to create folder")
            dir
        }
    }

    fun getFileUri(file: File): Uri {
        return try {
            FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        } catch (_: Exception) {
            Uri.fromFile(file)
        }
    }

    fun readTextFile(path: String): Flow<String> = flow {
        val file = File(path)
        if (file.exists() && file.isFile && file.length() < 5 * 1024 * 1024) {
            emit(file.readText())
        } else {
            emit("")
        }
    }.flowOn(Dispatchers.IO)

    private fun File.toFileItem(): FileItem {
        val ext = extension.lowercase()
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
        return FileItem(
            uri = Uri.fromFile(this),
            name = name,
            size = if (isFile) length() else 0L,
            lastModified = lastModified(),
            mimeType = mime,
            isDirectory = isDirectory,
            parentUri = parentFile?.let { Uri.fromFile(it) },
            path = absolutePath
        )
    }
}
