package com.playlistsync.ui.config

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
import com.playlistsync.data.model.PlaylistConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormatConfigScreen(
    onBack: () -> Unit,
    viewModel: FormatConfigViewModel = hiltViewModel()
) {
    val config by viewModel.config.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Format Config") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SyncModeSection(config) { viewModel.updateConfig(it) }
            FormatSection(config) { viewModel.updateConfig(it) }
            OptionsSection(config) { viewModel.updateConfig(it) }
            AdvancedSection(config) { viewModel.updateConfig(it) }

            Button(
                onClick = { viewModel.save(); onBack() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}

@Composable
private fun SyncModeSection(config: PlaylistConfig, onUpdate: (PlaylistConfig) -> Unit) {
    SectionHeader("Sync Mode")
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FilterChip(
            selected = config.syncMode == "audio",
            onClick = { onUpdate(config.copy(syncMode = "audio")) },
            label = { Text("Audio only") }
        )
        FilterChip(
            selected = config.syncMode == "video",
            onClick = { onUpdate(config.copy(syncMode = "video")) },
            label = { Text("Video + Audio") }
        )
    }
}

@Composable
private fun FormatSection(config: PlaylistConfig, onUpdate: (PlaylistConfig) -> Unit) {
    SectionHeader("Format")
    if (config.syncMode == "audio") {
        DropdownSetting(
            label = "Audio Format",
            options = listOf("m4a", "mp3", "opus", "flac", "wav"),
            selected = config.audioFormat,
            onSelected = { onUpdate(config.copy(audioFormat = it)) }
        )
    } else {
        DropdownSetting(
            label = "Video Container",
            options = listOf("mp4", "webm", "mkv"),
            selected = config.videoContainer,
            onSelected = { onUpdate(config.copy(videoContainer = it)) }
        )
        Spacer(Modifier.height(4.dp))
        DropdownSetting(
            label = "Quality",
            options = listOf("best", "1080p", "720p", "480p", "360p"),
            selected = config.qualityPreset,
            onSelected = { onUpdate(config.copy(qualityPreset = it)) }
        )
    }
}

@Composable
private fun OptionsSection(config: PlaylistConfig, onUpdate: (PlaylistConfig) -> Unit) {
    SectionHeader("Options")
    if (config.syncMode == "audio") {
        SwitchRow(
            label = "Embed thumbnail",
            checked = config.embedThumbnail,
            onCheckedChange = { onUpdate(config.copy(embedThumbnail = it)) }
        )
    } else {
        SwitchRow(
            label = "Embed subtitles (EN)",
            checked = config.embedSubs,
            onCheckedChange = { onUpdate(config.copy(embedSubs = it)) }
        )
    }
    SwitchRow(
        label = "Use aria2c (faster multi-connection download)",
        checked = config.useAria2c,
        onCheckedChange = { onUpdate(config.copy(useAria2c = it)) }
    )
    Spacer(Modifier.height(4.dp))
    DropdownSetting(
        label = "Parallel downloads",
        options = listOf("1", "2", "3", "4", "5"),
        selected = config.maxConcurrentDownloads.toString(),
        onSelected = { onUpdate(config.copy(maxConcurrentDownloads = it.toInt())) }
    )
}

@Composable
private fun AdvancedSection(config: PlaylistConfig, onUpdate: (PlaylistConfig) -> Unit) {
    SectionHeader("Advanced")
    OutlinedTextField(
        value = config.proxyUrl,
        onValueChange = { onUpdate(config.copy(proxyUrl = it)) },
        label = { Text("Proxy URL") },
        placeholder = { Text("http://host:port  or  socks5://host:port") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    Spacer(Modifier.height(4.dp))
    OutlinedTextField(
        value = config.extraArgs,
        onValueChange = { onUpdate(config.copy(extraArgs = it)) },
        label = { Text("Extra yt-dlp args") },
        placeholder = { Text("--no-overwrites --cookies /sdcard/cookies.txt") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 2
    )
    Spacer(Modifier.height(4.dp))
    OutlinedTextField(
        value = config.formatString,
        onValueChange = { onUpdate(config.copy(formatString = it)) },
        label = { Text("Custom format string (overrides all above)") },
        placeholder = { Text("bestvideo[height<=1080]+bestaudio/best") },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    HorizontalDivider()
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownSetting(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { onSelected(option); expanded = false }
                )
            }
        }
    }
}
