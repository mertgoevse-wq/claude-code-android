---
description: Update the docs the last change affected
---

# /doc-sync

A behavioural change without a doc change is a blocker, not a nit. A document
describing behaviour that no longer exists is a bug. This command closes that
gap.

## 1. Find what changed

```bash
git diff --name-only HEAD~1 HEAD
```

## 2. Map each changed file to its document

| Changed | The doc that owns it |
|---|---|
| `shared/domain/**` | `docs/02-architecture/data-model.md` |
| `shared/runtime/**` | `docs/06-runtime/` — the matching profile or mechanism file |
| `shared/orchestration/**` | `docs/08-orchestration/` and `docs/05-features/autonomy-levels.md` |
| `shared/data/**` providers or secrets | `docs/07-integrations/providers.md`, `secrets.md` |
| `shared/skills/**` | `docs/07-integrations/mcp.md` skills section |
| A Room entity or schema | `docs/02-architecture/data-model.md` **and** `data-migrations.md` |
| A new `AppError` | `docs/02-architecture/error-taxonomy.md` |
| A UI composable or screen | `docs/04-screens/<nn>-<name>.md` and `docs/05-features/` |
| A token value | `docs/03-design/design-tokens.md` and `color-and-contrast.md` |
| A new dependency | `docs/10-build/dependency-versions.md` **and** `THIRD_PARTY_NOTICES.md` |
| A new network host | `docs/11-operations/privacy.md` and the security threat model |
| A hard-block rule | `docs/08-orchestration/permissions.md` and the spec's section 22.1 |
| A build command or gate | `docs/10-build/local-build-and-run.md` |
| A release or version | `docs/15-appendix/changelog.md` |

If a change has no owning document, that is the finding: either the document is
missing, or the change is out of scope. Resolve it, do not skip it.

## 3. Update the doc, in the same commit

Rules from `docs/00-vision/README.md`:

1. Link, do not restate the spec.
2. Measurements are measured. A colour carries its contrast ratio; a size
   carries its value; a quota carries the date it was checked.
3. An unverified fact is written as `TBD — verify at build time`, never as a
   plausible guess.
4. Example code in the docs is real code copied from the repository. If it is
   illustrative, it says so.
5. One document, one job. If the title needs two "and"s, split it.

## 4. Check the result

```bash
python3 tools/check_doc_manifest.py
grep -rn 'TODO\|FIXME\|coming soon' docs/ | head
```

No new `TODO` without an owner and a link. No "coming soon" in anything
shipped.

## 5. Commit with the code

The doc change is not a follow-up commit. It is part of the change's commit,
because a commit whose doc lands later is a commit that was wrong when it was
merged.
