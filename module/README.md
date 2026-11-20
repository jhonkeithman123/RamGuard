# RamGuard (Game Memory Saver)

Flash this ZIP in Magisk, then reboot.

## What it does

- Force-stops configured background apps when a configured game opens.
- Adds a second 1 GiB zram swap device (`zram1`) at boot — total swap ≈ 2.4 GiB.
- Starts a hidden manager with a persistent notification. Tap the **RamGuard active**
  notification to configure games, interval, kill list, and RewardSkip scope/mode.

## Notes

- The manager has **no launcher icon**. Open it from the notification.
- Grant notification permission if prompted on first boot.
- Do NOT put system-critical apps (GMS, microG, Magisk, Vector/LSPosed, AdAway) in the
  background kill list — it can break logins or root management. The manager hides those
  from the pickers by default; use the `⋮` menu to reveal them.
- The manager edits its own private config at
  `/data/data/com.opencode.ramguard/files/config.txt`; the root daemon reads it. The zram
  expansion is non-fatal and never disables an existing swap device.

## Logs

- `/data/adb/modules/ramguard/boot.log` — boot-time actions
- `/data/adb/modules/ramguard/ramguard.log` — daemon actions

## Uninstall

Remove the module in Magisk and reboot.
