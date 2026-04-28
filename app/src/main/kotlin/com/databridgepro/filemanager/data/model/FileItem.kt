package com.databridgepro.filemanager.data.model

import android.net.Uri

data class FileItem(
    val uri: Uri,
    val name: String,
    val size: Long,
    val lastModified: Long,
    val mimeType: String?,
    val isDirectory: Boolean,
    val parentUri: Uri?,
    val path: String = "",
    val extension: String = name.substringAfterLast('.', "").lowercase()
) {
    val isImage: Boolean get() = mimeType?.startsWith("image/") == true ||
        extension in listOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "heif")
    val isVideo: Boolean get() = mimeType?.startsWith("video/") == true ||
        extension in listOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "3gp")
    val isAudio: Boolean get() = mimeType?.startsWith("audio/") == true ||
        extension in listOf("mp3", "wav", "flac", "aac", "ogg", "m4a", "wma")
    val isDocument: Boolean get() = mimeType?.contains("pdf") == true ||
        mimeType?.startsWith("text/") == true ||
        extension in listOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv", "rtf")
    val isApk: Boolean get() = extension == "apk" ||
        mimeType == "application/vnd.android.package-archive"
    val isPdf: Boolean get() = extension == "pdf" || mimeType?.contains("pdf") == true
    val isText: Boolean get() = mimeType?.startsWith("text/") == true ||
        extension in listOf("txt", "log", "json", "xml", "html", "css", "js", "kt", "java", "py", "md", "csv", "yaml", "yml", "ini", "cfg", "conf", "sh", "bat", "properties")
}
