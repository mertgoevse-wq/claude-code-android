# GitHub authentication

Two ways in, one stored token, and a small set of permissions. The comparison of the two is in `01-research/github-auth-options.md`; this is the implementation.

## Flows

### OAuth

| Step | Detail |
|---|---|
| 1 | The user taps "Mit GitHub anmelden" |
| 2 | A `Custom Tab` opens the authorization URL. Not an external browser, because returning to the app matters |
| 3 | GitHub shows the app's name and the **exact scope list**, on GitHub's own screen |
| 4 | The user approves; GitHub redirects to `app://github/callback?code=…&state=…` |
| 5 | `state` is verified against the value we generated. A mismatch aborts. This is the CSRF check and it is not optional. |
| 6 | The code is exchanged for a token, over a direct POST, never through a redirect |
| 7 | `GET /user` validates it |
| 8 | It is stored in the Keystore |

| Failure | Handling |
|---|---|
| The user denies | A plain message. No attempt to persuade them. |
| The redirect never arrives | A timeout with a cancel, and a fallback to the token route |
| `state` mismatches | Aborted, logged as a security event, and the token is discarded |
| The exchange fails | The specific HTTP error, and the token route is offered |
| The app is killed mid-flow | The flow is lost and restarted. There is no session to resume, and pretending otherwise would mean storing state across a process death. |

### The token route

| Step | Detail |
|---|---|
| 1 | The user creates a token in GitHub's settings. The app shows a scope checklist and a link, and does not collect the token in a browser it controls |
| 2 | The token is pasted into a `Password` field, so the keyboard does not learn it and it is not in the autofill history |
| 3 | "Token prüfen" calls `GET /user` and, on success, lists the repositories it can see |
| 4 | The repository list is the real validation. A token that authenticates but sees nothing is a token without repository access, and the app says exactly that. |
| 5 | It is stored in the Keystore, masked permanently |

## Scopes

| Scope | Requested | Why |
|---|---|---|
| `repo` | Yes | Private repository access, push, pull requests. The app works only with private repositories, so this is the whole job. |
| `read:user` | Yes | The login name and avatar in the UI. |
| `notifications` | Yes | PR comments and review requests as phone notifications. |

**Not requested:** `admin:org`, `delete_repo`, `gist`, `user`, `workflow`, `admin:repo_hook`, `write:discussion`. A permission the app does not need is a permission it cannot misuse, and a token that cannot delete a repository is a token that matches the app's stated rules.

## Storage

| Property | Value |
|---|---|
| Where | Encrypted with an AES-256-GCM key held in the Android Keystore |
| The key | In secure hardware where the device has it, software-backed where it does not. The app does not claim a guarantee the device cannot make. |
| The ciphertext | In a dedicated table with no other purpose, never in `AppSetting` |
| The token in memory | Only during a request, cleared afterwards. Never in a field, never in a log. |
| Backups | The keystore key is not backed up, so the ciphertext in a backup is useless. Nothing to exclude, and nothing leaks. |
| Uninstall | The key and the ciphertext are both removed |
| A screenshot | The field is `Password`, and the stored value is masked. A reveal requires a fresh biometric check and auto-hides after 15 seconds. |

## Refresh

| Aspect | Behaviour |
|---|---|
| OAuth with an expiring token | Refreshed before expiry, and on the first 401 |
| A refresh failure | One attempt, then the sign-in screen, with the reason |
| A user-supplied token | Never refreshed. It has no refresh token. When it expires, the user is told, with a link to create a new one. |
| A revoked token | A 401 on any call, one refresh attempt, then the sign-in screen: "Die Verbindung zu GitHub wurde aufgehoben." |
| Proactive check | Every 24 h, a cheap `GET /user`, so an expiry is discovered before a run needs it rather than during one |

## Disconnecting

| Step | Text |
|---|---|
| 1 | "GitHub-Verbindung trennen?" |
| 2 | "Danach funktionieren keine neuen Klone, keine Pushes und keine Pull Requests mehr. Lokale Projekte funktionieren weiter." |
| 3 | "**Es wird nichts auf GitHub gelöscht.**" |

| Then | Detail |
|---|---|
| The token is deleted | From the Keystore |
| The token is revoked | Server-side, where the flow supports it |
| Remotes | Left in the local repositories, exactly as they are. Nothing is unlinked, because unlinking is a small destruction of a user's configuration. |
| Local projects | Keep working. A local project with a GitHub remote simply cannot push until the connection is back. |
| The log | An entry, because a connection change is a security-relevant event |

