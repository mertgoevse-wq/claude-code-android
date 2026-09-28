# Task Plan: Claude Code Android Implementation

## Goal
Build the unofficial, independent Android app port of Claude Code on Galaxy A56 in PRoot Debian, completely fulfilling the 135-document contract, all 7 build phases, all hard blocks, and 100% green CI.

## Phases

### Phase 0 — Foundation
- **Status:** complete
- **Tasks:** P0-1 through P0-22 completed and merged to `main` via PR #2.

### Phase 1 — Design System
- **Status:** complete
- **Tasks:**
  - P1-1..P1-6: Design tokens, typography, spacing, motion, theme implementation.
  - P1-7: Primitives (Button, TextField, Card, Chip, BottomSheet, AppScaffold).
  - P1-8: Dynamic components (CodeBlock, StreamingText, ToolCard).
  - P1-9: Terminal ANSI parser and contrast-checked palette.
  - P1-10: Tabler icon set in `AppIcons` with zero trash icons.
  - P1-11, P1-12: `AnimatedClaudeMark` with 4 teardrop lobes, idle breathing sine loop, and working character state machine covering 11 states.
  - P1-13: Anti-slop review pass green.
  - P1-14: Contrast verification in `tools/check_token_usage.py` green.
  - Pull Request #3 created for `task/phase-1-design-system`.

### Phase 2 — Data and Core
- **Status:** in_progress
- **Tasks:**
  - [x] P2-1: Domain models for all 28 entities in `shared/domain`
  - [x] P2-2: Use cases: project lifecycle (Create, Clone, Rename, Archive/Unarchive, Get, UpdateSettings)
  - [ ] P2-3: Use cases: chat and run lifecycle
  - [ ] P2-4: Use cases: permission policy and autonomy levels
  - [ ] P2-5: Use cases: budget, plans, verification
  - [ ] P2-6: Room schema and DAOs in `shared/data`
  - [ ] P2-7: Database migrations with tests
  - [ ] P2-8: Repositories in `shared/data` with in-memory Room tests
  - [ ] P2-9: SecretStore on Android Keystore, SecretRef everywhere

### Phase 3 — Runtime
- **Status:** pending
- **Tasks:** P3-1 through P3-15 (Native glibc runner, PRoot profile, PTY bridge, terminal renderer, health checks).

### Phase 4 — Chat and Projects
- **Status:** pending
- **Tasks:** P4-1 through P4-18 (Message model, live tool cards, plan view, cost meter, diff viewer, chat detail, journey 1).

### Phase 5 — GitHub, Skills, Remote Runners
- **Status:** pending

### Phase 6 — Polish and Delivery
- **Status:** pending

### Phase 7 — Verification
- **Status:** pending

## Next Step
Implement Task P2-3: Use cases for chat and run lifecycle (Conversation, Turn, Message, Run, Plan, Verification).
