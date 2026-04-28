package com.databridgepro.filemanager.presentation.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.databridgepro.filemanager.data.model.FileItem
import com.databridgepro.filemanager.data.repository.StorageInfo
import com.databridgepro.filemanager.util.PermissionUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToFiles: () -> Unit = {},
    onNavigateToBackup: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToFolder: (String, String) -> Unit = { _, _ -> },
    onNavigateToCategory: (String) -> Unit = {},
    onOpenFile: (FileItem) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        LargeTopAppBar(
            title = {
                Column {
                    Text("DataBridge Pro", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("File Manager", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            actions = {
                IconButton(onClick = { viewModel.loadData() }) { Icon(Icons.Default.Refresh, contentDescription = "Refresh") }
            },
            colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        when (val state = uiState) {
            is HomeUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
            is HomeUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { viewModel.loadData() }) { Text("Retry") }
                    }
                }
            }
            is HomeUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(4.dp)) }

                    item { StorageArcChart(storageInfo = state.storageInfo) }

                    item {
                        Text("Categories", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            item {
                                CategoryChip(icon = Icons.Default.Image, label = "Photos", color = Color(0xFF4CAF50),
                                    onClick = { onNavigateToCategory("image") })
                            }
                            item {
                                CategoryChip(icon = Icons.Default.VideoFile, label = "Videos", color = Color(0xFFE91E63),
                                    onClick = { onNavigateToCategory("video") })
                            }
                            item {
                                CategoryChip(icon = Icons.Default.AudioFile, label = "Audio", color = Color(0xFF9C27B0),
                                    onClick = { onNavigateToCategory("audio") })
                            }
                            item {
                                CategoryChip(icon = Icons.Default.Description, label = "Docs", color = Color(0xFF2196F3),
                                    onClick = { onNavigateToCategory("document") })
                            }
                            item {
                                CategoryChip(icon = Icons.Default.PhoneAndroid, label = "APKs", color = Color(0xFFFF9800),
                                    onClick = { onNavigateToCategory("apk") })
                            }
                        }
                    }

                    item {
                        Text("Quick Folders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            QuickActionCard(modifier = Modifier.weight(1f), icon = Icons.Default.Download, title = "Downloads",
                                onClick = { onNavigateToFolder(viewModel.getQuickFolderPath("Downloads"), "Downloads") })
                            QuickActionCard(modifier = Modifier.weight(1f), icon = Icons.Default.CameraAlt, title = "DCIM",
                                onClick = { onNavigateToFolder(viewModel.getQuickFolderPath("DCIM"), "DCIM") })
                            QuickActionCard(modifier = Modifier.weight(1f), icon = Icons.Default.Description, title = "Documents",
                                onClick = { onNavigateToFolder(viewModel.getQuickFolderPath("Documents"), "Documents") })
                        }
                    }

                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            QuickActionCard(modifier = Modifier.weight(1f), icon = Icons.Default.FolderSpecial, title = "WhatsApp",
                                onClick = { onNavigateToFolder(viewModel.getQuickFolderPath("WhatsApp"), "WhatsApp") })
                            QuickActionCard(modifier = Modifier.weight(1f), icon = Icons.Default.FolderOpen, title = "Browse All",
                                onClick = onNavigateToFiles)
                            QuickActionCard(modifier = Modifier.weight(1f), icon = Icons.Default.SdStorage, title = "Android/data",
                                onClick = { onNavigateToFolder(viewModel.getQuickFolderPath("Android/data"), "Android/data") })
                        }
                    }

                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            QuickActionCard(modifier = Modifier.weight(1f), icon = Icons.Default.Warning, title = "Large Files",
                                onClick = { viewModel.loadLargeFiles() })
                            QuickActionCard(modifier = Modifier.weight(1f), icon = Icons.Default.ContentCopy, title = "Duplicates",
                                onClick = { viewModel.loadDuplicates() })
                            QuickActionCard(modifier = Modifier.weight(1f), icon = Icons.Default.Settings, title = "Settings",
                                onClick = onNavigateToSettings)
                        }
                    }

                    if (state.largeFiles.isNotEmpty()) {
                        item {
                            Text("Large Files (>10 MB)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        items(state.largeFiles.take(10)) { file ->
                            FileListItem(file = file, onClick = { onOpenFile(file) })
                        }
                    }

                    if (state.duplicateGroups.isNotEmpty()) {
                        item {
                            Text("Possible Duplicates", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        items(state.duplicateGroups.take(5)) { group ->
                            DuplicateGroupCard(files = group, onClick = { onOpenFile(it) })
                        }
                    }

                    if (state.recentFiles.isNotEmpty()) {
                        item {
                            Text("Recent Files", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        items(state.recentFiles.take(15)) { file ->
                            FileListItem(file = file, onClick = { onOpenFile(file) })
                        }
                    }

                    if (state.recentBackups.isNotEmpty()) {
                        item {
                            Text("Recent Backups", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        items(state.recentBackups) { backup ->
                            RecentBackupItem(backup = backup)
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun StorageArcChart(storageInfo: StorageInfo) {
    var targetProgress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(targetValue = targetProgress, animationSpec = tween(durationMillis = 1200), label = "storage_arc")
    LaunchedEffect(storageInfo) {
        targetProgress = if (storageInfo.totalBytes > 0) storageInfo.usedBytes.toFloat() / storageInfo.totalBytes.toFloat() else 0f
    }
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val surfaceVariant = MaterialTheme.colorScheme.outlineVariant

    Card(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Storage, contentDescription = null, tint = primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Internal Storage", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(20.dp))
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(180.dp)) {
                    val strokeWidth = 24f
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                    drawArc(color = surfaceVariant, startAngle = 135f, sweepAngle = 270f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
                    drawArc(brush = Brush.sweepGradient(colors = listOf(primary, tertiary, primary)), startAngle = 135f, sweepAngle = 270f * animatedProgress, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${(animatedProgress * 100).toInt()}%", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = primary)
                    Text("used", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StorageStat("Used", PermissionUtils.formatFileSize(storageInfo.usedBytes), primary)
                StorageStat("Free", PermissionUtils.formatFileSize(storageInfo.freeBytes), MaterialTheme.colorScheme.tertiary)
                StorageStat("Total", PermissionUtils.formatFileSize(storageInfo.totalBytes), MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun StorageStat(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CategoryChip(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = color)
        }
    }
}

@Composable
private fun QuickActionCard(modifier: Modifier = Modifier, icon: ImageVector, title: String, onClick: () -> Unit) {
    Card(
        onClick = onClick, modifier = modifier, shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(shape = androidx.compose.foundation.shape.CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun FileListItem(file: FileItem, onClick: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    Card(
        onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    val icon = when {
                        file.isImage -> Icons.Default.Image
                        file.isVideo -> Icons.Default.VideoFile
                        file.isAudio -> Icons.Default.AudioFile
                        file.isDocument -> Icons.Default.Description
                        file.isApk -> Icons.Default.PhoneAndroid
                        else -> Icons.Default.Description
                    }
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(file.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${PermissionUtils.formatFileSize(file.size)} • ${dateFormat.format(Date(file.lastModified))}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DuplicateGroupCard(files: List<FileItem>, onClick: (FileItem) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "${files.size} files • ${PermissionUtils.formatFileSize(files.first().size)} each",
                style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            files.take(3).forEach { file ->
                Text(
                    file.name, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
            if (files.size > 3) {
                Text("... and ${files.size - 3} more", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun RecentBackupItem(backup: com.databridgepro.filemanager.data.model.BackupItem) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    Card(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(backup.appName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${PermissionUtils.formatFileSize(backup.sizeBytes)} • ${dateFormat.format(Date(backup.createdAt))}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    MaterialTheme {
        HomeScreen()
    }
}
