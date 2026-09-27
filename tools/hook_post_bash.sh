#!/usr/bin/env bash
# PostToolUse hook for Bash.
#
# Two jobs:
#   1. If a Gradle build or gate just failed, surface the FIRST error, not the
#      last line, and remind the agent of the retry budget.
#   2. If a build succeeded, tell the agent not to re-run it. Repeating a
#      green gate burns tokens and tells us nothing.
#
# See docs/13-process/claude-code-instructions.md section 8.

set -uo pipefail
cd "$(git rev-parse --show-toplevel 2>/dev/null || echo .)" || exit 0

INPUT="$(cat 2>/dev/null || true)"
CMD="$(printf '%s' "$INPUT" | sed -n 's/.*"command"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')"
OUT="$(printf '%s' "$INPUT" | sed -n 's/.*"output"[[:space:]]*:[[:space:]]*"\(.*\)"[[:space:]]*}[[:space:]]*$/\1/p')"

case "$CMD" in
  *./gradlew*)
    if printf '%s' "$OUT" | grep -qE 'BUILD (FAILED|SUCCESSFUL)|FAILURE:'; then
      if printf '%s' "$OUT" | grep -q 'BUILD SUCCESSFUL'; then
        printf 'Gate is green. Do not re-run it. Commit and push the work it verified.\n' >&2
        exit 0
      fi
      printf '\n=== RED GATE: read the FIRST error, not the last line ===\n' >&2
      printf '%s' "$OUT" | grep -nE '^(e:|error:|.*FAILURE:|.*Compilation error)' | head -15 >&2
      printf '\nRetry budget: 3 attempts with 3 different causes means the design is wrong.\nStop after that and write the blocker to docs/14-build-plan/progress-log.md.\n' >&2
    fi
    ;;
esac

exit 0
