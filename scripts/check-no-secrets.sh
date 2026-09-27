#!/usr/bin/env bash
# Secret scanner.
# Scans working tree for real credentials, excluding docs/ and test placeholders.
# See docs/09-testing/test-data-safety.md and docs/10-build/static-analysis.md.

set -euo pipefail

ROOT_DIR="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
cd "$ROOT_DIR"

echo "Scanning for credentials..."

# Regex patterns for secrets
PATTERNS=(
  'sk-ant-[A-Za-z0-9_-]{20,}'
  'sk-(proj-)?[A-Za-z0-9]{32,}'
  'ghp_[A-Za-z0-9]{36}'
  'github_pat_[A-Za-z0-9_]{50,}'
  'AIza[0-9A-Za-z_-]{35}'
  'AKIA[0-9A-Z]{16}'
  '-----BEGIN [A-Z ]*PRIVATE KEY-----'
  'xox[baprs]-[A-Za-z0-9-]{10,}'
  'sk_live_[A-Za-z0-9]{20,}'
  'GOCSPX-[A-Za-z0-9_-]{20,}'
)

FOUND=0

# Scan all tracked and untracked text files, excluding docs, .git, build, and gradle caches
FILES=$(git ls-files --exclude-standard -co | grep -v -E '^(docs/|\.git/|\.gradle/|build/|gradle/wrapper/gradle-wrapper\.jar)' || true)

for pattern in "${PATTERNS[@]}"; do
  for file in $FILES; do
    if [ ! -f "$file" ]; then
      continue
    fi
    # Search for pattern but ignore known safe test fixtures like sk-ant-test-0000000000000000 and ghp_test00000000000000000000000000000000
    MATCHES=$(grep -n -E "$pattern" "$file" 2>/dev/null | grep -v -E "sk-ant-test-0000000000000000|ghp_test00000000000000000000000000000000" || true)
    if [ -n "$MATCHES" ]; then
      echo "ERROR: Potential secret matching pattern '$pattern' in $file:" >&2
      echo "$MATCHES" >&2
      FOUND=1
    fi
  done
done

if [ "$FOUND" -ne 0 ]; then
  echo "FAIL: Credentials detected in codebase. Revoke key immediately." >&2
  exit 1
fi

echo "PASS: No credentials found."
exit 0
