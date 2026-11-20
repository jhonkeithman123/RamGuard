#!/usr/bin/env python3
"""Build everything: manager APK, root daemon, Magisk module ZIP."""
import argparse
import pathlib
import shutil
import subprocess
import sys

HERE = pathlib.Path(__file__).resolve().parent
ROOT = HERE.parent
BUILD = ROOT / "build"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--sdk")
    ap.add_argument("--ndk")
    args = ap.parse_args()

    common = []
    if args.sdk:
        common += ["--sdk", args.sdk]
    if args.ndk:
        common += ["--ndk", args.ndk]

    def script(name, *extra):
        subprocess.run([sys.executable, str(HERE / name), *common, *extra], check=True)

    print("\n=== 1/4 building manager APK ===", flush=True)
    apk = BUILD / "out" / "RamGuardManager.apk"
    script("build_app.py", "--out", str(apk))

    print("\n=== 2/4 building root daemon ===", flush=True)
    script("build_daemon.py")

    print("\n=== 3/4 embedding APK into module ===", flush=True)
    dest = ROOT / "module" / "system" / "priv-app" / "RamGuardManager" / "RamGuardManager.apk"
    dest.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(apk, dest)
    print("copied", apk, "->", dest)

    print("\n=== 4/4 packaging Magisk module ===", flush=True)
    script("build_module.py", "--out", str(BUILD / "RamGuard.zip"))

    print(f"\nDone: {BUILD / 'RamGuard.zip'}")


if __name__ == "__main__":
    main()
