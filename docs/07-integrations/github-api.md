# GitHub API

The REST endpoints this app uses, what each is for, and the rules around them.

## Scope of the integration

| Uses the API for | Uses git locally for |
|---|---|
| Listing repositories | Cloning |
| Creating a pull request | Branching, committing |
| Reading pull request state and checks | Pushing |
| Reading comments and reviews | Anything that does not need a round trip |
| Repository metadata | File contents, which are already local |

**The split is deliberate.** Anything that can be done with the git binary is done with the git binary. The API is for the things git cannot do: pull requests, checks, and the account's repository list. A round trip through an API for something a local command does better would add latency, a rate-limit cost, and a failure mode for no benefit.

## Endpoints

### Repositories

| Endpoint | Method | Used for |
|---|---|---|
| `/user` | GET | Validate a token, get the login and avatar |
| `/user/repos` | GET | The repository list for the add-project flow. `visibility=private`, `sort=pushed`, paginated at 50 |
| `/repos/{owner}/{repo}` | GET | Metadata, the default branch, the size, the language |
| `/repos/{owner}/{repo}/git/refs/heads/{branch}` | GET | Confirm a branch exists |

**Public repositories are never listed or offered.** The app does not work with them, and offering a choice that leads to a dead end is a small dishonesty that costs a person ten minutes.

### Branches and pulls

| Endpoint | Method | Used for |
|---|---|---|
| `/repos/{owner}/{repo}/pulls` | POST | Open a pull request |
| `/repos/{owner}/{repo}/pulls` | GET | List a project's open pull requests |
| `/repos/{owner}/{repo}/pulls/{number}` | GET | One pull request's state |
| `/repos/{owner}/{repo}/pulls/{number}/commits` | GET | Whether the pushed commits arrived |
| `/repos/{owner}/{repo}/pulls/{number}/reviews` | GET | Review state |
| `/repos/{owner}/{repo}/pulls/{number}/comments` | GET | Review comments, for notifications |
| `/repos/{owner}/{repo}/issues/{number}/comments` | GET | Conversation comments |

### Checks

| Endpoint | Method | Used for |
|---|---|---|
| `/repos/{owner}/{repo}/commits/{ref}/check-runs` | GET | CI status, per commit |
| `/repos/{owner}/{repo}/commits/{ref}/status` | GET | The older status API, for a project that has not migrated |

A pull request's state is computed from three sources: its own `state`, the check runs on its head commit, and the mergeable flag. All three, because any one of them alone can be misleading.

### Rate limits

| Property | Value |
|---|---|
| Headers read | `x-ratelimit-limit`, `x-ratelimit-remaining`, `x-ratelimit-reset` |
| Below 10 % | A warning in the UI with the reset time |
| A secondary rate limit | A backoff, and a message. Secondary limits apply to search and some list endpoints, and a tight retry loop makes them worse. |
| A 403 with a rate-limit indication | Treated as rate limiting, not as a permissions problem. The two share a status code, and guessing wrong produces a confusing message. |
| A 401 | One refresh attempt, then the sign-in screen |
| Caching | Repository metadata for 5 minutes, pull request state for 30 seconds. Both keyed by ETag, and a `304` costs nothing against the limit. |

## Idempotency

Several operations can be retried, and a retried POST that creates a duplicate is a real problem.

| Operation | Idempotent | Approach |
|---|---|---|
| Opening a pull request | No | Before opening, `GET /pulls?head={owner}:{branch}&state=open`. If one exists, link to it instead. |
| Reading state | Yes | Naturally |
| Listing repositories | Yes | Naturally |
| Nothing else writes | — | The app creates no issues, no comments, and no files on GitHub |

**The app writes to GitHub exactly one thing: a pull request.** No comments are posted, no issues are opened, no labels are set, no workflows are dispatched (except in the Actions runner, which is a different thing entirely). This is a deliberate scope limit and it keeps the token's permission surface small.

## Errors

| Status | Meaning | Handling |
|---|---|---|
| 401 | The token is invalid or expired | One refresh, then the sign-in screen |
| 403 with a rate-limit header | Rate limited | Wait until the reset, and say when |
| 403 without | Missing scope | Name the missing scope, in plain language |
| 404 | Not found, or no access | The two are indistinguishable, and the message says so: "Das Repository ist nicht erreichbar oder für dieses Konto nicht sichtbar." |
| 409 | A conflict | Usually a branch that already exists. Report it; never force. |
| 422 | Validation failed | The message from the API, which is specific, shown as-is |
| 429 | Rate limited | The same as a secondary limit |
| 5xx | GitHub's problem | Retry with backoff, up to three times |

**A 404 is never reported as "the repository does not exist".** A private repository the token cannot see returns 404, and telling somebody their repository has been deleted is both wrong and alarming. The message names both causes.

## The pull request body

Written by the app, containing what somebody reviewing the change in six months would want.

