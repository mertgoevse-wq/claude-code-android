#!/usr/bin/env bash
# Analytics and telemetry scanner.
# Enforces zero analytics/telemetry SDKs in source code, dependency declarations, and APKs.
# See docs/11-operations/telemetry.md.

set -euo pipefail

ROOT_DIR="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
cd "$ROOT_DIR"

echo "Checking for forbidden analytics and telemetry SDKs..."

BANNED_SYMBOLS=(
  "com.google.firebase.analytics"
  "com.google.android.gms.measurement"
  "com.adjust.sdk"
  "com.appsflyer"
  "com.amplitude"
  "com.mixpanel"
  "io.sentry"
  "com.segment.analytics"
  "com.datadog"
  "com.newrelic"
  "com.bugsnag"
  "com.microsoft.appcenter"
  "io.branch"
  "com.flurry"
)

FOUND=0

# If an APK is passed as argument, scan its dex/zip entries
if [ $# -ge 1 ] && [ -f "$1" ]; then
  APK="$1"
  echo "Scanning APK: $APK"
  for symbol in "${BANNED_SYMBOLS[@]}"; do
    if unzip -l "$APK" 2>/dev/null | grep -q "$symbol"; then
      echo "ERROR: Banned analytics symbol '$symbol' found in $APK" >&2
      FOUND=1
    fi
  done
else
  # Scan source code and gradle files (excluding docs, .git, scripts)
  FILES=$(git ls-files --exclude-standard -co | grep -v -E '^(docs/|\.git/|\.gradle/|build/|scripts/|gradle/wrapper/gradle-wrapper\.jar)' || true)
  for symbol in "${BANNED_SYMBOLS[@]}"; do
    for file in $FILES; do
      if [ ! -f "$file" ]; then
        continue
      fi
      if grep -n "$symbol" "$file" 2>/dev/null; then
        echo "ERROR: Banned analytics symbol '$symbol' found in $file" >&2
        FOUND=1
      fi
    done
  done
fi

if [ "$FOUND" -ne 0 ]; then
  echo "FAIL: Analytics or telemetry SDK detected. See docs/11-operations/telemetry.md." >&2
  exit 1
fi

echo "PASS: Zero analytics/telemetry SDKs found."
exit 0
