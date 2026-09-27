#!/usr/bin/env bash
# Start an autonomous build session.
#
#   ./tools/autonomous.sh            # continue the current branch
#   ./tools/autonomous.sh 0          # start Phase 0
#   ./tools/autonomous.sh "…"        # a custom instruction
#
# Syncs the branch, then hands the session its order. The session reads
# BOOTSTRAP.md, which is the single source for the build loop, the hard
# blocks, the finish line, and the blocker format.
#
# See docs/13-process/claude-code-instructions.md.

set -euo pipefail
cd "$(git rev-parse --show-toplevel 2>/dev/null || echo "$(dirname "$0")/..")"

BRANCH="$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo '?')"

if [ "$BRANCH" = "main" ] || [ "$BRANCH" = "master" ] || [ "$BRANCH" = "develop" ]; then
  SLUG="task/$(date +%Y%m%d)-$(git log -1 --pretty=%s | tr -c 'a-z0-9\n' '-' | cut -c1-30 | sed 's/-$//')"
  git switch -c "$SLUG" 2>/dev/null || git switch -c "task/manual-$(date +%H%M%S)"
  echo "on a protected branch - created $(git rev-parse --abbrev-ref HEAD)"
fi

if [ -n "$(git status --porcelain 2>/dev/null)" ]; then
  echo "warning: uncommitted work in the tree; the session will be asked about it"
  git status --short
fi

git fetch --quiet origin 2>/dev/null || true
if git rev-parse --abbrev-ref --symbolic-full-name @{u} >/dev/null 2>&1; then
  git push --quiet 2>/dev/null || echo "note: nothing pushed, or the push needs a look"
fi

if [ $# -gt 0 ] && [ -n "$1" ]; then
  INSTRUCTION="$*"
else
  INSTRUCTION="Read BOOTSTRAP.md and execute it. Read the progress log once, continue from where it actually is, and do not stop until a signed APK is installed and running on the Galaxy A56. If you are blocked, write the blocker in the documented format, commit it, push it, and stop."
fi

echo
echo "branch:   $(git rev-parse --abbrev-ref HEAD)"
echo "starting: claude --dangerously-skip-permissions"
echo

if ! command -v claude >/dev/null 2>&1; then
  echo "claude is not on PATH. Install the Claude Code CLI, then re-run this script." >&2
  exit 1
fi

exec claude --dangerously-skip-permissions "$INSTRUCTION"