```markdown
## Aufgabe

Unterstützung für benutzerdefinierte Prüfbefehle einbauen.

## Plan

- [x] Prüfbefehle aus dem Projekt erkennen — automatisch geprüft
- [x] Prüfbefehle im Code konfigurierbar machen — automatisch geprüft
- [x] Tests schreiben — nicht automatisch prüfbar
- [x] Alles bauen und prüfen — 3 von 3 bestanden

## Prüfung

| Befehl | Ergebnis | Dauer |
|---|---|---|
| `./gradlew assembleDebug` | bestanden | 0:48 |
| `./gradlew test` | bestanden | 1:12 |
| `./gradlew lint` | bestanden | 0:14 |

## Metadaten

| | |
|---|---|
| Lauf | `01HQ8X2M4K7P…` |
| Autonomie | `ASK_RISKY` |
| Claude Code | 2.1.283 |
| Profil | Native |
| Gerät | Android 15 |
| Kosten | 0,31 $ |
| Skills | `gradle-test-helper` |

<sub>Erstellt mit Claude Code Android · inoffiziell, nicht von Anthropic.</sub>
```

| Rule | Detail |
|---|---|
| The task, verbatim | Somebody reading the pull request should not have to guess what was asked |
| The plan's checkboxes | Ticked by verification, not by the agent's belief. An unverifiable step is ticked with a note saying it could not be checked. |
| The verification table | Every command, with its result and duration. A failed verification appears here, not only in the app. |
| An unverified run | The heading says `**Nicht geprüft**` and the metadata row says so too |
| The unofficial line | A single line at the bottom. Not a disclaimer paragraph. |
| The cost | Included, because it is a fact about the change and the reader may want it |
| No auto-generated noise | No "this pull request was generated automatically", no emoji, no badges, no "please review" boilerplate |

## Pagination

| Endpoint | Strategy |
|---|---|
| Repository lists | 50 per page, lazily, with "weitere laden" and a count of what remains |
| Pull request lists | 30 per page, paged in the UI |
| Comments | The first page, most recent, with a total count. Nobody scrolls to comment 200. |
| Large repositories | The app requests 100 items over a 50-item limit, which GitHub honours, and caps at 10 pages before reporting that the list is truncated |

**Truncation is always stated.** "Zeige 500 von 1.240 Repositories" is honest. Silently showing the first 500 is not.

## Concurrency and caching

| Concern | Approach |
|---|---|
| Concurrent requests | A single queue per host, so a screen that fires six calls gets six serialised requests rather than a burst |
| Caching | An in-memory cache with an ETag, and a small on-disk cache for the repository list so the add-project screen is instant offline |
| Offline | The cached repository list is shown, marked as cached, with the fetch date. Adding a project by URL still works offline. |
| Staleness | The cache entry carries its fetch time and the UI shows it |
| Invalidation | A pull request write invalidates that repository's pull request cache |
| A large cache | Bounded at 200 repositories, evicting the least recently used. The cache is a convenience, not a copy of the user's account. |

## The base URL

`https://api.github.com`, hard-coded. **No GitHub Enterprise support in v1.** A configurable base URL is a small feature that a great many people would need and a small number would use, and it is a decision to make when somebody asks rather than to build speculatively.

## What the app never does

| Never | Reason |
|---|---|
| Push to the default branch | `PushPolicy`, per `05-features/project-lifecycle.md` |
| Make a repository public | There is no API call that does it and no UI that offers it |
| Delete a branch, a tag, a repository, a file, or a comment | A hard block, and no such call exists in `GitHubClient` |
| Close a pull request, merge it, or comment on it | Out of scope. The app prepares work; a person reviews and merges it. |
| Modify repository settings | Out of scope, and a large permission surface |
| Add a collaborator | Never |
| Use a webhook | It would require a server, per ADR-013 |
| Write anything but a pull request | Keeps the permission surface at the minimum |

## Testing

| Test | Type |
|---|---|
| `EndpointCoverage` | Unit — every repository, branch, and pull request action in the app maps to exactly one documented endpoint, and there are no undocumented calls |
| `NoWriteEndpoints` | Unit — `GitHubClient` has no method for a delete, a merge, a close, a comment, a settings change, or a visibility change |
| `AuthHeaderNeverLogged` | Unit — the Authorization header appears in no log, no exception message, and no diagnostics bundle |
| `RateLimitParsing` | Unit — remaining, reset, and the 403-without-header case |
| `RateLimitWarning` | UI — below 10 %, with the reset time |
| `SecondaryBackoff` | E2E — a 429 backs off and does not hammer |
| `NotFoundMessage` | UI — asserts the message names both causes and never says a repository was deleted |
| `PrIdempotency` | E2E — two open attempts produce one pull request, and the second links to the first |
| `PrBodyContents` | E2E — every required section, the plan's checkboxes reflecting verification, the unofficial line, and no boilerplate |
| `PrBodyUnverified` | E2E — an unverified run's body says so in the heading and the metadata |
| `Pagination` | E2E — 1.240 repositories, capped and honestly truncated |
| `CacheOffline` | E2E — the add-project screen works offline from cache, marked with a date |
| `Etag` | Integration — a `304` is handled and costs no limit |
| `SerialisedQueue` | Integration — six concurrent calls produce six serialised requests |
| `PublicNeverOffered` | UI — no public repository appears in the list, asserted against a fixture containing both |
| `RetryOn5xx` | E2E — three attempts, then a specific error |
