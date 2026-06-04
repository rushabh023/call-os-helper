# Call OS Helper

Android helper app that works with **Mes Validation** (`com.mesvalidation`) for call recording setup—similar to the Cube ACR + Cube ACR Helper pattern.

**GitHub:** https://github.com/rushabh023/call-os-helper  
**How this repo is shared, branched, and cloned:** see [GITHUB.md](GITHUB.md)

## Features

- Setup wizard: permissions, battery optimization, Accessibility **Mes Validation Connection**
- Detects incoming/outgoing calls and signals the main Mes Validation app
- Creates folder: `Internal storage/Documents/MesValidationCallRecorder`
- Foreground call monitor service (Android 12+ safe)

## Requirements

- Android Studio (Ladybug or newer recommended)
- JDK 17+ (bundled with Android Studio)
- Mes Validation main app (`com.mesvalidation`) for full record/save flow

## Call audio limitations (important)

Since **Android 9+**, normal apps cannot reliably record **both sides** of a cellular call. The helper tries several audio sources (`VOICE_COMMUNICATION`, `VOICE_RECOGNITION`, `MIC`, etc.), but on Samsung, Realme, and most stock ROMs you may get:

- A valid `.m4a` / `.wav` file with **no audible conversation** (silence or noise only), or  
- **Only your voice** (microphone), not the other person.

**What helps on many devices:** turn on **speakerphone** during the test call so the mic can pick up sound from the loudspeaker.

**Two-way mode in this helper (Android 10+):** Dashboard → **ENABLE TWO-WAY RECORDING** → accept system prompt → calls save as stereo **WAV** (playback + mic).

**Shizuku two-way (ACR-style, recommended on Realme/Samsung):**

Full integration steps (manifest, binder, permission, UserService, OEM timing): **[SHIZUKU_INTEGRATION.md](SHIZUKU_INTEGRATION.md)**.  
**Fresh reinstall (both apps removed):** **[FRESH_INSTALL.md](FRESH_INSTALL.md)**.

