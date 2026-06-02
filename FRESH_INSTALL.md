# Fresh install checklist (Helper + Shizuku)

Use this after uninstalling both apps. Goal: confirm the helper **does not crash**, then add Shizuku step by step.

## 1. Install Helper only (no Shizuku yet)

1. Android Studio: **Run → Always install with package manager** (required before Shizuku later).
2. Install and open **Call OS Helper**.
3. Complete setup wizard (permissions, battery, **Mes Validation Connection** if you use call monitoring).
4. Open **Dashboard**.

**Expected:** App stays open. Shizuku section shows **not installed** or **not running** — that is normal, not a crash.

**Logcat** (filter `HelperApplication`):

```text
[App] HelperApplication process started
[Setup] MainActivity onCreate
```

**Crash check:** If the app closes instantly, search Logcat for:

```text
UNCAUGHT CRASH
FATAL EXCEPTION
AndroidRuntime
```

There should be **no** `UNCAUGHT CRASH` from our handler if the app is stable.

---

## 2. Install and start Shizuku

1. Install [Shizuku](https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api) from Play Store.
2. Enable **Developer options** → **Wireless debugging** (Android 11+).
3. Pair once, then **Start** Shizuku until the app says it is **running**.
4. **Settings → Apps → Shizuku → Battery → Unrestricted**.
5. **Settings → Apps → Call OS Helper → Battery → Unrestricted**.

---

## 3. Connect Helper to Shizuku

1. Open **Call OS Helper → Dashboard**.
2. Tap **OPEN SHIZUKU APP** if needed, then return to Helper.
3. When prompted, **Allow** Shizuku permission for Helper.
4. Wait up to **30 seconds** (or tap **RETRY SHIZUKU CONNECTION**).

**Success Logcat:**

```text
[Shizuku] user_service_created | ctor=context ...
[Shizuku] call#- | user_service_connected | binderAlive=true
```

**Not a crash — bind timeout:**

```text
[Shizuku] bind timeout: user service still disconnected after 30s
```

App should **keep running**; only the privileged subprocess failed to start.

---

## 4. Test recording (without Shizuku)

Shizuku is optional. You can test first:

1. Turn **Speaker boost** ON on Dashboard.
2. Place a test call with **loudspeaker**.
3. Check `Documents/MesValidationCallRecorder` for a new `.amr` file.

---

## 5. Enable / disable in Shizuku Manager

| Action in Shizuku | What you see | What to do |
|-------------------|--------------|------------|
| **Disable** Helper | Logcat: `PROCESS ENDED (…helper_application)` | **Normal** — Shizuku revoked access and killed the process |
| **Enable** Helper again | Maybe **no new logs** if app stayed closed | **Force stop** Helper → open Helper → Dashboard (wait ~20s per attempt; after **5** failures, ROM-blocked message appears) |

Shizuku sends the binder when the app **process starts**, not always when you flip the toggle. **Enable in Shizuku ≠ automatic reconnect** until you reopen (or force-stop) Helper.

---

## 6. If you think the app crashed

| Symptom | Likely cause |
|---------|----------------|
| App vanishes to home | Real crash — capture `FATAL EXCEPTION` log |
| App open but Shizuku says Connecting | **Not a crash** — bind timeout / Shizuku stopped |
| `Channel is unrecoverably broken` in log | Activity closed (back button / rotation), not always app crash |
| Shizuku was killed | Reopen Shizuku app → Start again |

**ADB (optional):**

```bash
adb logcat -s AndroidRuntime:E HelperApplication:I
```

---

## 6. Order summary

```text
Install Helper → complete setup → confirm no crash
Install Shizuku → start server → unrestricted battery (both apps)
Open Helper → grant Shizuku permission → wait for Connected
Test call with speaker boost (works even if Shizuku fails)
```

See also: [SHIZUKU_INTEGRATION.md](SHIZUKU_INTEGRATION.md), [README.md](README.md).
