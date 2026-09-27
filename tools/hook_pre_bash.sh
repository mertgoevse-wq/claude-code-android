#!/usr/bin/env bash
# PreToolUse hook for Bash.
#
# The operator runs Claude Code with --dangerously-skip-permissions, so this
# hook is the enforcement point for the hard blocks on the git path. It reads
# the tool input on stdin and exits non-zero to refuse.
#
# See docs/13-process/git-strategy.md section 6.

set -uo pipefail
cd "$(git rev-parse --show-toplevel 2>/dev/null || echo .)" || exit 0

INPUT="$(cat 2>/dev/null || true)"
CMD="$(printf '%s' "$INPUT" | sed -n 's/.*"command"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')"
[ -z "$CMD" ] && exit 0

refuse() {
  printf 'HARD BLOCK: %s\n%s\n' "$1" "$2" >&2
  exit 2
}

case "$CMD" in
  *"push --force-with-lease"*)
    if printf '%s' "$CMD" | grep -qE 'main|master|develop'; then
      refuse "force-push to a shared branch" "Only your own task/<slug> branch may be force-pushed, and only with --force-with-lease."
    fi
    ;;
  *"push --force"*|*"push -f"*)
    refuse "force-push" "History is append-only. Fix forward. Only your own task/<slug> branch may be force-pushed, with --force-with-lease."
    ;;
  *"push --delete"*|*"push origin :"*|*"push origin"*" :"*)
    refuse "hard block 1: never delete" "No branch, tag, or ref may be deleted. Mark the old one deprecated instead."
    ;;
  *"git push"*)
    if printf '%s' "$CMD" | grep -qE 'origin[ /]+(main|master|develop)([[:space:]]|$)'; then
      refuse "hard block 4: never push to the default branch" "Create a task/<slug> branch and open a PR."
    fi
    BRANCH="$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo unknown)"
    case "$BRANCH" in
      main|master|develop)
        refuse "hard block 4: never push to the default branch" "You are on $BRANCH. Switch to task/<slug> and push there. The root commit was the only documented exception."
        ;;
    esac
    ;;
  *"commit --no-verify"*|*"commit -n"*|*"commit --amend"*)
    refuse "hard block 5: never hide anything" "--no-verify skips the secret scan; --amend rewrites pushed history. If a hook is wrong, fix the hook in its own commit."
    ;;
  *"rm -rf"*|*"rm -f"*|*"git clean -f"*|*"reset --hard"*)
    refuse "hard block 1: never delete" "Nothing is deleted in this project. Move the file to a quarantine directory and record why."
    ;;
  *"gh repo delete"*|*"push --mirror"*|*"visibility public"*)
    refuse "hard block 3: never make anything public" "Repositories stay private."
    ;;
esac

case "$CMD" in
  *"git commit"*|*"git push"*)
    if [ -x scripts/check-no-secrets.sh ]; then
      if ! scripts/check-no-secrets.sh >/dev/null 2>&1; then
        refuse "a secret was found in the working tree" "Run scripts/check-no-secrets.sh to see where. If a real value was committed, rotate it; deleting the line is not enough."
      fi
    fi
    ;;
esac

exit 0
