# Call recording options (industry comparison)

How popular solutions relate to **Call OS Helper** + **Mes Validation**, and what to build next.

## Your links — what each does

| Solution | Link | Role | Two-way on Android 10+? |
|----------|------|------|-------------------------|
| **ACR (Cube pattern)** | [acr.app](https://acr.app/) | Main dialer on Play Store + **Helper APK** sideloaded (APH) | Yes, **with Shizuku** or Accessibility / root |
| **Shizuku** | [User manual](https://github.com/NLLAPPS/Shizuku/wiki/User-manual) | Grants **shell UID (2000)** so apps can use privileged APIs | Enables real call audio when integrated |
| **thedjchi/Shizuku** | [GitHub](https://github.com/thedjchi/Shizuku) | Fork used by ACR / NLL Store | Same as Shizuku |
| **axet Call Recorder** | [GitLab](https://gitlab.com/axet/android-call-recorder) | FOSS app; tries **VOICE_CALL / UPLINK / DOWNLINK** then falls back to MIC | Only if ROM allows; most phones → MIC only |

### ACR model (same as yours)

```
┌─────────────────┐     broadcasts / IPC      ┌──────────────────────┐
│  ACR Phone      │ ◄──────────────────────── │  ACR Phone Helper    │
│  (Play Store)   │                           │  (sideloaded APK)    │
│  com.nll.cb     │                           │  Shizuku / A11y      │
└─────────────────┘                           └──────────────────────┘
```

**Call OS Helper** = APH  
**Mes Validation** = ACR Phone  

Google blocks call audio in Play Store apps → helper is sideloaded. See [ACR why page](https://acr.app/).

### Shizuku (recommended path for real two-way)

- User installs **Shizuku** (or NLL/thedjchi fork), starts it (wireless debugging or PC adb).
- Helper talks to Shizuku and runs recording code in a **shell-privileged** context.
- `AudioRecord` / `MediaRecorder` can use **VOICE_CALL**, **VOICE_DOWNLINK**, **VOICE_UPLINK** with `CAPTURE_AUDIO_OUTPUT` (shell has it).
- Works with **Bluetooth / wired headset** on many devices (ACR’s main selling point vs Accessibility-only).

References:

- [ACRPhoneHelper source](https://github.com/NLLAPPS/ACRPhoneHelper) (companion app, not full Shizuku client)
- [ShizuCallRecorder](https://github.com/kitsumed/ShizuCallRecorder) — FOSS example using Shizuku + scrcpy-server
- [cally](https://github.com/spoofer8/cally) — `com.android.shell` attribution via Shizuku UserService

### axet (android-call-recorder)

- Open source ([GPLv3](https://gitlab.com/axet/android-call-recorder)).
- Cycles **all** `MediaRecorder.AudioSource` values; if line recording fails → **MIC**.
- **No Shizuku** — on Samsung/Realme/Android 10+ you often get silence or one side only.
- Good reference for **source fallback ladder** (we already do this in `RecordingAudioSources.kt`).

### Root / Magisk (ACR option)

[ACR Magisk module](https://acr.app/) — privileged install without Shizuku setup. Same idea as system app: grant `CAPTURE_AUDIO_OUTPUT`.

---

## What Call OS Helper has today

| Method | Status |
|--------|--------|
| Telephony detect (RINGING/OFFHOOK/IDLE) | ✅ |
| Accessibility “Mes Validation Connection” | ✅ (like APH Accessibility) |
| Mes Validation broadcasts | ✅ (main app must implement) |
| Audio source ladder (VOICE_CALL → … → MIC) | ✅ |
| Dashboard **Two-way** (MediaProjection playback + mic WAV) | ✅ |
| **Shizuku** | ✅ Dashboard → SETUP SHIZUKU RECORDING |
| Root / Magisk module | ❌ |

---

## Recommended product roadmap (full two-way)

### Phase A — Done

- Helper + folder + local save + dual MediaProjection mode.
- [MES_VALIDATION_RECORDING.md](MES_VALIDATION_RECORDING.md) for main app work.

### Phase B — Shizuku ✅ (implemented)

1. Dependencies: `dev.rikka.shizuku:api` + `provider` 13.1.5
2. `ShizukuRecordingUserService` — shell process, `VOICE_CALL` source ladder
3. Dashboard: **SETUP SHIZUKU RECORDING**
4. Engine order: Shizuku → Dual MediaProjection → MediaRecorder → WAV

**User setup:** Install [Shizuku (Play Store)](https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api) → start (wireless debugging) → Dashboard **OPEN SHIZUKU APP** → return to helper for permission.

### Phase C — Mes Validation main app

- Implement `MesValidationRecordingReceiver` + foreground service.
- Can use same Shizuku stack inside Mes Validation for Play Store dialer users.

### Phase D — Optional

- Magisk module for enterprise/root users (like ACR).
- OEM partnerships (Samsung Knox, etc.).

---

## Realme RMX3933 notes

- Use **Shizuku + wireless debugging** (Android 14/15) for best chance of two-way.
- Disable battery optimization for Helper + Shizuku.
- **Allow restricted settings** for Accessibility (setup screen).
- Wi‑Fi calling: ACR warns both sides may fail (OEM driver) — test cellular first.

---

## Legal / distribution

- **Play Store** main app cannot ship working call recorder (Google policy since 2022).
- Helper must be **sideloaded** (your GitHub / NLL Store style), same as [acr.app](https://acr.app/).

---

## Next step for development

If you want the **same class of solution as ACR**, the next code task is **Phase B: Shizuku integration** in this repo. Say the word and we can add the Shizuku dependency, setup UI, and a `ShizukuRecordingEngine` scaffold based on open references above.