## The dedicated error-repair token

Per `05-features/field-error-triage.md`, the self-repair feature needs a token scoped to this app's own repository.

| Property | Value |
|---|---|
| Scope | One repository: this app's |
| Permissions | Contents: read and write. Nothing else. It cannot open an issue, cannot read anything else, cannot administer. |
| Stored | In the Keystore, like any other, under its own name |
| Shown | In About, permanently, with the repository and the scope in plain words |
| If not set | The feature says so and offers export only |

## Client id

| Aspect | Value |
|---|---|
| Shipped | A public constant. A client id is not a secret; it is an identifier. |
| The secret | **Never shipped.** For a public client using the authorization-code flow with PKCE, there is no client secret. |
| PKCE | Used. S256, a verifier generated per flow. This is what makes a public client safe without a secret. |
| The redirect | `app://github/callback`, registered by the app's owner |
| Forks | A fork that has no configured client id sees the token route as primary, with a note explaining why. It does not silently use ours. |
| The state | A fresh random value per flow, verified on return, never reused |

## Hardening

| Concern | Control |
|---|---|
| Token in a URL | Never. A token in a URL ends up in logs, in history, and in a referrer header. |
| Token in a log | Never. `Redactor` plus a test that asserts the `Authorization` header appears nowhere in any sink. |
| Token in an exception message | Never. Ktor exceptions are constructed with the request redacted. |
| Token in a diagnostics bundle | Never, asserted by the same test. |
| TLS | Certificate pinning is not required and not used. A pinned certificate breaks when GitHub rotates, and the user would have no way to fix it. |
| The custom tab | `CustomTabsIntent` with a verified origin. A web view is not used for a login, because a web view is the wrong place for a credential. |
| The redirect intent | Verified to come from the browser, with the expected scheme and host, before anything is read from it. |
| Screenshots | The token field is masked, and the reveal auto-hides. |

## The connected state

| Screen | What it shows |
|---|---|
| Settings, Anbieter | "GitHub verbunden · @mert · vor 2 Tagen geprüft", with "Trennen" and "Neu prüfen" |
| The add-project flow | The account, with "Konto wechseln" |
| The About screen | The permissions, in plain words, permanently |
| The project detail | Per project: "Verbunden", or "Kein Zugriff auf dieses Repository" with the likely cause named |

An expired or revoked connection shows as a `warning` row in Settings with the reason, not only when a run needs it. Finding out at the end of a two-hour run that the token expired an hour ago is the failure this prevents.

## Testing

| Test | Type |
|---|---|
| `OAuthFlow` | E2E — against a mock authorisation server, the full flow, ending with a validated token |
| `StateVerified` | E2E — a mismatched `state` aborts, logs a security event, and stores nothing |
| `PkceFlow` | E2E — the verifier is sent and is per-flow, never reused |
| `NoClientSecret` | Static — no client secret exists in the source or the resources |
| `TokenInUrl` | Static — no token is ever placed in a URL, asserted by a check on the client configuration |
| `ScopesExact` | Unit — the requested scope set is exactly the three documented, and a test fails if one is added |
| `TokenStorage` | E2E — the token round-trips through the Keystore, and the plaintext appears nowhere on disk |
| `TokenInBackup` | E2E — a restored backup cannot decrypt the token, because the key is not restored |
| `TokenMasked` | UI — the field is a password field, the stored value is masked, a reveal requires biometrics and auto-hides |
| `TokenRefreshOn401` | E2E — one refresh on a 401, then the sign-in screen if it fails |
| `UserTokenNeverRefreshed` | E2E — a user token is never refreshed; expiry produces the create-a-new-token message |
| `ProactiveCheck` | Integration — a 24 h check, and an expiry is surfaced in Settings before a run needs it |
| `Disconnect` | E2E — the token is deleted and revoked, remotes are untouched, local projects still work, and the log records it |
| `DisconnectSaysNothingDeleted` | UI — asserts the "nichts auf GitHub gelöscht" line is present |
| `NoWebViewLogin` | Static — no `WebView` is used in the authentication flow |
| `RedirectVerified` | E2E — an intent with an unexpected scheme or host is ignored |
| `RepairTokenScope` | UI — the repair token's repository and permissions are shown in About at all times |
| `TokenNeverInBundle` | E2E — a diagnostics bundle with a token seeded in every place contains none |
| `ExpiredShowsEarly` | E2E — an expired token produces a Settings warning, discovered by the proactive check rather than by a run |
