#!/usr/bin/env bash
# Run JVM unit tests — no emulator or device needed.
# Usage: ./scripts/test.sh [optional filter]
#   ./scripts/test.sh                          → all tests
#   ./scripts/test.sh DownloadRepositoryTest   → single class
#
# Artifacts: each run appends a timestamped log to /tmp/playsync-test-runs/
set -euo pipefail

FILTER="${1:-}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
TIMESTAMP=$(date '+%Y%m%d_%H%M%S')
ARTIFACT_DIR="$ROOT_DIR/results"
ARTIFACT_FILE="$ARTIFACT_DIR/$TIMESTAMP.log"

mkdir -p "$ARTIFACT_DIR"

{
  echo "=== PlaySync test run ==="
  echo "Date : $(date '+%Y-%m-%d %H:%M:%S')"
  echo "Filter: ${FILTER:-<all>}"
  echo "========================="
  echo ""
} | tee "$ARTIFACT_FILE"

EXIT_CODE=0
if [[ -n "$FILTER" ]]; then
  ./gradlew :app:testDebugUnitTest --tests "com.playlistsync.*.$FILTER" 2>&1 \
    | tee -a "$ARTIFACT_FILE" || EXIT_CODE=$?
else
  ./gradlew :app:testDebugUnitTest 2>&1 \
    | tee -a "$ARTIFACT_FILE" || EXIT_CODE=$?
fi

{
  echo ""
  echo "========================="
  echo "Exit code : $EXIT_CODE"
  echo "Log      : $ARTIFACT_FILE"
  echo "HTML report: app/build/reports/tests/testDebugUnitTest/index.html"
} | tee -a "$ARTIFACT_FILE"

exit $EXIT_CODE
