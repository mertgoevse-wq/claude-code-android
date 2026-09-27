---
name: kmp-shared
description: Rules for keeping shared/ portable across Android and iOS. Use before writing any import in shared/core, shared/domain, shared/data, shared/runtime, shared/orchestration, shared/skills, shared/vcs, or shared/ui.
---

# kmp-shared

`shared/` is written for portability from day one, not retrofitted
(spec decision D20). The iOS target is a scaffold, so "portable" currently
means one thing: **enforceable now, so the door is not welded shut later.**

## The hard rule

`shared/core` and `shared/domain` import **nothing** from Android or from any
JVM-only library.

```bash
python3 tools/check_no_android_imports_in_shared.py
```

This runs in CI. There is no suppression comment. If the check fires, the code
is in the wrong module: move it to `androidApp/`.

## Expect/actual, not leaking the platform

```kotlin
// shared/core — the interface
expect class FileSystemGateway {
    suspend fun read(path: String): String
    suspend fun write(path: String, content: String)
    suspend fun exists(path: String): Boolean
}

// androidApp — the implementation
actual class AndroidFileSystemGateway : FileSystemGateway { /* ... */ }
```

A gateway that is not yet needed on iOS is fine — it is a single `actual` later,
and it is much cheaper than untangling an Android type from a use case.

Banned in `shared/core` and `shared/domain`: `android.*`, `androidx.*`,
`java.io.*`, `java.nio.file.*`, `javax.*`, Ktor engine classes, Room
annotations on a domain type, and Compose runtime types in a domain model.

Allowed in `shared/data`, `shared/runtime`, `shared/ui`: Ktor, Room, Compose —
all in their multiplatform form. Check that an artifact actually ships a
common or `jvm`+`ios` variant before depending on it.

## The layer rules that CI cannot check for you

| Rule | Why it exists |
|---|---|
| No DAO is called from a `ViewModel` | The ViewModel must be testable without a database |
| Feature code never branches on the runtime profile | `RuntimeProfile` is data. `if (profile == PROOT)` in a feature is a bug |
| Feature code never branches on the execution backend | `ExecutionBackend` is the boundary. The chat screen is identical on a phone and on a cloud VM |
| Domain knows no framework | Room annotations on a domain entity couple the rule to the storage |
| Errors are typed at the boundary | A bare exception crossing a repository is a crash on a phone |

## Adding a dependency

Before adding one:

1. Is it multiplatform? If not, does it belong in `androidApp/`?
2. Is it FOSS, and is the licence in `THIRD_PARTY_NOTICES.md`?
3. Is the reason written in `docs/10-build/dependency-versions.md`?
4. Does `tools/check_foss_dependencies.sh` still pass?

Four "no" answers means the code goes in `androidApp/`, not the dependency list.

## Test it

```bash
python3 tools/check_no_android_imports_in_shared.py
./gradlew :shared:domain:check :shared:core:check
```

Both green before the change leaves your hands. Portability is a property of
the build, not an intention.
