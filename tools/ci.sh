#!/usr/bin/env bash
# Runs everything CI runs, locally.
# Usage: ./tools/ci.sh

set -euo pipefail

ROOT_DIR="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
cd "$ROOT_DIR"

echo "=== Running local CI pipeline ==="

echo "1. Checking doc manifest..."
python3 tools/check_doc_manifest.py

echo "2. Checking source manifest..."
python3 tools/check_source_manifest.py --phase 0

echo "3. Checking layer discipline (no Android imports in pure shared)..."
python3 tools/check_no_android_imports_in_shared.py

echo "3b. Checking typed errors at boundaries (no bare throw in shared)..."
python3 tools/check_typed_errors_at_boundaries.py

echo "4. Checking for secrets..."
bash scripts/check-no-secrets.sh

echo "5. Checking for analytics..."
bash scripts/check-no-analytics.sh

echo "6. Running format check..."
./tools/format.sh --check

if [ -f "./gradlew" ] && [ -f "./settings.gradle.kts" ]; then
  echo "7. Running Gradle verification..."
  ./gradlew --version
  if ./gradlew tasks --all 2>&1 | grep -q "^check "; then
    ./gradlew check
  else
    echo "Notice: :check task not yet declared in root/subprojects"
  fi
fi

echo "=== Local CI pipeline passed successfully! ==="
exit 0
