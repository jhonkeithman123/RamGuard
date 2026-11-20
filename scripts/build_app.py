#!/usr/bin/env python3
"""Build the hidden RamGuard manager APK from source."""
import argparse
import pathlib
import shutil
import sys
import zipfile

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from _common import find_sdk, tool, run, javac_name, keytool_name, classpath

ROOT = pathlib.Path(__file__).resolve().parent.parent
APP = ROOT / "app"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--sdk")
    ap.add_argument("--out", default=str(ROOT / "build" / "RamGuardManager.apk"))
    ap.add_argument("--keystore", default=str(ROOT / "build" / "ramguard.jks"))
    ap.add_argument("--storepass", default="ramguard")
    ap.add_argument("--alias", default="mgr")
    args = ap.parse_args()

    sdk = find_sdk(args.sdk)
    platform = sorted((sdk / "platforms").glob("android-*"),
                      key=lambda p: int(p.name.split("-")[-1]), reverse=True)[0]
    android_jar = platform / "android.jar"

    work = ROOT / "build"
    if work.exists():
        shutil.rmtree(work)
    for sub in ("classes", "gen", "dex", "apk"):
        (work / sub).mkdir(parents=True, exist_ok=True)

    aapt2, d8 = tool("aapt2", sdk), tool("d8", sdk)
    zipalign, apksigner = tool("zipalign", sdk), tool("apksigner", sdk)

    # 1. manifest -> binary APK + generated R class
    base = work / "apk" / "base.apk"
    run([aapt2, "link", "--manifest", APP / "AndroidManifest.xml",
         "-I", android_jar, "--java", work / "gen", "-o", base,
         "--min-sdk-version", "26", "--target-sdk-version", "33",
         "--version-code", "1000000", "--version-name", "1.0.0"])

    # 2. sources (plus generated R.java)
    sources = list((APP / "src").rglob("*.java")) + list((work / "gen").rglob("*.java"))
    run([javac_name(), "-encoding", "UTF-8", "-source", "8", "-target", "8",
         "-cp", android_jar, "-d", work / "classes", *sources])

    # 3. DEX + inject
    run([d8, "--lib", android_jar, "--output", work / "dex",
         *(work / "classes").rglob("*.class")])
    with zipfile.ZipFile(base, "a", zipfile.ZIP_DEFLATED) as z:
        z.write(work / "dex" / "classes.dex", "classes.dex")

    aligned = work / "apk" / "aligned.apk"
    run([zipalign, "-f", "-p", "4", base, aligned])

    # 4. signing key generated locally - never committed
    ks = pathlib.Path(args.keystore)
    if not ks.exists():
        run([keytool_name(), "-genkeypair", "-alias", args.alias,
             "-keyalg", "RSA", "-keysize", "2048", "-validity", "10000",
             "-keystore", ks, "-storepass", args.storepass,
             "-keypass", args.storepass, "-dname", "CN=RamGuardMgr"])

    out = pathlib.Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    run([apksigner, "sign", "--ks", ks,
         "--ks-pass", f"pass:{args.storepass}",
         "--key-pass", f"pass:{args.storepass}",
         "--out", out, aligned])
    print(f"\nBuilt {out} ({out.stat().st_size} bytes)")


if __name__ == "__main__":
    main()
