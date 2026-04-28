package com.databridgepro.filemanager.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showScheduleDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        LargeTopAppBar(title = { Text("Settings") })

        when (val state = uiState) {
            is SettingsUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is SettingsUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is SettingsUiState.Success -> {
                val settings = state.settings

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SettingsGroupTitle("Appearance")

                    SettingsClickItem(
                        icon = Icons.Default.Brightness6,
                        title = "Theme",
                        subtitle = settings.theme.replaceFirstChar { it.uppercase() },
                        onClick = { showThemeDialog = true }
                    )

                    SettingsSwitchItem(
                        icon = Icons.Default.ColorLens,
                        title = "Dynamic Colors",
                        subtitle = "Use Material You colors from wallpaper",
                        checked = settings.dynamicColor,
                        onCheckedChange = { viewModel.setDynamicColor(it) }
                    )

                    SettingsGroupTitle("Security")

                    SettingsSwitchItem(
                        icon = Icons.Default.Fingerprint,
                        title = "Biometric Lock",
                        subtitle = "Require biometric authentication to open app",
                        checked = settings.biometricLock,
                        onCheckedChange = { viewModel.setBiometricLock(it) }
                    )

                    SettingsGroupTitle("Backup")

                    SettingsClickItem(
                        icon = Icons.Default.FolderOpen,
                        title = "Default Backup Folder",
                        subtitle = settings.backupFolder.ifEmpty { "Not set" },
                        onClick = { }
                    )

                    SettingsSwitchItem(
                        icon = Icons.Default.Sync,
                        title = "Auto Backup",
                        subtitle = "Automatically backup selected apps",
                        checked = settings.autoBackup,
                        onCheckedChange = { viewModel.setAutoBackup(it) }
                    )

                    if (settings.autoBackup) {
                        SettingsClickItem(
                            icon = Icons.Default.Schedule,
                            title = "Backup Schedule",
                            subtitle = settings.backupSchedule.replaceFirstChar { it.uppercase() },
                            onClick = { showScheduleDialog = true }
                        )
                    }

                    SettingsGroupTitle("Permissions")

                    SettingsClickItem(
                        icon = Icons.Default.Security,
                        title = "Storage Permissions",
                        subtitle = "Re-grant storage and SAF permissions",
                        onClick = { }
                    )

                    SettingsGroupTitle("About")

                    SettingsClickItem(
                        icon = Icons.Default.Info,
                        title = "Version",
                        subtitle = "1.0.0",
                        onClick = { }
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    if (showThemeDialog) {
        val state = uiState
        if (state is SettingsUiState.Success) {
            ThemeDialog(
                currentTheme = state.settings.theme,
                onDismiss = { showThemeDialog = false },
                onSelect = {
                    viewModel.setTheme(it)
                    showThemeDialog = false
                }
            )
        }
    }

    if (showScheduleDialog) {
        val state = uiState
        if (state is SettingsUiState.Success) {
            ScheduleDialog(
                currentSchedule = state.settings.backupSchedule,
                onDismiss = { showScheduleDialog = false },
                onSelect = {
                    viewModel.setBackupSchedule(it)
                    showScheduleDialog = false
                }
            )
        }
    }
}

@Composable
private fun SettingsGroupTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsClickItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun ThemeDialog(
    currentTheme: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val themes = listOf("system" to "System Default", "light" to "Light", "dark" to "Dark")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Theme") },
        text = {
            Column {
                themes.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(value) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentTheme == value,
                            onClick = { onSelect(value) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ScheduleDialog(
    currentSchedule: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val schedules = listOf("daily" to "Daily", "weekly" to "Weekly", "monthly" to "Monthly")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Backup Schedule") },
        text = {
            Column {
                schedules.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(value) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentSchedule == value,
                            onClick = { onSelect(value) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    MaterialTheme {
        SettingsScreen()
    }
}
