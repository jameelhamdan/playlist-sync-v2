#!/usr/bin/env bash
# Downloads a full playlist exactly as the app would — same yt-dlp flags as
# DownloadRepository.downloadVideo(), same config defaults as PlaylistConfig.
#
# Requires: yt-dlp  (brew install yt-dlp)
#
# Usage:
#   ./scripts/test-download.sh                          → audio, default playlist
#   ./scripts/test-download.sh audio <url>              → audio, custom playlist
#   ./scripts/test-download.sh video <url>              → video, custom playlist
#
# Config (edit below or override via env vars):
#   MODE              audio | video               (default: audio)
#   AUDIO_FORMAT      m4a | mp3 | opus | flac     (default: m4a)
#   VIDEO_QUALITY     best | 1080p | 720p | 480p  (default: best)
#   VIDEO_CONTAINER   mp4 | mkv | webm            (default: mp4)
#   EMBED_THUMBNAIL   true | false                (default: true)
#   CONCURRENT        1-5                         (default: 2)
set -euo pipefail

# ── config defaults (mirrors PlaylistConfig / AppSettings) ───────────────────
DEFAULT_PLAYLIST="https://www.youtube.com/playlist?list=PL59FEE129ADFF2B12"
MODE="${1:-audio}"
PLAYLIST_URL="${2:-$DEFAULT_PLAYLIST}"

AUDIO_FORMAT="${AUDIO_FORMAT:-m4a}"
VIDEO_QUALITY="${VIDEO_QUALITY:-best}"
VIDEO_CONTAINER="${VIDEO_CONTAINER:-mp4}"
EMBED_THUMBNAIL="${EMBED_THUMBNAIL:-true}"
CONCURRENT="${CONCURRENT:-2}"

# Extractor args — must match DownloadRepository exactly
EXTRACTOR_ARGS="youtube:player_client=ios,web"

# ── paths ────────────────────────────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
TIMESTAMP=$(date '+%Y%m%d_%H%M%S')
ARTIFACT_DIR="$ROOT_DIR/results"
ARTIFACT_FILE="$ARTIFACT_DIR/${TIMESTAMP}-download.log"
DOWNLOAD_DIR="$ARTIFACT_DIR/${TIMESTAMP}-files"

mkdir -p "$ARTIFACT_DIR" "$DOWNLOAD_DIR"

# ── helpers ──────────────────────────────────────────────────────────────────
log()  { echo "$*" | tee -a "$ARTIFACT_FILE"; }
fail() { log "FAIL: $*"; exit 1; }
pass() { log "PASS: $*"; }

check_ytdlp() {
  command -v yt-dlp &>/dev/null || fail "yt-dlp not found — brew install yt-dlp"
  log "yt-dlp : $(yt-dlp --version)"
}

# ── format string (mirrors DownloadRepository.buildFormatString) ──────────────
build_format_string() {
  if [[ "$MODE" == "audio" ]]; then
    echo "bestaudio/best"
    return
  fi
  local h="${VIDEO_QUALITY%p}"
  case "$h" in
    1080) echo "bestvideo[height<=1080]+bestaudio/bestvideo[height<=1080]/best[height<=1080]/best" ;;
    720)  echo "bestvideo[height<=720]+bestaudio/bestvideo[height<=720]/best[height<=720]/best" ;;
    480)  echo "bestvideo[height<=480]+bestaudio/bestvideo[height<=480]/best[height<=480]/best" ;;
    360)  echo "bestvideo[height<=360]+bestaudio/bestvideo[height<=360]/best[height<=360]/best" ;;
    *)    echo "bestvideo+bestaudio/best" ;;
  esac
}

