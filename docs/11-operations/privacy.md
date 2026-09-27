# Privacy

The default answer to "does this app send anything anywhere?" is *nothing, unless you asked it to*. Every exception is listed here, and each one is something a user can turn off.

## The one-paragraph version

This app talks to exactly three kinds of service: the AI provider you configured, the code host you connected, and — if you set it up — the machine you told it to run code on. It collects nothing, has no analytics, contains no advertising identifier, and has no crash reporting unless you turn it on. Your keys never leave the device except to be sent to the provider you chose, and they are stored encrypted with a key held by the Android Keystore.

## What leaves the device

| Destination | When | What is sent | Can it be turned off |
|---|---|---|---|
| The AI provider (Anthropic, OpenAI, or your own) | Every model request | The prompt, the conversation, the relevant files, the tool results | Yes — the app is useless without it, so this is a provider removal, not a switch |
| The code host (GitHub) | When you connect a repository | OAuth token, API calls for repos, branches, PRs, checks, notifications | Yes |
| Your runtime host (Oracle, a home PC, a server) | When you configure one | SSH, the code, the commands, the results | Yes |
| A crash backend | Only if you opt in | The redacted report | Yes, and the default is off |
| A self-update server (the GitHub release) | When you check for updates | A plain GET, plus your current version | Yes, and it is off by default in the `dist` variant |
| Android's own backup | If you have it on | The database, per `backup-and-restore.md` | Yes, in system settings |

**That is the complete list.** If a code path causes a network request to a host not on this list, it is a bug and `scripts/check-no-secrets.sh` plus the network-security config in `res/xml/network_security_config.xml` will fail the build.

## What never leaves the device

| Never | Where it would have to be to leak |
|---|---|
| API keys, OAuth tokens, runner credentials | The Keystore-backed store; the app never has plaintext at rest |
| Prompt and assistant text outside a model request | Nowhere — it is only in the encrypted database and in the engine process |
| File contents, except as part of a model request or a build command | The working tree stays where it is |
| Project names, paths, run history | Local database only |
| The transparency log | Local database, append-only, exported only on request |
| Usage analytics, crash statistics, telemetry | There is none. See `telemetry.md`. |
| An advertising or device identifier | The manifest requests neither `AD_ID` nor any vendor identifier |
| Contacts, location, camera, microphone, SMS, call log | The manifest requests no such permission, and a CI check asserts the permission list |

## Permissions

| Permission | Needed for | Justified? |
|---|---|---|
| `INTERNET` | Provider, GitHub, runner, download | Yes |
| `ACCESS_NETWORK_STATE` | Distinguishing "offline" from "server unreachable", which changes the message shown | Yes |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_DATA_SYNC` | A long run must keep executing | Yes |
| `POST_NOTIFICATIONS` | Run progress and completion | Requested at first use, not at install |
| `WAKE_LOCK` | Keeping a run alive with the screen off | Yes, released immediately after |
| `USE_BIOMETRIC` | The app lock | Requested when the user enables it |
| `VIBRATE` | Haptics | Yes |

Everything else — storage, location, contacts, camera, microphone, the full SMS set — is not requested. `android:requestLegacyExternalStorage` is not used; file access to a user's project directory is through the system file picker, which returns a grant the user chose.

`scripts/check-hard-blocks.sh` and a CI step assert that the manifest permission set is exactly the table above. Adding a permission is a change that requires updating this document in the same pull request, deliberately.

## Storage at rest

| Data | Protection |
|---|---|
| The database (conversations, runs, settings) | SQLCipher, AES-256, key from the Android Keystore, never exported in plaintext |
| Key profiles | AES-GCM with a Keystore-held key; the key itself is non-exportable |
| Working trees | **Not encrypted by us.** They are the user's own files in their own directory, in whatever form they chose to keep them in. Claiming otherwise would be a lie. |
| Backups | The user's passphrase, Argon2id, XChaCha20-Poly1305 |
| Log files | Encrypted at rest because they are in the app's private, encrypted directory; contents are redacted on write regardless |
| Shared files | Only what the user explicitly shares, through a scoped `FileProvider` grant that the app revokes |

## What the app can see about you

Deliberately little, and stated rather than implied:

- **No analytics.** No usage counters, no session lengths, no feature adoption, no A/B assignment, no remote config. The build contains no analytics SDK — `dependency-analysis` reports the absence as a fact, and adding one is a change that has to be argued for in a pull request.
- **No device fingerprinting.** No `ANDROID_ID`, no advertising ID, no sensor-derived identifier, no IP-derived identifier stored.
- **No contact discovery.** A crash report is not linked to any identifier beyond a per-installation random value the user can clear.
- **The per-installation identifier** exists solely to correlate two diagnostics exports from the same install. It is random, generated at first launch, and clearable from Settings.

## Data deletion

Because the app deletes nothing, "delete my data" is a thing the user does, not the app does:

| What the user wants gone | How |
|---|---|
| A project | The project is a reference; the files are the user's. Nothing to delete. |
| A conversation | Not offered. See below. |
| A key profile | Swipe is not used; Settings → Key profile → "Profil entfernen", with a confirmation naming what it affects |
| A runner connection | Settings → remove, with a confirmation |
| Everything | Uninstall the app. The data directory goes with it. |

**Conversations are not deletable.** This is a deliberate refusal, taken from the same principle as the five hard blocks: this tool keeps a record of what it did, and a chat transcript is part of that record. A user who wants the record gone uninstalls the app. This is stated plainly in the app, once, in the privacy section — an unexplained absence of a delete button reads as an oversight, and it is not one.

## Third parties

| Party | Where they appear | What they can see |
|---|---|---|
| The AI provider | Whichever the user configured | The prompts sent to it — which is the product working |
| GitHub | The code host | Repository operations the user authorised |
| The runtime host | Whatever the user configured | The code and commands |
| The app store | Distribution | The package, the permissions, the version |
| A crash backend, if configured | Only on opt-in | A redacted report |

Bundled third-party code is listed in `THIRD_PARTY_NOTICES.md` with its licence. Nothing in the app phones home for licensing, analytics, or telemetry.

## Compliance posture

| Obligation | Status |
|---|---|
| GDPR data minimisation | Everything sent is necessary for the feature that sends it |
| Purpose limitation | Diagnostics exist to diagnose; they are not reused |
| Storage limitation | Crash reports 30 days; logs rotate by size; no data is retained without a reason |
| Consent | Opt-in for crash reporting, explicit for every external connection |
| Right to erasure | Uninstall, plus a clear statement that the app holds no data elsewhere |
| Children's data | Not directed at children; no age gate, because the design is for someone who installs developer tools |
| Legal requests | A self-hosted crash backend means the maintainer holds nothing. A hosted one is disclosed in Settings. |

## What we would refuse

A future request that would violate this document is a no, and the reasoning is written down in advance so it is not a negotiation later:

- Adding an analytics SDK for "just crash-free rate" — crash reporting is opt-in and sufficient.
- Adding a remote config server to toggle features — a feature flag that cannot be turned off is a feature that cannot be trusted.
- Collecting prompt content in diagnostics "temporarily, just for debugging" — it is the user's content, and "temporarily" is how it ends up permanent.
- Fingerprinting a device to correlate sessions — the installation identifier already covers the legitimate need.
