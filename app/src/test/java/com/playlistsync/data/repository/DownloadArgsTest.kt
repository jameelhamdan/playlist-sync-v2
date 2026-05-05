package com.playlistsync.data.repository

import com.playlistsync.data.model.PlaylistConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadArgsTest {

    // ── audio mode ───────────────────────────────────────────────────────────

    @Test
    fun `audio mode sets extractAudio and addMetadata`() {
        val args = DownloadRepository.buildDownloadArgs(PlaylistConfig(syncMode = "audio"))
        assertTrue(args.extractAudio)
        assertTrue(args.addMetadata)
        assertFalse(args.embedSubs)
    }

    @Test
    fun `audio mode format string is bestaudio`() {
        val args = DownloadRepository.buildDownloadArgs(PlaylistConfig(syncMode = "audio"))
        assertEquals("bestaudio/best", args.formatString)
    }

    @Test
    fun `audio mode m4a enables embed thumbnail`() {
        val config = PlaylistConfig(syncMode = "audio", audioFormat = "m4a", embedThumbnail = true)
        assertTrue(DownloadRepository.buildDownloadArgs(config).embedThumbnail)
    }

    @Test
    fun `audio mode mp3 skips embed thumbnail (unsupported codec)`() {
        val config = PlaylistConfig(syncMode = "audio", audioFormat = "mp3", embedThumbnail = true)
        assertFalse(DownloadRepository.buildDownloadArgs(config).embedThumbnail)
    }

    @Test
    fun `audio mode opus enables embed thumbnail`() {
        val config = PlaylistConfig(syncMode = "audio", audioFormat = "opus", embedThumbnail = true)
        assertTrue(DownloadRepository.buildDownloadArgs(config).embedThumbnail)
    }

    @Test
    fun `audio mode embed thumbnail false is respected`() {
        val config = PlaylistConfig(syncMode = "audio", audioFormat = "m4a", embedThumbnail = false)
        assertFalse(DownloadRepository.buildDownloadArgs(config).embedThumbnail)
    }

    @Test
    fun `audio mode populates postprocessor args when all metadata provided`() {
        val args = DownloadRepository.buildDownloadArgs(
            config = PlaylistConfig(syncMode = "audio"),
            channelName = "Artist",
            playlistName = "Album",
            trackNumber = 5
        )
        assertNotNull(args.metadataPostprocessorArgs)
        assertTrue(args.metadataPostprocessorArgs!!.contains("album_artist=Artist"))
        assertTrue(args.metadataPostprocessorArgs.contains("album=Album"))
        assertTrue(args.metadataPostprocessorArgs.contains("track=5"))
    }

    @Test
    fun `audio mode with no metadata gives null postprocessor args`() {
        val args = DownloadRepository.buildDownloadArgs(
            config = PlaylistConfig(syncMode = "audio"),
            channelName = "",
            playlistName = "",
            trackNumber = 0
        )
        assertNull(args.metadataPostprocessorArgs)
    }

    // ── video mode ───────────────────────────────────────────────────────────

    @Test
    fun `video mode does not set extractAudio or addMetadata`() {
        val args = DownloadRepository.buildDownloadArgs(PlaylistConfig(syncMode = "video"))
        assertFalse(args.extractAudio)
        assertFalse(args.addMetadata)
        assertNull(args.metadataPostprocessorArgs)
    }

    @Test
    fun `video mode never embeds thumbnail`() {
        val config = PlaylistConfig(syncMode = "video", embedThumbnail = true)
        assertFalse(DownloadRepository.buildDownloadArgs(config).embedThumbnail)
    }

    @Test
    fun `video mode uses mergeOutputFormat`() {
        val config = PlaylistConfig(syncMode = "video", videoContainer = "mkv")
        assertEquals("mkv", DownloadRepository.buildDownloadArgs(config).mergeOutputFormat)
    }

    @Test
    fun `video mode embed subs when enabled`() {
        val config = PlaylistConfig(syncMode = "video", embedSubs = true)
        assertTrue(DownloadRepository.buildDownloadArgs(config).embedSubs)
    }

    @Test
    fun `video mode embed subs off by default`() {
        val config = PlaylistConfig(syncMode = "video", embedSubs = false)
        assertFalse(DownloadRepository.buildDownloadArgs(config).embedSubs)
    }

    @Test
    fun `video mode audio embed subs always false`() {
        val config = PlaylistConfig(syncMode = "audio", embedSubs = true)
        assertFalse(DownloadRepository.buildDownloadArgs(config).embedSubs)
    }

    // ── aria2c + extra args ──────────────────────────────────────────────────

    @Test
    fun `aria2c flag is forwarded`() {
        val config = PlaylistConfig(useAria2c = true)
        assertTrue(DownloadRepository.buildDownloadArgs(config).useAria2c)
    }

    @Test
    fun `extra args are split on whitespace`() {
        val config = PlaylistConfig(extraArgs = "--no-part --concurrent-fragments 4")
        val args = DownloadRepository.buildDownloadArgs(config)
        assertEquals(listOf("--no-part", "--concurrent-fragments", "4"), args.extraArgs)
    }

    @Test
    fun `blank extra args returns empty list`() {
        val config = PlaylistConfig(extraArgs = "")
        assertTrue(DownloadRepository.buildDownloadArgs(config).extraArgs.isEmpty())
    }

    @Test
    fun `custom format string bypasses quality preset`() {
        val config = PlaylistConfig(
            syncMode = "video",
            qualityPreset = "1080p",
            formatString = "bestvideo[ext=mp4]+bestaudio[ext=m4a]"
        )
        assertEquals("bestvideo[ext=mp4]+bestaudio[ext=m4a]", DownloadRepository.buildDownloadArgs(config).formatString)
    }
}
