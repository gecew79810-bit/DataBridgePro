package com.databridgepro.filemanager.presentation.viewer

import android.content.Intent
import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.databridgepro.filemanager.util.PermissionUtils
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileViewerScreen(
    filePath: String,
    fileName: String,
    fileType: String,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val file = remember { File(filePath) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        fileName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        try {
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "*/*"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share $fileName"))
                        } catch (_: Exception) { }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = {
                        try {
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "*/*")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) { }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open with")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (fileType) {
                "image" -> ImageViewer(file)
                "video" -> VideoViewer(file)
                "audio" -> AudioViewer(file, fileName)
                "pdf" -> PdfViewer(file, context)
                "text" -> TextViewer(file)
                "apk" -> ApkViewer(file, fileName, context)
                else -> GenericViewer(file, fileName, context)
            }
        }
    }
}

@Composable
private fun ImageViewer(file: File) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.5f, 5f)
                    offsetX += pan.x
                    offsetY += pan.y
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(file)
                .crossfade(true)
                .build(),
            contentDescription = file.name,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                ),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun VideoViewer(file: File) {
    var isPlaying by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    setVideoURI(Uri.fromFile(file))
                    setOnPreparedListener { mp ->
                        mp.isLooping = false
                        if (isPlaying) start()
                    }
                    start()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp)),
            update = { videoView ->
                if (isPlaying && !videoView.isPlaying) {
                    videoView.start()
                } else if (!isPlaying && videoView.isPlaying) {
                    videoView.pause()
                }
            }
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { isPlaying = !isPlaying }) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(file.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(PermissionUtils.formatFileSize(file.length()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AudioViewer(file: File, fileName: String) {
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose { mediaPlayer?.release() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(160.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.AudioFile,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(fileName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(modifier = Modifier.height(8.dp))
        Text(PermissionUtils.formatFileSize(file.length()), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = {
                if (isPlaying) {
                    mediaPlayer?.pause()
                    isPlaying = false
                } else {
                    if (mediaPlayer == null) {
                        mediaPlayer = android.media.MediaPlayer().apply {
                            setDataSource(file.absolutePath)
                            prepare()
                            setOnCompletionListener { isPlaying = false }
                        }
                    }
                    mediaPlayer?.start()
                    isPlaying = true
                }
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.size(72.dp)
        ) {
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun PdfViewer(file: File, context: android.content.Context) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.size(120.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Description,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(file.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(modifier = Modifier.height(8.dp))
        Text(PermissionUtils.formatFileSize(file.length()), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                try {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/pdf")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(intent)
                } catch (_: Exception) { }
            },
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open in PDF Viewer")
        }
    }
}

@Composable
private fun TextViewer(file: File) {
    var content by remember { mutableStateOf("Loading...") }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(file) {
        try {
            content = if (file.length() > 2 * 1024 * 1024) {
                file.bufferedReader().use { reader ->
                    val sb = StringBuilder()
                    var line: String?
                    var count = 0
                    while (reader.readLine().also { line = it } != null && count < 5000) {
                        sb.appendLine(line)
                        count++
                    }
                    if (count >= 5000) sb.appendLine("\n... (truncated, file too large)")
                    sb.toString()
                }
            } else {
                file.readText()
            }
        } catch (e: Exception) {
            content = "Error reading file: ${e.message}"
        }
        isLoading = false
    }

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Text(
                    text = content,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun ApkViewer(file: File, fileName: String, context: android.content.Context) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(120.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Android,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(fileName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(modifier = Modifier.height(8.dp))
        Text(PermissionUtils.formatFileSize(file.length()), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                try {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/vnd.android.package-archive")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (_: Exception) { }
            },
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Download, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Install APK")
        }
    }
}

@Composable
private fun GenericViewer(file: File, fileName: String, context: android.content.Context) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(fileName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(PermissionUtils.formatFileSize(file.length()), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                try {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "*/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(intent)
                } catch (_: Exception) { }
            },
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open With...")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FileViewerScreenPreview() {
    MaterialTheme {
        FileViewerScreen(filePath = "/test", fileName = "test.txt", fileType = "text")
    }
}
