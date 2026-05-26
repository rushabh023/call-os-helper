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

## Logs

Logcat filter: `HelperApplication`
