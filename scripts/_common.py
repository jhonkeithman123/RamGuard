"""Shared Android SDK / NDK discovery and command helpers for the build scripts."""
import os
import pathlib
import subprocess


def sdk_dirs(explicit=None):
    if explicit:
        yield pathlib.Path(explicit)
        return
    for var in ("ANDROID_HOME", "ANDROID_SDK_ROOT"):
        if os.environ.get(var):
            yield pathlib.Path(os.environ[var])
    home = pathlib.Path.home()
    yield home / "AppData" / "Local" / "Android" / "Sdk"
    yield home / "Android" / "Sdk"


def find_sdk(explicit=None):
    for sdk in sdk_dirs(explicit):
        if (sdk / "platform-tools" / "adb.exe").exists() or (sdk / "platform-tools" / "adb").exists():
            return sdk
    raise SystemExit("Android SDK not found. Pass --sdk or set ANDROID_HOME.")


def newest(path, pattern="*"):
    items = sorted(pathlib.Path(path).glob(pattern), reverse=True)
    if not items:
        raise SystemExit(f"No entries in {path}")
    return items[0]


def tool(name, sdk):
    suffixes = (".exe", ".bat", "") if os.name == "nt" else ("",)
    for bt in sorted((sdk / "build-tools").glob("*"), reverse=True):
        for suffix in suffixes:
            p = bt / (name + suffix)
            if p.exists():
                return str(p)
    raise SystemExit(f"Missing {name} in any build-tools directory")


def run(cmd, **kwargs):
    print("+", " ".join(str(c) for c in cmd), flush=True)
    subprocess.run([str(c) for c in cmd], check=True, **kwargs)


def javac_name():
    return "javac.exe" if os.name == "nt" else "javac"


def keytool_name():
    return "keytool.exe" if os.name == "nt" else "keytool"


def classpath(*parts):
    sep = ";" if os.name == "nt" else ":"
    return sep.join(str(p) for p in parts)
