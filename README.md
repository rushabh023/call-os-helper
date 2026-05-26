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
