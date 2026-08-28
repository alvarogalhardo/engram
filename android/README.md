# engram — Android client

Kotlin + Jetpack Compose spaced-repetition app (SM-2, Anki `.apkg` import,
Markdown/HTML cards, stats). Fully offline today; the sync client is milestone
M2 — see [../docs/roadmap.md](../docs/roadmap.md) and
[../docs/sync-protocol.md](../docs/sync-protocol.md).

```bash
./gradlew test assembleDebug        # needs JDK 17 + Android SDK 35 (local.properties)
python3 tools/make_fixture_apkg.py  # tiny .apkg fixture to exercise the importer
```

Release builds are signed only in CI (tag `android-v*`) with a keystore injected
from GitHub Secrets; local `assembleRelease` falls back to the debug signature.
