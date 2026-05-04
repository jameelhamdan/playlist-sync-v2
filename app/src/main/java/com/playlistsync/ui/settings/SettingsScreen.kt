package com.playlistsync.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.playlistsync.data.settings.AppSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val s by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            SectionHeader("Auto-Sync")

            SettingsDropdown(
                label = "Sync interval",
                options = listOf(1, 2, 6, 12, 24),
                optionLabels = listOf("1 hour", "2 hours", "6 hours", "12 hours", "24 hours"),
                selected = s.syncIntervalHours,
                onSelected = { v -> viewModel.update { it.copy(syncIntervalHours = v) } }
            )

            SwitchPrefRow(
                label = "Wi-Fi only",
                subtitle = "Only sync and download on unmetered connections",
                checked = s.wifiOnly,
                onCheckedChange = { v -> viewModel.update { it.copy(wifiOnly = v) } }
            )

            SwitchPrefRow(
                label = "Require battery not low",
                subtitle = "Pause sync when battery is low",
                checked = s.requireBatteryNotLow,
                onCheckedChange = { v -> viewModel.update { it.copy(requireBatteryNotLow = v) } }
            )

            Spacer(Modifier.height(8.dp))
            SectionHeader("New Playlist Defaults")

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(
                    selected = s.defaultSyncMode == "audio",
                    onClick = { viewModel.update { it.copy(defaultSyncMode = "audio") } },
                    label = { Text("Audio only") }
                )
                FilterChip(
                    selected = s.defaultSyncMode == "video",
                    onClick = { viewModel.update { it.copy(defaultSyncMode = "video") } },
                    label = { Text("Video + Audio") }
                )
            }

            Spacer(Modifier.height(4.dp))

            if (s.defaultSyncMode == "audio") {
                SettingsStringDropdown(
                    label = "Default audio format",
                    options = listOf("m4a", "mp3", "opus", "flac", "wav"),
                    optionLabels = listOf("M4A (recommended)", "MP3", "Opus", "FLAC", "WAV"),
                    selected = s.defaultAudioFormat,
                    onSelected = { v -> viewModel.update { it.copy(defaultAudioFormat = v) } }
                )
            } else {
                SettingsStringDropdown(
                    label = "Default video quality",
                    options = listOf("best", "1080p", "720p", "480p", "360p"),
                    optionLabels = listOf("Best available", "1080p", "720p", "480p", "360p"),
                    selected = s.defaultVideoQuality,
                    onSelected = { v -> viewModel.update { it.copy(defaultVideoQuality = v) } }
                )
            }

            SwitchPrefRow(
                label = "Embed thumbnail",
                subtitle = "Embed artwork in downloaded audio / video files",
                checked = s.defaultEmbedThumbnail,
                onCheckedChange = { v -> viewModel.update { it.copy(defaultEmbedThumbnail = v) } }
            )

            SettingsDropdown(
                label = "Parallel downloads",
                options = listOf(1, 2, 3, 4, 5),
                optionLabels = listOf("1 (sequential)", "2", "3", "4", "5"),
                selected = s.defaultConcurrentDownloads,
                onSelected = { v -> viewModel.update { it.copy(defaultConcurrentDownloads = v) } }
            )

            OutlinedTextField(
                value = s.defaultProxyUrl,
                onValueChange = { v -> viewModel.update { it.copy(defaultProxyUrl = v) } },
                label = { Text("Default proxy URL") },
                placeholder = { Text("http://host:port  or  socks5://host:port") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    HorizontalDivider()
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun SwitchPrefRow(
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsDropdown(
    label: String,
    options: List<Int>,
    optionLabels: List<String>,
    selected: Int,
    onSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = optionLabels.getOrElse(options.indexOf(selected)) { selected.toString() }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.zip(optionLabels).forEach { (option, optLabel) ->
                DropdownMenuItem(
                    text = { Text(optLabel) },
                    onClick = { onSelected(option); expanded = false }
                )
            }
        }
    }
    Spacer(Modifier.height(8.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsStringDropdown(
    label: String,
    options: List<String>,
    optionLabels: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = optionLabels.getOrElse(options.indexOf(selected)) { selected }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.zip(optionLabels).forEach { (option, optLabel) ->
                DropdownMenuItem(
                    text = { Text(optLabel) },
                    onClick = { onSelected(option); expanded = false }
                )
            }
        }
    }
    Spacer(Modifier.height(8.dp))
}
