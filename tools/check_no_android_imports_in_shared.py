#!/usr/bin/env python3
"""
Layer discipline check.
Ensures shared/core and shared/domain never import android.* or androidx.*.
Usage:
  python3 tools/check_no_android_imports_in_shared.py
"""

import sys
import re
from pathlib import Path

ROOT_DIR = Path(__file__).resolve().parent.parent

FORBIDDEN_PATTERNS = [
    re.compile(r"^\s*import\s+android\.", re.MULTILINE),
    re.compile(r"^\s*import\s+androidx\.", re.MULTILINE),
]

PURE_MODULES = [
    ROOT_DIR / "shared" / "core",
    ROOT_DIR / "shared" / "domain",
]

def check_file(path):
    violations = []
    try:
        content = path.read_text(encoding="utf-8")
    except Exception as e:
        return [f"Cannot read file: {e}"]

    for idx, line in enumerate(content.splitlines(), start=1):
        for pattern in FORBIDDEN_PATTERNS:
            if pattern.search(line):
                violations.append((idx, line.strip()))
    return violations

def main():
    print("Checking for forbidden android/androidx imports in pure modules (shared/core, shared/domain)...")
    violations = []
    files_checked = 0

    for mod in PURE_MODULES:
        if not mod.exists():
            continue
        for kt_file in mod.glob("**/*.kt"):
            # Skip actual/Android platform implementations
            if "actual" in str(kt_file) or "androidMain" in str(kt_file):
                continue
            files_checked += 1
            file_violations = check_file(kt_file)
            for line_no, line_text in file_violations:
                violations.append((kt_file, line_no, line_text))

    if violations:
        print(f"FAIL: Found {len(violations)} illegal Android import(s) in pure shared modules:", file=sys.stderr)
        for path, line_no, text in violations:
            print(f"  {path.relative_to(ROOT_DIR)}:{line_no} -> {text}", file=sys.stderr)
        sys.exit(1)

    print(f"PASS: Checked {files_checked} files in pure modules. Zero forbidden imports.")
    sys.exit(0)

if __name__ == "__main__":
    main()
