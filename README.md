# RamGuard

A Magisk module that keeps games smooth on low-RAM Android devices. It frees background apps
when a game comes to the foreground, provides a hidden notification control panel, manages
[RewardSkip](https://github.com/OPencode/RewardSkip) from one place, and safely expands swap
with a second zram device at boot.

```
Copyright (C) 2026 RamGuard contributors — GPL-2.0-or-later
```

---

## Features

- **Foreground game detection** — watches `dumpsys activity activities` and, when one of the
  configured target games becomes foreground, force-stops the configured background apps.
- **Hidden manager** — a systemless app with **no launcher icon**. It is opened by tapping the
  persistent, non-clearable `RamGuard active` notification.
- **RewardSkip integration** — from the notification menu you can:
  - add/remove games to RewardSkip's Xposed scope (writes directly to the LSPosed/Vector DB)
  - switch RewardSkip between **instant** and **delayed** reward-skip modes
- **Safe zram expansion** — hot-adds `zram1` (1 GiB) alongside the existing `zram0`, giving
  ~2.4 GiB of swap without resizing or disabling the live device.
- **Watchdog** — restarts the manager's foreground service if Android kills it under memory
  pressure.
- **No root app required** — the manager only reads/writes files in its own private directory;
  a root daemon living in the Magisk module does the privileged work.

---

## Architecture

```
┌──────────────────────┐   writes request files   ┌────────────────────────┐
│  Hidden manager APK  │ ────────────────────────► │  Rust root daemon      │
│  com.opencode.ramguard│  (its own private dir)   │  /data/adb/modules/... │
└──────────────────────┘                           └───────────┬────────────┘
          ▲                                                   │ sqlite3 / am / dumpsys
          │ STATUS + scope list                               ▼
          └──────────────────────────────────  LSPosed DB, foreground service, swap
```

The manager and daemon never talk over a socket. The manager writes small text files into its
private `files/` directory (`config.txt`, `scope_request.txt`, `scope_remove.txt`,
`skip_mode.txt`). The daemon polls them on the configured interval and applies the changes.

The daemon mirrors the mode into `/data/local/tmp/skip_mode.txt` (mode `0644`) so that the
RewardSkip Xposed hooks, running inside unprivileged game processes, can read it.

---

## Installation

1. Flash `RamGuard.zip` in Magisk.
2. Reboot.
3. Open the `RamGuard active` notification (grant the notification permission if prompted).
4. Configure target games, the check interval and the background kill list from the menu.

The manager is installed systemlessly to `/system/priv-app/RamGuardManager`, so uninstalling
it from Settings is not possible — remove the Magisk module instead.

---

## Configuration

Configuration lives in the manager's private storage and can also be edited in
`module/config.txt` before flashing:

```ini
# Games that trigger memory cleanup when they become foreground
target=droidhang.twgame.restaurant
target=com.playgendary.bowmastersremastered

# Polling interval in seconds
interval=3

# Background apps to force-stop on trigger
kill=com.facebook.katana
kill=com.facebook.orca
kill=app.revanced.android.youtube
kill=com.android.vending
kill=com.google.android.play.games
```

Multiple `target=` lines are supported. Avoid adding system-critical apps (GMS, microG,
Magisk, Vector/LSPosed, AdAway) to the kill list — doing so can break logins or root
management. The manager hides them from the app pickers by default; use the `⋮` menu to reveal
them if you really need to.

---

## RewardSkip scope and mode

RamGuard discovers the RewardSkip module by its package name (`com.opencode.rewardskip`)
and edits its scope directly. It prefers the framework's SQLite database (`sqlite3`), and
falls back to the **Vector CLI** (`/data/adb/modules/zygisk_vector/cli`) on devices that do
not ship a `sqlite3` binary — both LSPosed and Vector are supported. The menu shows:

- **REWARDSKIP · GAME SCOPE** — add or remove games from the Xposed scope
- **REWARDSKIP MODE** — switch between `instant` and `delayed`

Scope changes made through the DB may be cached by Vector until the affected app is
restarted. Restart the game after changing scope or mode.

---

## zram expansion

`module/service.sh` hot-adds a second zram device at boot:

- runs only **after** `sys.boot_completed`
- never calls `swapoff` (which can hang under memory pressure)
- bounded retry loop (max 8 × 1 s) so a missing device node cannot stall the boot
- all failures are non-fatal — the manager and daemon still start

Result: `zram0` (~1.4 GiB) + `zram1` (1 GiB) ≈ 2.4 GiB total swap. Check with `cat /proc/swaps`.

---

## Building

```sh
python scripts/build_all.py
```

This builds, in order:

1. `scripts/build_app.py` — hidden manager APK (aapt2 + javac + d8 + zipalign + apksigner)
2. `scripts/build_daemon.py` — Rust daemon cross-compiled for Android/arm64
3. `scripts/build_module.py` — packs `module/` into `build/RamGuard.zip`

The scripts auto-detect the Android SDK from `ANDROID_HOME`, `ANDROID_SDK_ROOT` or the default
location, and the NDK from `ANDROID_NDK_HOME` or the SDK's `ndk/` directory. Signing keys are
generated locally into `build/` and are gitignored — **never commit a keystore**.

Individual steps can be run separately; see `--help` on each script.

---

## Project structure

```
daemon/    Rust root daemon (no external crates)
app/       Hidden Android manager (Java, built programmatically - no XML layouts)
module/    Magisk module template (service.sh, customize.sh, module.prop, config.txt)
scripts/   Reproducible build scripts
```

---

## Privacy & permissions

RamGuard needs no INTERNET permission and does not phone home. The manager requests only
`POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED` and `FOREGROUND_SERVICE`. The daemon invokes
`dumpsys`, `am`, `pm` and `sqlite3` through the shell as root, and only touches the paths
listed above.

---

## Disclaimer

This module force-stops other apps and modifies system swap and the LSPosed database. Use it
at your own risk. It is intended for personal use on devices you own and control.

---

## License

GPL-2.0-or-later. Full text in [LICENSE](LICENSE).
