package com.databridgepro.filemanager.data.model

import android.net.Uri

data class FileItem(
    val uri: Uri,
    val name: String,
    val size: Long,
    val lastModified: Long,
    val mimeType: String?,
    val isDirectory: Boolean,
    val parentUri: Uri?
)
