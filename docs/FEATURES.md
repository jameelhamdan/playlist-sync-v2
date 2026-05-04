# PlaylistSync — Feature Reference

## Playlist Management

### Add a Playlist
- Paste any YouTube playlist URL into the Add Playlist screen
- Tap **Fetch Playlist Info** — the app calls yt-dlp to retrieve the playlist title, channel name, and video count without downloading any media
- A preview card shows the fetched details before saving
- Tap **Add Playlist** to save it to the local database

### Playlist List
- All saved playlists are shown in a scrollable list
- Each row shows: thumbnail, playlist name, channel name, sync mode badge (Audio / Video), and downloaded/total video count
- The video count label turns green when all videos are downloaded
- Empty state shown when no playlists have been added yet

### Delete a Playlist
- Tap the delete icon on any playlist row
- A confirmation dialog prevents accidental deletion
- Deletes the playlist record and all associated video records from the local database

---

## Syncing and Downloading

### Manual Sync
- Tap the **sync icon** (↻) on the playlist list or the detail screen to trigger an immediate download of all pending/failed videos in that playlist

### Auto-Sync (Background)
- Each playlist has an **auto-sync toggle** (clock icon on playlist row)
- When enabled, a WorkManager periodic job runs every **6 hours** and checks each auto-sync playlist for new videos
- New videos are discovered by comparing the remote playlist's video IDs against locally known IDs — only new entries are queued
- The periodic job requires network connectivity and battery not low

### Download Queue
- Downloads run one video at a time per playlist in playlist order
- A download worker chains itself: when one video finishes it immediately enqueues the next pending video in the same playlist
- Downloads survive app restarts — WorkManager re-enqueues automatically
- Failed videos are retried with exponential backoff on the next sync

### Download Notification
- A persistent foreground notification is shown while any download is active
- Displays "Downloading: {video title}" with a progress bar
- Includes a **Cancel** button that terminates the active yt-dlp process

---

## Per-Playlist Format Configuration

Accessible via the ⚙️ Settings icon on the playlist detail screen.

### Sync Mode
- **Audio only** — extracts audio from each video and saves as the chosen audio format
- **Video + Audio** — downloads and merges video and audio streams

### Audio Formats (when in Audio mode)
- m4a (default, AAC in M4A container)
- mp3
- opus (WebM/Opus)
- flac
- wav

### Video Containers (when in Video mode)
- mp4 (default)
- webm
- mkv

### Quality Presets (when in Video mode)
- Best available (default)
- 1080p cap
- 720p cap
- 480p cap

### Options
- **Embed thumbnail** (audio mode) — embeds album art into the audio file metadata
- **Embed subtitles** (video mode) — downloads and embeds English subtitles if available
- **Use aria2c** — enables multi-connection downloading for faster speeds on supported connections

### Advanced
- **Extra yt-dlp args** — raw flags appended verbatim to every yt-dlp call (e.g. `--no-overwrites`, `--cookies /sdcard/cookies.txt`)
- **Custom format string** — overrides all format settings above with a raw yt-dlp format selector (e.g. `bestvideo[height<=1080]+bestaudio/best`)

---

## Playlist Detail View

- Lists all known videos in the playlist with index, title, and duration
- Each video shows a **status badge**:
  - **Pending** (grey) — queued, not yet downloaded
  - **Downloading** (blue) — currently being downloaded, with a live progress bar
  - **Downloaded** (green) — file is available locally
  - **Error** (red) — last download attempt failed, shows the error message
- Header shows total downloaded/total count and last synced timestamp

---

## Storage

- Files are stored in app-private external storage — **no storage permissions required** on Android 8.0+
- Path: `Android/data/com.playlistsync/files/playlists/{playlistId}/{title} [{ytId}].{ext}`
- Files are removed automatically if the app is uninstalled

---

## yt-dlp Integration

- yt-dlp is bundled inside the APK via the `youtubedl-android` library (no separate install required)
- FFmpeg and aria2c are also bundled for post-processing and multi-connection downloads
- yt-dlp can be updated in-place without an app update (uses the library's built-in update mechanism)
- No Google account, API keys, or external services are required

---

## Permissions

| Permission | Reason |
|---|---|
| `INTERNET` | Fetch playlist metadata and download media |
| `FOREGROUND_SERVICE` | Show download notification while downloading |
| `FOREGROUND_SERVICE_DATA_SYNC` | Required on Android 14+ for data-sync foreground services |
| `POST_NOTIFICATIONS` | Show download progress notification (requested at runtime on Android 13+) |
| `WAKE_LOCK` | Keep CPU active during downloads when screen is off |

---

## Platform Requirements

- **Minimum Android version**: 8.0 (API 26)
- **Target Android version**: 14 (API 35)
- Tested architecture: arm64-v8a (native yt-dlp binary is bundled per ABI)