1. Install and start [Shizuku from Play Store](https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api) (`moe.shizuku.privileged.api`) — this is what Call OS Helper targets by default ([setup guide](https://shizuku.rikka.app/guide/setup.html)). NLL Store Shizuku is optional and only needed if you deliberately use APH with that fork.
2. Android Studio: **Run → Always install with package manager** when debugging.
3. Open Call OS Helper → Dashboard → **OPEN SHIZUKU APP** → start Shizuku → grant permission → app auto-tries **4** binds; **Ready** or ROM-blocked message.
4. Place a test call; logs should show `user_service_connected` and engine `shizuku_voice_call`.

Engine priority: **Shizuku** → dual MediaProjection WAV → MediaRecorder M4A → WAV mic.

**Industry references (same problem, same patterns):**

| Approach | Link |
|----------|------|
| ACR Phone + Helper (Shizuku / Accessibility) | [acr.app](https://acr.app/) · [ACRPhoneHelper](https://github.com/NLLAPPS/ACRPhoneHelper) — public repo is Accessibility-only; Shizuku build uses NLL fork |
| Shizuku (privileged APIs without root) | [User manual](https://github.com/NLLAPPS/Shizuku/wiki/User-manual) · [thedjchi fork](https://github.com/thedjchi/Shizuku) |
| Open-source source ladder (MIC fallback) | [axet/android-call-recorder](https://gitlab.com/axet/android-call-recorder) |

See [CALL_RECORDING_OPTIONS.md](CALL_RECORDING_OPTIONS.md) for engine details. See [MES_VALIDATION_RECORDING.md](MES_VALIDATION_RECORDING.md) for the Mes Validation main app.

## Build

```bash
./gradlew assembleDebug
```

Install:

```bash
./gradlew installDebug
```

## Mes Validation main app

The helper sends broadcasts to Mes Validation:

- `com.mesvalidation.action.START_RECORD`
- `com.mesvalidation.action.STOP_RECORD`

Implement handling in the Mes Validation app (see `MesValidationRecordingReceiver` in MESValidationMobile).

## Logs (Logcat)

**Filter tag:** `HelperApplication`

In Android Studio Logcat: `tag:HelperApplication`  
On device (USB): `adb logcat -s HelperApplication`

Structured lines include a **call session** id (`call#1`, `call#2`, …) so you can follow one phone call end-to-end.

### Log categories

| Prefix | What it logs |
|--------|----------------|
| `[Diagnostics]` | Device snapshot (SDK, manufacturer, model) + recording capability matrix |
| `[Telephony]` | Call states, direction, Mes Validation vs local path |
| `[Recording]` | CallMonitorService begin/end/save, FGS, session start/end |
| `[Engine]` | Engine ladder, per-engine try/fail, audio source used |
| `[Shizuku]` | Binder, permission, bind, shell process sources, two-way M4A |
| `[Storage]` | Folder ensure, save strategy (direct vs MediaStore), paths |
| `[Bridge]` | Broadcasts to Mes Validation (if installed) |
| `[Setup]` | Permissions, accessibility, boot |
| `[Dashboard]` | Status refresh, Shizuku/dual-capture flags |

### App startup (once per process)

```
[Diagnostics] call#- | device_snapshot | sdk=34 release=14 manufacturer=realme model=RMX3933 ...
[Diagnostics] call#- | recording_capabilities | shizukuInstalled=true shizukuBinder=true ...
[Shizuku] [Shizuku] ShizukuManager initialized
[Shizuku] call#- | init | installed=true binderPing=true permission=false ...
```

### Shizuku setup (Dashboard → SETUP SHIZUKU RECORDING)

```
[Shizuku] User tapped SETUP SHIZUKU RECORDING
[Shizuku] call#- | setup_tap | binderPing=true permission=false serviceConnected=false ready=false
[Shizuku] Requesting Shizuku permission (code=9001)
[Shizuku] call#- | permission_result | granted=true
[Shizuku] call#- | bind_requested | component=com.example.helper_application/... processSuffix=shizuku_recording
[Shizuku] call#- | user_service_connected | binderAlive=true recordingInterface=true
[Shizuku] call#- | shell_recording_started | audioSource=VOICE_CALL ...   (during a call)
```

### Expected flow — local recording with Shizuku (Mes Validation **not** installed)

```
[Telephony] call#- | state_change | state=RINGING ...
[Telephony] Call answered/active OFFHOOK → auto-start recording (INCOMING)
[Recording] Call session #1 started
[Recording] call#1 | begin | direction=INCOMING number=hidden
[Engine] call#1 | engine_ladder | order=shizuku_voice_call → media_recorder_mic → ...
[Engine] Trying engine 1/4: shizuku_voice_call
[Shizuku] call#1 | engine_started | audioSource=VOICE_CALL tempPath=... bytes=0
[Recording] call#1 | active | engine=shizuku_voice_call extension=m4a tempBytes=...
[Telephony] call#- | state_change | state=IDLE ...
[Recording] call#1 | end | direction=INCOMING targetFolder=Documents/MesValidationCallRecorder
[Shizuku] call#1 | shell_recording_stopped | audioSource=VOICE_CALL bytes=484120
[Storage] call#1 | save_start | fileName=20260527_120000_IN_unknown.m4a tempBytes=484120 ...
[Storage] call#1 | save_direct_ok | fileManager=Internal storage/Documents/MesValidationCallRecorder/...
[Recording] call#1 | save_ok | fileName=... fileManagerPath=Internal storage/Documents/MesValidationCallRecorder/...
[Recording] Call session #1 ended
```

### Troubleshooting from logs

| Symptom | Look for |
|---------|----------|
| Silent .m4a | `audioSource=MIC` or `empty_capture` — enable Shizuku or speakerphone |
| Shizuku not used | `Shizuku excluded` in Engine list — fix binder/permission |
| Service not connected | `user service not connected yet` — open Dashboard, wait for `ready=true` |
| Shizuku bind timeout (30s) | No `user_service_created` — subprocess did not start or ctor never ran. **Rebuild** after fixes: `HelperApplication` skips full init in `:shizuku_recording` process; `HelperShizukuProvider` is lightweight. On PC: `adb logcat -s ShizukuServiceStarter:* HelperApplication:I` — look for `provider is null`, `unable to start service`. Success: `user_service_created \| ctor=context` and `user_service_connected`. Studio: **Always install with package manager**. In Shizuku app: revoke + re-grant permission if `provider is null` (Shizuku #451). |
| Save failed | `[Storage] saveVia*` errors — storage permission or folder path |
| Stuck recording | `[Recording] active` without matching `end` / `save_ok` |

### File manager path (from logs)

Successful saves include:

`fileManager=Internal storage/Documents/MesValidationCallRecorder/<filename>.m4a` or `.wav`

---

## GitHub & distribution

| Item | Value |
|------|--------|
| Repo | [rushabh023/call-os-helper](https://github.com/rushabh023/call-os-helper) |
| Visibility | **Public** |
| Clone | `git clone https://github.com/rushabh023/call-os-helper.git` |
| Branch | `rushabh-call-os-helper` |

**Branches**

| Branch | Use |
|--------|-----|
| `main` | Default / stable |
| `rushabh-call-os-helper` | Working branch |

**Clone this branch**

```bash
git clone -b rushabh-call-os-helper https://github.com/rushabh023/call-os-helper.git
```

**Push your changes to this branch**

```bash
git checkout rushabh-call-os-helper
git add .
git commit -m "Your message"
git push origin rushabh-call-os-helper
```

Anyone with the link can **view and clone** the code. Only you (and collaborators you add) can **push** to this repository.

Full details: [GITHUB.md](GITHUB.md)
