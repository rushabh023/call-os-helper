# Full two-way call recording — architecture

The **Helper app alone cannot guarantee** two-way cellular audio on every phone: Google blocks `VOICE_CALL` for normal APKs. A **complete product** uses **three layers**:

## Layer 1 — Helper: two-way mode (implemented)

1. User taps **ENABLE TWO-WAY RECORDING** on the dashboard.
2. Android shows **MediaProjection** consent (capture device audio).
3. On each call, the helper tries engine **`dual_playback_mic_wav`** first:
   - **Playback track** — other party (what plays on the phone)
   - **Mic track** — your voice
   - Output: stereo **`.wav`** in `Documents/MesValidationCallRecorder`

**Requirements:** Android 10+, user must accept the projection prompt, re-enable after reboot if needed.

**Tips:** Use **speakerphone** if playback track is still weak on some Realme/Samsung devices.

## Layer 2 — Mes Validation main app (you must implement)

When `com.mesvalidation` is installed, the helper **stops local recording** and sends:

| Broadcast | Action |
|-----------|--------|
| `com.mesvalidation.action.START_RECORD` | Start recording in Mes Validation |
| `com.mesvalidation.action.STOP_RECORD` | Stop and save |
| `com.mesvalidation.action.RECORD_COMPLETE` | Helper saved a file locally (extras: `file_name`, `file_path`) — update DB/UI |

Copy the sample from `MesValidationRecordingReceiverSample.kt` into Mes Validation and implement a **foreground `RecordingService`** that:

1. Uses `MediaRecorder` with `VOICE_CALL` / `VOICE_COMMUNICATION` (works only if Mes Validation is signed as system/priv-app or OEM allows).
2. Or uses the same **dual playback + mic** approach with MediaProjection held in Mes Validation.
3. Saves to the **same folder**: `Documents/MesValidationCallRecorder`.
4. Optionally replies with broadcast `com.mesvalidation.action.RECORDING_STATUS` so the helper dashboard can show status.

### Mes Validation manifest (privileged path — OEM / system app only)

```xml
<!-- Only if Mes Validation is installed as system/privileged partner -->
<uses-permission android:name="android.permission.CAPTURE_AUDIO_OUTPUT"
    tools:ignore="ProtectedPermissions" />
```

Without system privileges, use **dual capture + MediaProjection** inside Mes Validation (same as helper).

## Layer 3 — OEM / enterprise (100% on supported devices)

- Samsung Knox / dialer integration  
- Signed partnership with Realme/Oppo dialer APIs  
- Default dialer + `InCallService` (helper becomes phone app — large project)

---

## What to build next in Mes Validation

| Priority | Task |
|----------|------|
| P0 | `MesValidationRecordingReceiver` + `CallRecordingService` |
| P0 | Save to `MesValidationCallRecorder` (same path as helper) |
| P1 | Hold MediaProjection in Mes Validation for dual capture |
| P2 | OEM-specific hooks if you have device partnerships |

## Testing checklist

1. Helper only: enable two-way → cellular call → play `.wav` (L/R channels).
2. Mes Validation installed: verify helper logs `START_RECORD` / `STOP_RECORD` and file appears from main app.
3. Logcat tag `HelperApplication`, engines: `dual_playback_mic_wav`, `DUAL_PLAYBACK+MIC`.

Helper repo: [README.md](README.md)
