#!/usr/bin/env python3
"""Cross-compile the Rust root daemon for Android/arm64 and install it into the module."""
import argparse
import os
import pathlib
import shutil
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
DAEMON = ROOT / "daemon"


def run(cmd, **kwargs):
    print("+", " ".join(str(c) for c in cmd), flush=True)
    subprocess.run([str(c) for c in cmd], check=True, **kwargs)


def find_ndk(explicit=None):
    if explicit:
        return pathlib.Path(explicit)
    if os.environ.get("ANDROID_NDK_HOME"):
        return pathlib.Path(os.environ["ANDROID_NDK_HOME"])
    sdk = None
    for var in ("ANDROID_HOME", "ANDROID_SDK_ROOT"):
        if os.environ.get(var):
            sdk = pathlib.Path(os.environ[var])
            break
    if sdk is None:
        home = pathlib.Path.home()
        for candidate in (home / "AppData" / "Local" / "Android" / "Sdk", home / "Android" / "Sdk"):
            if candidate.exists():
                sdk = candidate
                break
    if sdk and (sdk / "ndk").exists():
        return sorted((sdk / "ndk").iterdir(), reverse=True)[0]
    raise SystemExit("Android NDK not found. Set ANDROID_NDK_HOME or install NDK via SDK Manager.")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--ndk")
    ap.add_argument("--target", default="aarch64-linux-android")
    args = ap.parse_args()

    ndk = find_ndk(args.ndk)
    host = "windows-x86_64" if os.name == "nt" else ("darwin-x86_64" if sys.platform == "darwin" else "linux-x86_64")
    clang = ndk / "toolchains" / "llvm" / "prebuilt" / host / "bin" / (args.target + "26-clang" + (".cmd" if os.name == "nt" else ""))
    if not clang.exists():
        raise SystemExit(f"Missing compiler: {clang}")

    cfg = DAEMON / ".cargo" / "config.toml"
    cfg.parent.mkdir(parents=True, exist_ok=True)
    cfg.write_text(
        f"[target.{args.target}]\nlinker = '{clang.as_posix()}'\n",
        encoding="utf-8", newline="\n")

    run(["cargo", "build", "--release", "--target", args.target, "--manifest-path", DAEMON / "Cargo.toml"])

    built = DAEMON / "target" / args.target / "release" / "ramguard"
    if not built.exists():
        built = built.with_suffix(".exe")
    dest = ROOT / "module" / "bin" / "ramguard"
    dest.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(built, dest)
    os.chmod(dest, 0o755)
    print(f"\nInstalled daemon: {dest} ({dest.stat().st_size} bytes)")
    print("Note: daemon/.cargo/config.toml is generated and gitignored.")


if __name__ == "__main__":
    main()
