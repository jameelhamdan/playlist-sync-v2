Run tests for this project.

**Unit tests** (no emulator needed):
- `./scripts/test.sh` — all JVM unit tests
- `./scripts/test.sh DownloadRepositoryTest` — single class

**Download script** (real yt-dlp download, requires `yt-dlp` installed):
- `./scripts/download.sh` — audio, default playlist
- `./scripts/download.sh audio <url>` — audio, custom playlist
- `./scripts/download.sh video <url>` — video, custom playlist

After a unit test run, report how many tests passed/failed; on failure show the test name and assertion message. HTML report at `app/build/reports/tests/testDebugUnitTest/index.html`.

Artifacts for both test types are saved to the project `results/` directory with a datetime-stamped filename.
