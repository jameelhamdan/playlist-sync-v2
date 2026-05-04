package com.playlistsync.ui.playlist

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlaylistScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddPlaylistViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val url by viewModel.urlInput.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is AddPlaylistUiState.Saved -> onSaved()
            is AddPlaylistUiState.Error -> snackbarHostState.showSnackbar(state.message)
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Playlist") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = url,
                onValueChange = { viewModel.urlInput.value = it },
                label = { Text("YouTube Playlist URL") },
                placeholder = { Text("https://www.youtube.com/playlist?list=…") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = uiState !is AddPlaylistUiState.Loading
            )

            Button(
                onClick = { viewModel.fetchPlaylistInfo() },
                enabled = url.isNotBlank() && uiState !is AddPlaylistUiState.Loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState is AddPlaylistUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Fetching…")
                } else {
                    Text("Fetch Playlist Info")
                }
            }

            if (uiState is AddPlaylistUiState.Preview) {
                PreviewSection(
                    preview = uiState as AddPlaylistUiState.Preview,
                    onSyncModeChange = { viewModel.setSyncMode(it) },
                    onAudioFormatChange = { viewModel.setAudioFormat(it) },
                    onVideoQualityChange = { viewModel.setVideoQuality(it) },
                    onSave = { viewModel.savePlaylist() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreviewSection(
    preview: AddPlaylistUiState.Preview,
    onSyncModeChange: (String) -> Unit,
    onAudioFormatChange: (String) -> Unit,
    onVideoQualityChange: (String) -> Unit,
    onSave: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(preview.name, style = MaterialTheme.typography.titleMedium)
            if (preview.channelName.isNotEmpty()) {
                Text(
                    preview.channelName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "${preview.videoCount} videos",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    Text("Download as", style = MaterialTheme.typography.labelLarge)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FilterChip(
            selected = preview.syncMode == "audio",
            onClick = { onSyncModeChange("audio") },
            label = { Text("Audio only") }
        )
        FilterChip(
            selected = preview.syncMode == "video",
            onClick = { onSyncModeChange("video") },
            label = { Text("Video + Audio") }
        )
    }

    if (preview.syncMode == "audio") {
        FormatDropdown(
            label = "Audio format",
            options = listOf("m4a", "mp3", "opus", "flac", "wav"),
            selected = preview.audioFormat,
            onSelected = onAudioFormatChange
        )
    } else {
        FormatDropdown(
            label = "Video quality",
            options = listOf("best", "1080p", "720p", "480p", "360p"),
            selected = preview.videoQuality,
            onSelected = onVideoQualityChange
        )
    }

    Button(
        onClick = onSave,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Add Playlist")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormatDropdown(
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
                    text = { Text(option.uppercase()) },
                    onClick = { onSelected(option); expanded = false }
                )
            }
        }
    }
}
