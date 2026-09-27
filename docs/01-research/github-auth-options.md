# GitHub authentication

**Verified 2026-09-27.** GitHub changes its auth surface; re-verify before implementing and record the outcome in `15-appendix/references.md`.

## The three options

| | OAuth App | GitHub App | Fine-grained PAT |
|---|---|---|---|
| Who runs it | We do | We do | The user does |
| Setup for the user | One tap | One tap, plus an install step | Create a token by hand |
| Token lifetime | Long, refreshable | Short (≈1 h), auto-refreshed | User chooses, up to 1 year |
| Scope control | Coarse per permission | Per-repository, per-permission | Per-repository, per-permission |
| Best for | Our own single-user app | A product serving many users | Zero-setup, full control |
| Refresh | Manual or via a token endpoint | Automatic | None; user re-creates |

## What we ship

**Both, per the user's decision.** The OAuth path is the default in the UI because it is the least effort. The token path exists because it requires no app registration at all, which matters for an open-source project that anyone can build.

### Path A — OAuth (recommended)

A plain OAuth App with the device flow or a custom-URI callback.

Scopes requested, and no more:

| Scope | Why |
|---|---|
| `repo` | Clone, read, and push to private repositories. Required; the app is private-repo-only. |
| `read:user` | Display the account name and avatar in the UI. |
| `notifications` | Deliver PR comments, review requests, and check results as phone notifications. |

Scopes **not** requested: `admin:org`, `delete_repo`, `gist`, `user`, `workflow`. A deletion scope would be a lie about what the app can do; the app cannot delete, by policy.

Flow:

1. Tap "Mit GitHub anmelden".
2. The app opens the authorization URL in a custom tab (`app://github/callback`).
3. The user approves; GitHub redirects to our scheme.
4. We exchange the code for a token, store it in the Keystore, and immediately validate it with `GET /user`.
5. The token is refreshed before expiry and on `401` once, then the user is asked to sign in again.

The user sees the exact scope list on GitHub's own screen. Our screen repeats it in plain language before they leave the app, because that is where they actually read it.

**Deployment consequence:** an OAuth App has one fixed client ID. For an open-source project, that means a shared client ID whose redirect URL must be registered. We document the app's client ID as a public constant — client IDs are not secrets — and we accept that a fork must register its own for full isolation. The token path is the escape hatch for forks.

### Path B — Fine-grained PAT (zero setup)

The user creates a token in GitHub's settings. Our screen walks through it and lists what to select:

| Setting | Value |
|---|---|
| Token name | `claude-code-android` |
| Expiration | 90 days (we recommend this, we cannot enforce it) |
| Repository access | Only the repositories you choose |
| Permissions | **Contents**: read & write · **Metadata**: read-only · **Pull requests**: read & write · **Notifications**: read |

Permissions deliberately excluded: administration, secrets, workflows, and delete. There is no delete permission we can grant, because we do not need one.

We validate the token the moment it is entered, by listing the repositories it can see. If the list is empty, the common causes are shown: the token was not given repository access, or it was not given Contents.

### Path C — `gh` CLI token (advanced)

If the user already uses `gh auth login` on a machine reachable as a remote runner, we can read the credentials from the runner's own store. This is offered only in the remote-runner setup screen, is never a requirement, and is clearly labelled as advanced.

## Storage

- Tokens are encrypted with a Keystore-held AES-256-GCM key. The key lives in secure hardware where the device has it.
- The token is never written to logs, never included in a diagnostics export, never sent to our own infrastructure — there is no infrastructure.
- The app has no network endpoint of its own. It talks to `api.github.com` and to the provider, and to nothing else.
- Uninstalling the app destroys the key and the tokens with it.

## What the app does with the token

| Operation | Scope used | Notes |
|---|---|---|
| List repositories | `repo` | Filtered to private and owned |
| Clone | — | Local git, not the API |
| Read a file | `repo` | Only inside a cloned project |
| Create a branch | `repo` | Local git |
| Commit | — | Local git |
| Push | `repo` | To a feature branch only, and only after verification passes |
| Open a pull request | `repo` | After push |
| Read PR state | `repo` | For notifications |
| Notifications | `notifications` | Polling, see below |

## Notifications: webhook or polling

| | Webhook | Polling |
|---|---|---|
| Latency | Seconds | 30 s to 5 min |
| Setup | Needs a public endpoint, i.e. a server | None |
| Cost | A server to run | A few API calls per minute |
| Works offline | No | No |
| Fits "no third-party server" | No | Yes |

**We poll.** The whole product promise is that no third-party server is required, and a webhook needs one. Polling is a small cost against `notifications`, backs off when nothing is happening, and stops entirely when the app has been closed for a while.

Poll interval: 60 s while the app is in the foreground, 5 min when backgrounded with an active run, 30 min otherwise, and no polling at all below a configurable battery threshold.

## Rate limits

Unauthenticated: 60/hour. Authenticated: 5,000/hour for most REST endpoints, with a separate, much lower limit on secondary limits that apply to search and some list endpoints.

We read `x-ratelimit-remaining` and `x-ratelimit-reset` and show a warning below 10 %. A secondary-rate-limit response backs off and reports it, rather than retrying in a tight loop.

## Revocation

- The settings screen has "GitHub-Verbindung trennen", which deletes the token from the Keystore and revokes it server-side via `DELETE /applications/{client_id}/token` where the OAuth path supports it.
- The screen states plainly what stops working afterwards: new clones, pushes, PRs, and GitHub notifications. Local projects keep working, because they do not need GitHub.
- **Remote branches and repositories are never deleted on disconnect.** That is a hard block. Disconnecting leaves them exactly where they are.

## Fork support

A fork of this project must be able to run without our OAuth app. The token path is therefore not an afterthought: it is the path a fork will use, and the UI leads with it in that case. `01-research/legal-and-trademark.md` covers the naming and branding constraint for forks.

## Open items

- Verify the current OAuth device flow support and endpoint. **TBD — verify at build time.**
- Verify whether a self-hosted runner's `gh` credentials are readable without extra permissions on a current GitHub Actions runner. **TBD — verify during phase 5.**
- Decide whether to ship a shared client ID or require configuration. Leaning towards: ship it, document it loudly, and detect a fork by the absence of a configured ID. **TBD — decide in phase 5.**
