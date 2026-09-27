#!/usr/bin/env python3
"""
Source manifest checker.
Tracks required source files per module and phase as defined in spec Section 19.
Usage:
  python3 tools/check_source_manifest.py [--phase <0-7>] [--list]
"""

import sys
from pathlib import Path

ROOT_DIR = Path(__file__).resolve().parent.parent

# Files that must exist by phase
MANIFEST = {
    0: [
        "README.md",
        "CLAUDE.md",
        "LICENSE",
        "NOTICE",
        "THIRD_PARTY_NOTICES.md",
        ".gitignore",
        ".editorconfig",
        "gradle/wrapper/gradle-wrapper.properties",
        "gradle.properties",
        "settings.gradle.kts",
        "build.gradle.kts",
        "gradle/libs.versions.toml",
        "tools/check_doc_manifest.py",
        "tools/check_source_manifest.py",
        "tools/check_no_android_imports_in_shared.py",
        "scripts/check-no-secrets.sh",
        "scripts/check-no-analytics.sh",
        "tools/ci.sh",
        "tools/format.sh",
    ],
    2: [
        "shared/core/src/commonMain/kotlin/dev/ccandroid/core/Result.kt",
        "shared/core/src/commonMain/kotlin/dev/ccandroid/core/AppError.kt",
        "shared/core/src/commonMain/kotlin/dev/ccandroid/core/Dispatchers.kt",
        "shared/core/src/commonMain/kotlin/dev/ccandroid/core/TimeProvider.kt",
        "shared/core/src/commonMain/kotlin/dev/ccandroid/core/IdGenerator.kt",
        "shared/core/src/commonMain/kotlin/dev/ccandroid/core/Logger.kt",
        "shared/core/src/commonMain/kotlin/dev/ccandroid/core/Redactor.kt",
        "shared/core/src/commonMain/kotlin/dev/ccandroid/core/BuildInfo.kt",
        "shared/core/src/commonMain/kotlin/dev/ccandroid/core/FlowExt.kt",
        "shared/core/src/commonMain/kotlin/dev/ccandroid/core/ProcessGateway.kt",
        "shared/core/src/commonMain/kotlin/dev/ccandroid/core/CryptoGateway.kt",
        "shared/core/src/commonMain/kotlin/dev/ccandroid/core/FileSystemGateway.kt",
        "shared/core/src/commonMain/kotlin/dev/ccandroid/core/NetworkMonitor.kt",
        "shared/core/src/commonMain/kotlin/dev/ccandroid/core/ClipboardGateway.kt",
        "shared/core/src/commonMain/kotlin/dev/ccandroid/core/PlatformCapabilities.kt",
    ],
}

def main():
    phase_filter = None
    if "--phase" in sys.argv:
        idx = sys.argv.index("--phase")
        if idx + 1 < len(sys.argv):
            phase_filter = int(sys.argv[idx + 1])

    if "--list" in sys.argv:
        print("Source files in manifest:")
        for phase, files in sorted(MANIFEST.items()):
            print(f"Phase {phase}:")
            for f in files:
                print(f"  {f}")
        sys.exit(0)

    phases_to_check = [phase_filter] if phase_filter is not None else sorted(MANIFEST.keys())

    missing = []
    total_checked = 0
    for p in phases_to_check:
        for rel_path in MANIFEST.get(p, []):
            total_checked += 1
            full_path = ROOT_DIR / rel_path
            if not full_path.exists():
                missing.append((p, rel_path))

    if missing:
        print(f"FAIL: {len(missing)} of {total_checked} expected files are missing:", file=sys.stderr)
        for phase, path in missing:
            print(f"  [Phase {phase}] {path}", file=sys.stderr)
        sys.exit(1)

    print(f"PASS: All {total_checked} source files in checked phases exist.")
    sys.exit(0)

if __name__ == "__main__":
    main()
