package com.playlistsync.ui.playlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ScheduleSend
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.playlistsync.data.model.PlaylistEntity
import com.playlistsync.ui.theme.StatusDownloaded
import com.playlistsync.ui.theme.StatusPending

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistListScreen(
    onAddPlaylist: () -> Unit,
    onPlaylistClick: (String) -> Unit,
    viewModel: PlaylistListViewModel = hiltViewModel()
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("PlaylistSync") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddPlaylist) {
                Icon(Icons.Default.Add, contentDescription = "Add playlist")
            }
        }
    ) { padding ->
        if (playlists.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.LibraryMusic,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Text(
                        "No playlists yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Tap + to add a YouTube playlist",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = padding,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(playlists, key = { it.id }) { playlist ->
                    PlaylistRow(
                        playlist = playlist,
                        onClick = { onPlaylistClick(playlist.id) },
                        onSyncNow = { viewModel.syncNow(playlist) },
                        onDelete = { viewModel.delete(playlist) },
                        onToggleAutoSync = { viewModel.toggleAutoSync(playlist) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistRow(
    playlist: PlaylistEntity,
    onClick: () -> Unit,
    onSyncNow: () -> Unit,
    onDelete: () -> Unit,
    onToggleAutoSync: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Remove playlist?") },
            text = { Text("\"${playlist.name}\" will be removed from the app.") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete() }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = playlist.thumbnailUrl.ifEmpty { null },
                contentDescription = null,
                modifier = Modifier.size(60.dp)
            )

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (playlist.channelName.isNotEmpty()) {
                    Text(
                        text = playlist.channelName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    SyncModeChip(playlist.config.syncMode)
                    val allDone = playlist.downloadedCount == playlist.videoCount && playlist.videoCount > 0
                    Text(
                        text = "${playlist.downloadedCount}/${playlist.videoCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (allDone) StatusDownloaded else StatusPending
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onSyncNow, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Sync, contentDescription = "Sync now", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onToggleAutoSync, modifier = Modifier.size(36.dp)) {
                    Icon(
                        if (playlist.autoSyncEnabled) Icons.Default.Schedule
                        else Icons.AutoMirrored.Filled.ScheduleSend,
                        contentDescription = if (playlist.autoSyncEnabled) "Auto-sync on" else "Auto-sync off",
                        modifier = Modifier.size(20.dp),
                        tint = if (playlist.autoSyncEnabled)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
                IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun SyncModeChip(syncMode: String) {
    val isAudio = syncMode == "audio"
    SuggestionChip(
        onClick = {},
        label = {
            Text(
                if (isAudio) "Audio" else "Video",
                style = MaterialTheme.typography.labelSmall
            )
        },
        icon = {
            Icon(
                if (isAudio) Icons.Default.AudioFile else Icons.Default.VideoFile,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
        }
    )
}
