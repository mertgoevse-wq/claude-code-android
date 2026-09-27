#!/usr/bin/env python3
"""
Manifest checker for docs.
Ensures all 135+ documentation files exist, are non-stub, and internal links resolve.
Usage:
  python3 tools/check_doc_manifest.py
  python3 tools/check_doc_manifest.py --list
"""

import sys
import os
import re
from pathlib import Path

ROOT_DIR = Path(__file__).resolve().parent.parent
DOCS_DIR = ROOT_DIR / "docs"

MIN_DOC_COUNT = 135
STUB_PATTERNS = [
    re.compile(r"^\s*TODO\s*$", re.IGNORECASE),
    re.compile(r"^\s*TBD\s*$", re.IGNORECASE),
    re.compile(r"^\s*stub\s*$", re.IGNORECASE),
]

def find_docs():
    return sorted(list(DOCS_DIR.glob("**/*.md")))

def check_doc(path):
    issues = []
    try:
        content = path.read_text(encoding="utf-8")
    except Exception as e:
        return [f"Cannot read file: {e}"]

    if len(content.strip()) < 50:
        issues.append(f"Doc is too short or empty ({len(content.strip())} bytes)")

    lines = content.strip().splitlines()
    if len(lines) < 3:
        issues.append("Doc has fewer than 3 lines")

    for p in STUB_PATTERNS:
        if p.match(content.strip()):
            issues.append("Doc contains only a stub marker")

    # Check internal markdown links [text](path)
    link_pattern = re.compile(r'\[([^\]]+)\]\(([^)]+)\)')
    for match in link_pattern.finditer(content):
        target = match.group(2).split("#")[0].strip()
        if not target:
            continue
        if target.startswith("http://") or target.startswith("https://") or target.startswith("mailto:"):
            continue
        # Relative or root-relative path
        if target.startswith("/"):
            resolved = ROOT_DIR / target.lstrip("/")
        else:
            resolved = (path.parent / target).resolve()

        if not resolved.exists():
            issues.append(f"Broken link '{target}' in {path.relative_to(ROOT_DIR)}")

    return issues

def main():
    docs = find_docs()

    if "--list" in sys.argv:
        print(f"Listing {len(docs)} documents in {DOCS_DIR}:")
        for d in docs:
            print(f"  {d.relative_to(ROOT_DIR)}")
        print(f"Total: {len(docs)} docs")
        sys.exit(0)

    print(f"Checking doc manifest ({len(docs)} docs found, min required: {MIN_DOC_COUNT})...")

    if len(docs) < MIN_DOC_COUNT:
        print(f"ERROR: Expected at least {MIN_DOC_COUNT} docs, found {len(docs)}", file=sys.stderr)
        sys.exit(1)

    all_issues = []
    for d in docs:
        issues = check_doc(d)
        if issues:
            for issue in issues:
                all_issues.append((d, issue))

    if all_issues:
        print(f"FAIL: Found {len(all_issues)} issue(s) in documentation set:", file=sys.stderr)
        for doc_path, issue in all_issues:
            print(f"  {doc_path.relative_to(ROOT_DIR)}: {issue}", file=sys.stderr)
        sys.exit(1)

    print(f"PASS: All {len(docs)} documentation files exist, are non-stub, and links resolve.")
    sys.exit(0)

if __name__ == "__main__":
    main()