# ── fetch all video ids from playlist ────────────────────────────────────────
fetch_metadata() {
  log ""
  log "=== Fetching playlist metadata ==="
  log "URL: $PLAYLIST_URL"

  local raw
  raw=$(yt-dlp \
    --flat-playlist \
    --dump-single-json \
    --no-warnings \
    --socket-timeout 30 \
    --retries 5 \
    --extractor-args "$EXTRACTOR_ARGS" \
    "$PLAYLIST_URL" 2>&1) || { log "$raw"; fail "Metadata fetch failed"; }

  PLAYLIST_ID=$(echo "$raw"    | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('id','unknown'))")
  PLAYLIST_TITLE=$(echo "$raw" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('title','Unknown'))")
  CHANNEL_NAME=$(echo "$raw"   | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('uploader') or d.get('channel',''))")

  # Build arrays of IDs and titles
  VIDEO_IDS=()
  while IFS= read -r line; do VIDEO_IDS+=("$line"); done < <(echo "$raw" | python3 -c "
import sys, json
d = json.load(sys.stdin)
for e in d.get('entries', []):
  if e.get('id'): print(e['id'])
")
  VIDEO_TITLES=()
  while IFS= read -r line; do VIDEO_TITLES+=("$line"); done < <(echo "$raw" | python3 -c "
import sys, json
d = json.load(sys.stdin)
for e in d.get('entries', []):
  if e.get('id'): print(e.get('title','Untitled'))
")

  log "Playlist : $PLAYLIST_TITLE  [$PLAYLIST_ID]"
  log "Channel  : $CHANNEL_NAME"
  log "Videos   : ${#VIDEO_IDS[@]}"
  pass "Metadata fetch"
}

# ── download a single video (mirrors downloadVideo in DownloadRepository) ────
download_one() {
  local yt_id="$1"
  local title="$2"
  local track_num="$3"
  local out_dir="$4"
  local format_str
  format_str=$(build_format_string)

  log ""
  log "  [$track_num/${#VIDEO_IDS[@]}] $title  ($yt_id)"

  local url="https://www.youtube.com/watch?v=$yt_id"

  local base_args=(
    -f "$format_str"
    -o "$out_dir/%(title)s [%(id)s].%(ext)s"
    --no-playlist
    --no-warnings
    --retries 5
    --fragment-retries 5
    --socket-timeout 30
    --extractor-args "$EXTRACTOR_ARGS"
  )

  if [[ "$MODE" == "audio" ]]; then
    local thumb_args=()
    if [[ "$EMBED_THUMBNAIL" == "true" ]] && [[ "$AUDIO_FORMAT" =~ ^(m4a|opus|flac|wav)$ ]]; then
      thumb_args=(--embed-thumbnail)
    fi
    yt-dlp "${base_args[@]}" \
      -x \
      --audio-format "$AUDIO_FORMAT" \
      --audio-quality 0 \
      "${thumb_args[@]}" \
      --add-metadata \
      --parse-metadata "%(uploader)s:%(meta_artist)s" \
      --postprocessor-args "ffmpeg:-metadata 'album_artist=${CHANNEL_NAME//\'/}' -metadata 'album=${PLAYLIST_TITLE//\'/}' -metadata track=$track_num" \
      "$url" 2>&1 | tee -a "$ARTIFACT_FILE"
  else
    yt-dlp "${base_args[@]}" \
      --merge-output-format "$VIDEO_CONTAINER" \
      "$url" 2>&1 | tee -a "$ARTIFACT_FILE"
  fi
}

# ── download all videos with concurrency limit ────────────────────────────────
download_all() {
  local out_dir="$DOWNLOAD_DIR/$MODE"
  mkdir -p "$out_dir"

  log ""
  log "=== Downloading ${#VIDEO_IDS[@]} videos ==="
  log "Mode      : $MODE"
  if [[ "$MODE" == "audio" ]]; then
    log "Format    : $AUDIO_FORMAT  |  embed-thumbnail: $EMBED_THUMBNAIL"
  else
    log "Quality   : $VIDEO_QUALITY  |  container: $VIDEO_CONTAINER"
  fi
  log "Concurrent: $CONCURRENT"
  log ""

  local running=0
  local failed=0

  for i in "${!VIDEO_IDS[@]}"; do
    local yt_id="${VIDEO_IDS[$i]}"
    local title="${VIDEO_TITLES[$i]}"
    local track_num=$((i + 1))

    # Wait if we're at the concurrency limit
    while (( running >= CONCURRENT )); do
      wait -n 2>/dev/null && (( running-- )) || true
    done

    download_one "$yt_id" "$title" "$track_num" "$out_dir" &
    (( running++ ))
  done

  # Wait for remaining jobs
  wait

  local file_count
  file_count=$(find "$out_dir" -type f | wc -l | tr -d ' ')
  log ""
  log "Downloaded : $file_count / ${#VIDEO_IDS[@]} files"
  log "Output dir : $out_dir"
  pass "All downloads complete"
}

# ── main ──────────────────────────────────────────────────────────────────────
{
  echo "=== PlaySync download test ==="
  echo "Date     : $(date '+%Y-%m-%d %H:%M:%S')"
  echo "Mode     : $MODE"
  echo "Playlist : $PLAYLIST_URL"
  echo "=============================="
} | tee "$ARTIFACT_FILE"

[[ "$MODE" == "audio" || "$MODE" == "video" ]] \
  || fail "Unknown mode '$MODE'. Use: audio | video"

check_ytdlp
fetch_metadata
download_all

{
  echo ""
  echo "=============================="
  echo "Log   : $ARTIFACT_FILE"
  echo "Files : $DOWNLOAD_DIR"
} | tee -a "$ARTIFACT_FILE"
