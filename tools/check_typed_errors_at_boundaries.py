#!/usr/bin/env python3
"""P2-13: no bare exception crosses a repository boundary.

The rule from CLAUDE.md code rule 4 and docs/02-architecture/error-taxonomy.md:
a `Throwable` never crosses a module boundary; it becomes an `AppError` at the
boundary. In practice the codebase allows exactly two throw shapes, and both
are mechanically recognisable:

- `throw OutcomeException(...)` -- the getOrThrow bridge, thrown by a use case
  and re-caught by `tryCatch` at the same call's boundary (also in the
  `?: throw` idiom).
- `throw e` where `e` is the parameter of the enclosing `catch` -- a rethrow
  propagates the original error for the caller's boundary to classify; it
  creates no new untyped error.

Anything else -- constructing and throwing a non-OutcomeException anywhere in
a line -- is a bare exception at a boundary and fails this check.

Usage: python3 tools/check_typed_errors_at_boundaries.py
Exit codes: 0 = clean, 1 = violations found.
"""

import re
import sys
from pathlib import Path

THROW = re.compile(r"\bthrow\s+([A-Za-z_][\w.]*)")
CATCH_PARAM = re.compile(r"catch\s*\(\s*(\w+)\s*:")

SCAN_ROOTS = [
    "shared/core/src/main",
    "shared/domain/src/main",
    "shared/data/src/main",
    "shared/runtime/src/main",
    "shared/orchestration/src/main",
    "shared/skills/src/main",
    "shared/vcs/src/main",
]


def violations_in(path: Path) -> list[str]:
    try:
        lines = path.read_text(encoding="utf-8").splitlines()
    except (OSError, UnicodeDecodeError):
        return []

    found: list[str] = []
    last_catch_param: str | None = None
    for number, line in enumerate(lines, start=1):
        code = line.split("//", 1)[0]  # comments may say "throw anything"
        for match in CATCH_PARAM.finditer(code):
            last_catch_param = match.group(1)

        for match in THROW.finditer(code):
            target = match.group(1)
            if "OutcomeException" in target:
                continue
            if last_catch_param is not None and target == last_catch_param:
                continue
            found.append(f"{path}:{number}: untyped throw at a boundary: {line.strip()}")
    return found


def main() -> int:
    root = Path(__file__).resolve().parent.parent
    problems: list[str] = []

    for rel in SCAN_ROOTS:
        directory = root / rel
        if not directory.exists():
            continue
        for path in sorted(directory.rglob("*.kt")):
            problems.extend(violations_in(path))

    if problems:
        print(f"FAIL: {len(problems)} untyped throw(s) at a shared boundary:")
        for problem in problems:
            print(f"  {problem}")
        print("\nEvery failure gets a code, a user-facing message, a retryability,")
        print("and a recovery action: docs/02-architecture/error-taxonomy.md.")
        return 1

    print("PASS: every throw in shared production code is typed or a rethrow of the caught error.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
