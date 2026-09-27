---
name: docs-authoring
description: The structure, voice, and evidence rules for the 135 documents in docs/. Use before writing or changing any document, and when deciding whether something belongs in a doc or in the code.
---

# docs-authoring

135 documents. They are a deliverable, not overhead, and they are only worth
having if something keeps them true.

## The five rules

1. **A document describing behaviour that no longer exists is a bug.** Change
   the document in the same commit as the code. `/doc-sync` exists for this.
2. **Measurements are measured.** A colour carries its contrast ratio. A size
   carries its value. A quota carries the date it was checked. An unverified
   fact is written `TBD — verify at build time`, never as a plausible guess.
3. **One document, one job.** If the title needs two "and"s, split it.
4. **Do not restate the spec.** The spec holds the decision. These documents hold
   the detail. Link, do not duplicate.
5. **Example code is real code**, copied from the repository. If it is
   illustrative, it says so.

## Structure

Every document has, in this order:

```markdown
# Title

<one paragraph: what this is for, and who it is for>

<the content, with tables for genuinely tabular data>

## Depends on

<the documents this one assumes, as bare paths>
```

The closing `## Depends on` line is not optional. It is how a reader with no
context knows where to go next, and how the link checker verifies the set.

## Voice

Direct and specific. Second person. No hedging, no throat-clearing, no
"it is important to note". State the decision and the consequence. A document
that says "this can be configured" without saying what the trade-off is has not
finished the thought.

**Banned prose:** "seamless", "powerful", "revolutionary", "effortless",
"leverage", "unlock", "elevate", "delve", "robust", "harness", "journey",
"in today's fast-paced world", "furthermore", "moreover", "in conclusion".
The full list is in `docs/03-design/anti-slop-rules.md` section 24.

**Also banned:** exclamation marks, three-bullet lists with no content, and any
sentence that would make sense on a completely different product's page.

The test: would a competent engineer reading this learn anything they could not
have inferred? If not, cut it.

## Never invent

A version number, a free-tier quota, a checksum, an API shape, a licence, or a
performance figure. Look it up, record the access date, and cite the source in
`docs/15-appendix/references.md`. A fabricated reference is worse than an
honest gap, because it will be trusted.

## Before you finish

```bash
python3 tools/check_doc_manifest.py          # present and non-stub
grep -rn 'TODO\|FIXME\|coming soon' docs/     # each needs an owner and a link
```

And check that every path you reference exists. A broken link makes a reader
distrust the whole set immediately.

## When in doubt

Write it down. An unwritten behaviour gets invented three times, and two of the
inventions will disagree.
