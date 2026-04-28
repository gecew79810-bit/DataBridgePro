package com.databridgepro.filemanager.util

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ZipUtils {

    suspend fun zipFolder(
        context: Context,
        sourceUri: Uri,
        destUri: Uri,
        onProgress: (Int) -> Unit
    ) = withContext(Dispatchers.IO) {
        val sourceDoc = DocumentFile.fromTreeUri(context, sourceUri) ?: return@withContext
        context.contentResolver.openOutputStream(destUri)?.use { outStream ->
            ZipOutputStream(BufferedOutputStream(outStream)).use { zip ->
                val allFiles = mutableListOf<Pair<DocumentFile, String>>()
                fun collectFiles(doc: DocumentFile, path: String) {
                    doc.listFiles().forEach { file ->
                        if (file.isDirectory) {
                            collectFiles(file, "$path${file.name}/")
                        } else {
                            allFiles.add(file to "$path${file.name}")
                        }
                    }
                }
                collectFiles(sourceDoc, "")
                if (allFiles.isEmpty()) {
                    onProgress(100)
                    return@use
                }
                allFiles.forEachIndexed { index, (file, path) ->
                    try {
                        zip.putNextEntry(ZipEntry(path))
                        context.contentResolver.openInputStream(file.uri)?.use { it.copyTo(zip) }
                        zip.closeEntry()
                    } catch (_: Exception) { }
                    onProgress((index + 1) * 100 / allFiles.size)
                }
            }
        }
    }

    suspend fun calculateFolderSize(
        context: Context,
        uri: Uri
    ): Long = withContext(Dispatchers.IO) {
        var totalSize = 0L
        fun calculate(doc: DocumentFile) {
            doc.listFiles().forEach { file ->
                if (file.isDirectory) calculate(file)
                else totalSize += file.length()
            }
        }
        try {
            DocumentFile.fromTreeUri(context, uri)?.let { calculate(it) }
        } catch (_: Exception) { }
        totalSize
    }
}
