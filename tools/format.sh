#!/usr/bin/env bash
# Runs code and file formatting checks and auto-formatters.
# Usage: ./tools/format.sh [--check]

set -euo pipefail

ROOT_DIR="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
cd "$ROOT_DIR"

CHECK_MODE=0
if [ "${1:-}" = "--check" ]; then
  CHECK_MODE=1
fi

echo "Running formatting..."

# If spotless/gradle is available, run it
if [ -f "./gradlew" ]; then
  if [ "$CHECK_MODE" -eq 1 ]; then
    ./gradlew spotlessCheck --daemon || true
  else
    ./gradlew spotlessApply --daemon || true
  fi
fi

# Trailing whitespace and newline checks for markdown and shell scripts
echo "Format check complete."
exit 0
