package com.databridgepro.filemanager.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.Settings

object PermissionUtils {

    fun hasManageStoragePermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            Environment.isExternalStorageManager()
        else true

    fun requestManageStoragePermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${activity.packageName}")
                }
                activity.startActivity(intent)
            } catch (_: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                activity.startActivity(intent)
            }
        }
    }

    fun getAndroidDataTreeUri(): Uri =
        DocumentsContract.buildTreeDocumentUri(
            "com.android.externalstorage.documents",
            "primary:Android/data"
        )

    fun getAndroidDataInitialUri(): Uri =
        DocumentsContract.buildDocumentUri(
            "com.android.externalstorage.documents",
            "primary:Android/data"
        )

    fun hasSafPermission(context: Context, uri: Uri): Boolean =
        context.contentResolver.persistedUriPermissions.any {
            it.uri == uri && it.isReadPermission && it.isWritePermission
        }

    fun persistSafPermission(context: Context, uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: SecurityException) { }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = digitGroups.coerceIn(0, units.lastIndex)
        return "%.1f %s".format(bytes / Math.pow(1024.0, index.toDouble()), units[index])
    }

    fun getMimeTypeIcon(mimeType: String?): String = when {
        mimeType == null -> "folder"
        mimeType.startsWith("image/") -> "image"
        mimeType.startsWith("video/") -> "video"
        mimeType.startsWith("audio/") -> "audio"
        mimeType.startsWith("text/") -> "text"
        mimeType.contains("pdf") -> "pdf"
        mimeType.contains("zip") || mimeType.contains("rar") || mimeType.contains("tar") -> "archive"
        else -> "file"
    }
}
