#!/usr/bin/env bash
# Run instrumented tests on a connected device or emulator.
# These call real DownloadRepository Kotlin code via the actual youtubedl-android library.
#
# Requires: adb-connected device or running emulator
# Usage:    ./scripts/test-instrumented.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
TIMESTAMP=$(date '+%Y%m%d_%H%M%S')
ARTIFACT_DIR="$ROOT_DIR/results"
ARTIFACT_FILE="$ARTIFACT_DIR/${TIMESTAMP}-instrumented.log"

mkdir -p "$ARTIFACT_DIR"

{
  echo "=== PlaySync instrumented test ==="
  echo "Date   : $(date '+%Y-%m-%d %H:%M:%S')"
  echo "Device : $(adb devices | grep -v 'List' | head -1 || echo 'none detected')"
  echo "=================================="
} | tee "$ARTIFACT_FILE"

cd "$ROOT_DIR"

./gradlew :app:connectedDebugAndroidTest 2>&1 | tee -a "$ARTIFACT_FILE"

{
  echo ""
  echo "=================================="
  echo "Log : $ARTIFACT_FILE"
  echo "HTML: app/build/reports/androidTests/connected/debug/index.html"
} | tee -a "$ARTIFACT_FILE"
