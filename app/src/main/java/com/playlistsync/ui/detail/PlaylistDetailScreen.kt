package com.playlistsync.ui.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.playlistsync.data.model.VideoEntity
import com.playlistsync.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    onBack: () -> Unit,
    onFormatConfig: (String) -> Unit,
    viewModel: PlaylistDetailViewModel = hiltViewModel()
) {
    val playlist by viewModel.playlist.collectAsStateWithLifecycle()
    val videos   by viewModel.videos.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(playlist?.name ?: "Playlist") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.syncNow() }) {
                        Icon(Icons.Default.Sync, contentDescription = "Sync now")
                    }
                    IconButton(onClick = { onFormatConfig(viewModel.playlistId) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Format config")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            playlist?.let { pl ->
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${pl.downloadedCount} / ${pl.videoCount} downloaded",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (pl.downloadedCount == pl.videoCount && pl.videoCount > 0)
                                    StatusDownloaded else MaterialTheme.colorScheme.onSurface
                            )
                            SyncModeLabel(pl.config.syncMode)
                        }
                        pl.lastSyncedAt?.let { ts ->
                            Text(
                                "Last synced: ${formatTimestamp(ts)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }

            if (videos.isEmpty() && playlist != null) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No videos found. Tap ↻ to sync.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(videos, key = { it.id }) { video ->
                VideoRow(
                    video = video,
                    onRetry = { viewModel.retryVideo(video.id) }
                )
            }
        }
    }
}

@Composable
private fun SyncModeLabel(syncMode: String) {
    val isAudio = syncMode == "audio"
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            if (isAudio) Icons.Default.AudioFile else Icons.Default.VideoFile,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            if (isAudio) "Audio" else "Video",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun VideoRow(video: VideoEntity, onRetry: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = {
                Text(video.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
            },
            supportingContent = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    StatusRow(video = video, onRetry = onRetry)
                    if (video.status == "downloading") {
                        LinearProgressIndicator(
                            progress = { video.downloadProgress / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (video.status == "error" && video.errorMessage != null) {
                        Text(
                            video.errorMessage,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            },
            leadingContent = {
                Text(
                    "#${video.playlistIndex + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(28.dp)
                )
            },
            trailingContent = {
                if (video.duration > 0) {
                    Text(
                        formatDuration(video.duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
        HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
    }
}

@Composable
private fun StatusRow(video: VideoEntity, onRetry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val (label, color) = when (video.status) {
                "downloaded"  -> "Downloaded"  to StatusDownloaded
                "downloading" -> "Downloading ${video.downloadProgress}%" to StatusDownloading
                "error"       -> "Error"       to MaterialTheme.colorScheme.error
                "skipped"     -> "Skipped"     to MaterialTheme.colorScheme.onSurfaceVariant
                else          -> "Pending"     to StatusPending
            }
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)

            if (video.status == "error") {
                TextButton(
                    onClick = onRetry,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    modifier = Modifier.height(20.dp)
                ) {
                    Text("Retry", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        if (video.status == "downloaded" && video.localPath != null) {
            Text(
                video.localPath.substringAfterLast('/'),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

private fun formatTimestamp(epochMillis: Long): String =
    SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(epochMillis))
