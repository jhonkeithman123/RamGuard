use std::fs::{self, OpenOptions};
use std::io::Write;
use std::os::unix::fs::PermissionsExt;
use std::process::Command;
use std::thread;
use std::time::{Duration, SystemTime, UNIX_EPOCH};

const DIR: &str = "/data/adb/modules/ramguard";
const LOG: &str = "/data/adb/modules/ramguard/ramguard.log";
// App-writable config (no root needed for the companion UI)
const CONFIG: &str = "/data/data/com.opencode.ramguard/files/config.txt";
// Module fallback config
const MODULE_CONFIG: &str = "/data/adb/modules/ramguard/config.txt";
// IPC with companion app: app writes a package name here, daemon applies it
const SCOPE_REQ: &str = "/data/data/com.opencode.ramguard/files/scope_request.txt";
const SCOPE_REMOVE: &str = "/data/data/com.opencode.ramguard/files/scope_remove.txt";
// Daemon writes current RewardSkip scope here for the app to display
const SCOPE_LIST: &str = "/data/data/com.opencode.ramguard/files/rewardskip_scope.txt";
// RewardSkip mode: app writes private file, daemon mirrors to a world-readable
// file that the hooked game processes can actually read.
const MODE_PRIVATE: &str = "/data/data/com.opencode.ramguard/files/skip_mode.txt";
const MODE_PUBLIC: &str = "/data/local/tmp/skip_mode.txt";
const LSPD_DB: &str = "/data/adb/lspd/config/modules_config.db";

fn now() -> u64 {
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap_or_default()
        .as_secs()
}

fn log(msg: &str) {
    if let Ok(mut f) = OpenOptions::new().create(true).append(true).open(LOG) {
        let _ = writeln!(f, "[{}] {}", now(), msg);
    }
}

fn run(cmd: &str, args: &[&str]) -> String {
    match Command::new(cmd).args(args).output() {
        Ok(o) => String::from_utf8_lossy(&o.stdout).to_string(),
        Err(e) => {
            log(&format!("exec {} failed: {}", cmd, e));
            String::new()
        }
    }
}

fn default_config() -> String {
    [
        "target=droidhang.twgame.restaurant",
        "interval=3",
        "kill=com.facebook.katana",
        "kill=com.facebook.orca",
        "kill=app.revanced.android.youtube",
        "kill=com.android.vending",
        "kill=com.google.android.play.games",
    ]
    .join("\n")
        + "\n"
}

fn config_path() -> &'static str {
    if std::path::Path::new(CONFIG).exists() {
        CONFIG
    } else {
        MODULE_CONFIG
    }
}

fn config_mtime() -> Option<u64> {
    fs::metadata(config_path())
        .ok()
        .and_then(|m| m.modified().ok())
        .and_then(|t| t.duration_since(UNIX_EPOCH).ok())
        .map(|d| d.as_secs())
}

fn read_config() -> (Vec<String>, u64, Vec<String>) {
    let _ = fs::create_dir_all(DIR);
    let _ = fs::create_dir_all("/sdcard/ramguard");
    if !std::path::Path::new(CONFIG).exists() {
        let _ = fs::write(CONFIG, default_config());
        log("created default config");
    }
    let mut targets = Vec::new();
    let mut interval = 3u64;
    let mut kills = Vec::new();
    if let Ok(text) = fs::read_to_string(config_path()) {
        for raw in text.lines() {
            let line = raw.trim();
            if line.is_empty() || line.starts_with('#') {
                continue;
            }
            if let Some(v) = line.strip_prefix("target=") {
                let p = v.trim().to_string();
                if !p.is_empty() {
                    targets.push(p);
                }
            } else if let Some(v) = line.strip_prefix("interval=") {
                if let Ok(n) = v.trim().parse::<u64>() {
                    interval = n.max(1);
                }
            } else if let Some(v) = line.strip_prefix("kill=") {
                let p = v.trim().to_string();
                if !p.is_empty() {
                    kills.push(p);
                }
            }
        }
    }
    if targets.is_empty() {
        targets.push("droidhang.twgame.restaurant".to_string());
    }
    (targets, interval, kills)
}

