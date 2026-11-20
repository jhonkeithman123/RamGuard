#!/usr/bin/env python3
"""Package module/ into a flashable RamGuard.zip."""
import argparse
import pathlib
import zipfile

ROOT = pathlib.Path(__file__).resolve().parent.parent
MODULE = ROOT / "module"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=str(ROOT / "build" / "RamGuard.zip"))
    args = ap.parse_args()

    out = pathlib.Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    if out.exists():
        out.unlink()
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
        for f in sorted(MODULE.rglob("*")):
            if f.is_file():
                z.write(f, f.relative_to(MODULE).as_posix())
                print("+", f.relative_to(MODULE).as_posix())
    print(f"\nBuilt {out} ({out.stat().st_size} bytes)")


if __name__ == "__main__":
    main()
