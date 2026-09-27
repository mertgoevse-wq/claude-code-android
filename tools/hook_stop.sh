#!/usr/bin/env bash
# Stop hook.
#
# The operator runs with --dangerously-skip-permissions, so the rule
# "commit and push after every step" cannot be enforced by a dialog. This
# hook runs when a turn ends.
#
# It refuses to let a turn end with uncommitted work on the branch, unless
# the uncommitted paths are exactly the ones the work was about (a doc
# change in flight) - in which case it says so out loud.
#
# See docs/13-process/git-strategy.md section 6.

set -uo pipefail
cd "$(git rev-parse --show-toplevel 2>/dev/null || echo .)" || exit 0

git rev-parse --is-inside-work-tree >/dev/null 2>&1 || exit 0

BRANCH="$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo unknown)"
DIRTY="$(git status --porcelain 2>/dev/null | grep -vE '^\?\?' || true)"
UNTRACKED="$(git status --porcelain 2>/dev/null | grep -E '^\?\?' || true)"
AHEAD="$(git rev-list --count @{u}..HEAD 2>/dev/null || echo 0)"

if [ "$BRANCH" = "main" ] || [ "$BRANCH" = "master" ] || [ "$BRANCH" = "develop" ]; then
  if [ -n "$DIRTY" ] || [ "$AHEAD" != "0" ]; then
    printf 'You are on %s with uncommitted or unpushed work. Hard block 4 says the default\nbranch only ever received the root commit. Switch to task/<slug>, commit, push, open a PR.\n' "$BRANCH" >&2
  fi
fi

if [ -n "$DIRTY" ]; then
  printf '\n=== UNCOMMITTED WORK: %s file(s) ===\n' "$(printf '%s\n' "$DIRTY" | wc -l | tr -d ' ')" >&2
  printf '%s\n' "$DIRTY" | head -20 >&2
  printf '\nCommit it before this turn ends. git add -p (never -A), commit, push.\n' >&2
  printf 'If it should NOT be committed, say why in the response and move it to a quarantine path.\n' >&2
fi

if [ -n "$UNTRACKED" ]; then
  printf '\n=== UNTRACKED FILES ===\n' >&2
  printf '%s\n' "$UNTRACKED" | head -10 >&2
  printf 'Either stage them, add them to .gitignore, or name them as deliberately untracked.\n' >&2
fi

if [ "$AHEAD" != "0" ] && [ "$AHEAD" != "" ]; then
  printf '\n=== %s commit(s) not pushed ===\n' "$AHEAD" >&2
  printf 'An interrupted build must leave a remote branch another machine can continue from. git push.\n' >&2
fi

exit 0