fn sync_rewardskip_scope() {
    let mid = run("sqlite3", &[LSPD_DB,
        "select mid from modules where module_pkg_name='com.opencode.rewardskip';"]);
    let mid = mid.trim();
    if mid.is_empty() {
        let _ = fs::write(SCOPE_LIST, "STATUS=NOT_INSTALLED\n");
        return;
    }

    // apply a pending REMOVE request from the companion app
    if let Ok(req) = fs::read_to_string(SCOPE_REMOVE) {
        let pkg = req.trim();
        if !pkg.is_empty() && pkg.chars().all(|c| c.is_ascii_alphanumeric() || c == '.' || c == '_') {
            let sql = format!(
                "DELETE FROM scope WHERE mid={} AND app_pkg_name='{}';", mid, pkg
            );
            let _ = run("sqlite3", &[LSPD_DB, &sql]);
            log(&format!("RewardSkip scope: removed {}", pkg));
        }
        let _ = fs::remove_file(SCOPE_REMOVE);
    }

    // apply a pending ADD request from the companion app
    if let Ok(req) = fs::read_to_string(SCOPE_REQ) {
        let pkg = req.trim();
        if !pkg.is_empty() && pkg.chars().all(|c| c.is_ascii_alphanumeric() || c == '.' || c == '_') {
            let sql = format!(
                "INSERT OR REPLACE INTO scope(mid,app_pkg_name,user_id) VALUES({},'{}',0);",
                mid, pkg
            );
            let _ = run("sqlite3", &[LSPD_DB, &sql]);
            log(&format!("RewardSkip scope: added {}", pkg));
        }
        let _ = fs::remove_file(SCOPE_REQ);
    }

    // publish current scope for the app
    let sql = format!("select app_pkg_name from scope where mid={};", mid);
    let list = run("sqlite3", &[LSPD_DB, &sql]);
    let mut out = String::from("STATUS=OK\n");
    out.push_str(&list);
    let _ = fs::write(SCOPE_LIST, out);
}

fn sync_skip_mode() {
    if let Ok(s) = fs::read_to_string(MODE_PRIVATE) {
        let v = s.trim();
        if v == "instant" || v == "delayed" {
            if let Ok(mut f) = OpenOptions::new()
                .create(true)
                .write(true)
                .truncate(true)
                .open(MODE_PUBLIC)
            {
                let _ = f.write_all(v.as_bytes());
                let _ = fs::set_permissions(MODE_PUBLIC, fs::Permissions::from_mode(0o644));
            }
        }
    }
}

fn manager_running() -> bool {
    // Check the service itself, not just the process — an Activity can be alive
    // while the foreground service (and its notification) is dead.
    run("dumpsys", &["activity", "services", "com.opencode.ramguard"])
        .contains("ServiceRecord")
}

fn game_in_foreground(targets: &[String]) -> bool {
    // "dumpsys window windows" has no focus lines on this ROM; use activities instead.
    let out = run("dumpsys", &["activity", "activities"]);
    out.lines().any(|l| {
        let l = l.trim();
        if !(l.starts_with("topResumedActivity=") || l.starts_with("ResumedActivity:")) {
            return false;
        }
        targets.iter().any(|t| l.contains(t))
    })
}

fn cleanup(kills: &[String]) {
    let mut n = 0;
    for pkg in kills {
        if pkg.is_empty() {
            continue;
        }
        let _ = run("am", &["force-stop", pkg]);
        n += 1;
    }
    log(&format!("game foreground -> force-stopped {} background app(s)", n));
}

fn main() {
    let _ = fs::create_dir_all(DIR);
    log("ramguard daemon started");
    let (mut targets, mut interval, mut kills) = read_config();
    let mut last_mtime = config_mtime();
    log(&format!(
        "config: targets={} interval={}s kills={}",
        targets.join(","),
        interval,
        kills.len()
    ));

    let mut was_foreground = false;
    let mut last_mgr_restart = 0u64;
    loop {
        // reload config if the companion app changed it
        let mt = config_mtime();
        if mt != last_mtime {
            let (t, i, k) = read_config();
            targets = t;
            interval = i;
            kills = k;
            last_mtime = mt;
            log(&format!(
                "config reloaded: targets={} interval={}s kills={}",
                targets.join(","),
                interval,
                kills.len()
            ));
        }

        sync_rewardskip_scope();
        sync_skip_mode();

        // watchdog: keep the companion notification service alive
        // (it can be killed by the low-memory killer on this device)
        if !manager_running() && now().saturating_sub(last_mgr_restart) > 30 {
            let _ = run("am", &["start-foreground-service", "-n", "com.opencode.ramguard/.GuardService"]);
            last_mgr_restart = now();
            log("manager service restarted by watchdog");
        }

        let fg = game_in_foreground(&targets);
        if fg && !was_foreground {
            cleanup(&kills);
        }
        was_foreground = fg;
        thread::sleep(Duration::from_secs(interval));
    }
}
