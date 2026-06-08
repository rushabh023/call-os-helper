# Call OS Dialer (`:dialer`)

Standalone default-dialer app module with integrated call recording (no Shizuku).

## Build & run

1. Open the repo in Android Studio.
2. Select the **`dialer`** run configuration (module `com.example.dialer`).
3. **Run → Always install with package manager** (recommended for Telecom/InCallService).
4. On first launch you see a **blocking gate** — the app stays locked until **Call OS Dialer** is set as the default phone app (no skip).
5. Grant phone, contacts, call log, microphone, and notification permissions.

## Critical path (implemented)

| Component | Package |
|-----------|---------|
| `MyInCallService` | `feature.incall` |
| `RecordingManager` | `feature.recordings` |
| `DialpadFragment` | `feature.dialpad` |
| Room DB | `core.data.local` |

## Recording

- Tries `VOICE_CALL` → downlink/uplink → `MIC` at runtime.
- Saves M4A under `Music/Recordings/Dialer/` (public recordings folder).
- In-call **Record** button + optional auto-record in Settings.
- Legal disclaimer on first record.

## Next phases (not yet in UI)

- Swipe answer/decline gestures
- Conference merge/swap
- Speed-dial management screen
- Blocklist CSV import UI
- Visual voicemail
- Full theme/accent picker

## Tests

```bash
./gradlew :dialer:testDebugUnitTest
```
