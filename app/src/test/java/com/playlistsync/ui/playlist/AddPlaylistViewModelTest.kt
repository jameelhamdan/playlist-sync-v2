package com.playlistsync.ui.playlist

import com.playlistsync.data.repository.PlaylistMetadata
import com.playlistsync.data.repository.VideoMetadata
import com.playlistsync.data.settings.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AddPlaylistViewModelTest {

    private val sampleMeta = PlaylistMetadata(
        id = "PLabc123",
        title = "My Playlist",
        channelName = "Some Artist",
        thumbnailUrl = "https://img.example.com/thumb.jpg",
        entries = listOf(
            VideoMetadata(ytId = "vid1", title = "Track 1", duration = 180L, thumbnailUrl = "https://t1.jpg", playlistIndex = 0),
            VideoMetadata(ytId = "vid2", title = "Track 2", duration = 240L, thumbnailUrl = "https://t2.jpg", playlistIndex = 1),
            VideoMetadata(ytId = "vid3", title = "Track 3", duration = 300L, thumbnailUrl = "https://t3.jpg", playlistIndex = 2),
        )
    )

    // ── buildPlaylistEntity ──────────────────────────────────────────────────

    @Test
    fun `playlist entity id and url come from meta and url param`() {
        val preview = AddPlaylistUiState.Preview(
            name = "My Playlist", videoCount = 3, channelName = "Some Artist",
            syncMode = "audio", audioFormat = "m4a", videoQuality = "best"
        )
        val entity = AddPlaylistViewModel.buildPlaylistEntity(
            "https://youtube.com/playlist?list=PLabc123", sampleMeta, preview, AppSettings()
        )
        assertEquals("PLabc123", entity.id)
        assertEquals("https://youtube.com/playlist?list=PLabc123", entity.url)
    }

    @Test
    fun `playlist entity videoCount reflects entry count`() {
        val preview = AddPlaylistUiState.Preview(
            name = "My Playlist", videoCount = 3, channelName = "Some Artist",
            syncMode = "audio", audioFormat = "m4a", videoQuality = "best"
        )
        val entity = AddPlaylistViewModel.buildPlaylistEntity("url", sampleMeta, preview, AppSettings())
        assertEquals(3, entity.videoCount)
    }

    @Test
    fun `playlist entity config inherits syncMode from preview`() {
        val preview = AddPlaylistUiState.Preview(
            name = "p", videoCount = 0, channelName = "",
            syncMode = "video", audioFormat = "opus", videoQuality = "720p"
        )
        val entity = AddPlaylistViewModel.buildPlaylistEntity("url", sampleMeta, preview, AppSettings())
        assertEquals("video", entity.config.syncMode)
        assertEquals("720p", entity.config.qualityPreset)
    }

    @Test
    fun `playlist entity config inherits audioFormat from preview`() {
        val preview = AddPlaylistUiState.Preview(
            name = "p", videoCount = 0, channelName = "",
            syncMode = "audio", audioFormat = "flac", videoQuality = "best"
        )
        val entity = AddPlaylistViewModel.buildPlaylistEntity("url", sampleMeta, preview, AppSettings())
        assertEquals("flac", entity.config.audioFormat)
    }

    @Test
    fun `playlist entity config inherits embedThumbnail from settings`() {
        val preview = AddPlaylistUiState.Preview(
            name = "p", videoCount = 0, channelName = "",
            syncMode = "audio", audioFormat = "m4a", videoQuality = "best"
        )
        val settings = AppSettings(defaultEmbedThumbnail = false)
        val entity = AddPlaylistViewModel.buildPlaylistEntity("url", sampleMeta, preview, settings)
        assertEquals(false, entity.config.embedThumbnail)
    }

    @Test
    fun `playlist entity config inherits concurrentDownloads from settings`() {
        val preview = AddPlaylistUiState.Preview(
            name = "p", videoCount = 0, channelName = "",
            syncMode = "audio", audioFormat = "m4a", videoQuality = "best"
        )
        val settings = AppSettings(defaultConcurrentDownloads = 4)
        val entity = AddPlaylistViewModel.buildPlaylistEntity("url", sampleMeta, preview, settings)
        assertEquals(4, entity.config.maxConcurrentDownloads)
    }

    // ── buildVideoEntities ───────────────────────────────────────────────────

    @Test
    fun `video entity id is playlistId slash ytId`() {
        val videos = AddPlaylistViewModel.buildVideoEntities(sampleMeta, "PLabc123")
        assertEquals("PLabc123/vid1", videos[0].id)
        assertEquals("PLabc123/vid2", videos[1].id)
        assertEquals("PLabc123/vid3", videos[2].id)
    }

    @Test
    fun `all video entities start with pending status`() {
        val videos = AddPlaylistViewModel.buildVideoEntities(sampleMeta, "PLabc123")
        assertTrue(videos.all { it.status == "pending" })
    }

    @Test
    fun `video entity count matches metadata entries`() {
        val videos = AddPlaylistViewModel.buildVideoEntities(sampleMeta, "PLabc123")
        assertEquals(3, videos.size)
    }

    @Test
    fun `video entity playlistId matches supplied id`() {
        val videos = AddPlaylistViewModel.buildVideoEntities(sampleMeta, "PLabc123")
        assertTrue(videos.all { it.playlistId == "PLabc123" })
    }

    @Test
    fun `video entity preserves metadata fields`() {
        val videos = AddPlaylistViewModel.buildVideoEntities(sampleMeta, "PLabc123")
        val first = videos[0]
        assertEquals("vid1", first.ytId)
        assertEquals("Track 1", first.title)
        assertEquals(180L, first.duration)
        assertEquals("https://t1.jpg", first.thumbnailUrl)
        assertEquals(0, first.playlistIndex)
    }

    @Test
    fun `video entity playlist index is preserved from metadata`() {
        val videos = AddPlaylistViewModel.buildVideoEntities(sampleMeta, "PLabc123")
        assertEquals(0, videos[0].playlistIndex)
        assertEquals(1, videos[1].playlistIndex)
        assertEquals(2, videos[2].playlistIndex)
    }

    @Test
    fun `empty playlist produces empty video list`() {
        val emptyMeta = sampleMeta.copy(entries = emptyList())
        val videos = AddPlaylistViewModel.buildVideoEntities(emptyMeta, "PLabc123")
        assertTrue(videos.isEmpty())
    }
}
