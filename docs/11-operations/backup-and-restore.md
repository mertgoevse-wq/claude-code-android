# Backup and restore

Projects, sessions, settings, and history are the user's work. Restoring them onto a new phone is a feature, not a nice-to-have. Deleting any of it is not a feature at all.

## The rule that outranks everything here

**Nothing in the app's data directory is ever deleted by the app, except caches and rotated logs.**

No uninstall-cleanup path, no "free up space" button that touches conversations, no retention policy on runs, no TTL on projects, no auto-prune of old transparency logs that a run depends on. An archived project is marked, not removed. A run log is kept with the run. The only deletions are:

| Deletable | What it is |
|---|---|
| Download cache (the Claude Code archive, re-downloadable) | Bytes, not work |
| Log rotation beyond the run's own log | Diagnostics, not work |
| A user-initiated export file after its share window | Already delivered |
| Temporary build outputs in a project working tree | The user's VCS owns those, not us |

`05-features/project-lifecycle.md` and `08-orchestration/permissions.md` both restate this, and `scripts/check-hard-blocks.sh` asserts that no code path deletes a `Project`, `Session`, or `Run` row.

## What is in a backup

| Data | Included | Format |
|---|---|---|
| Projects (metadata, autonomy level, verification commands, provider selection) | Yes | JSON |
| Working trees | **By reference, not by content** | A list of the project directory and whether it is a git working tree |
| Sessions and messages | Yes | JSONL, the same format the transparency log uses |
| Run records (verification results, token counts, cost, duration, tool calls) | Yes | JSON |
| Transparency logs | Yes | JSONL |
| Skills (installed, including edits) | Yes | The skill directory, verbatim |
| Settings | Yes, except secrets | JSON |
| Key profiles | **Never.** Presence and provider kind only | — |
| Remote runner credentials | **Never.** Presence and host only | — |
| The runtime binary | No. Re-downloadable, ~300 MB | — |
| Caches and logs | No | — |

A backup is not a snapshot of the working trees. Copying a user's source code into our backup format would be both enormous and presumptuous — the files are already in a git repository, and a local directory is a directory. What we back up is the thing that is *only* here: the conversation, the settings, the run history, the skills, and the knowledge of what a project is.

## Format

A single file: `cc-android-backup-<version>-<timestamp>.cca`, which is a `.zip` with:

```
manifest.json          version, app version, created-at, item counts, per-item checksum
projects.json
sessions/<projectId>.jsonl
runs/<projectId>/<runId>.summary.json
transparency/<runId>.jsonl
skills/…               verbatim
settings.json
README.txt             what this is, in German and English, plain text
```

`manifest.json` carries a SHA-256 per item, so a restore can detect a truncated or corrupted archive and say which item is bad instead of importing half of it.

## Encryption

The backup is encrypted with a passphrase the user chooses. Not a key from the device, because the point is to move to a new device.

| Property | Value |
|---|---|
| Cipher | XChaCha20-Poly1305, via Tink |
| KDF | Argon2id, 64 MiB, 3 iterations, per-file random salt |
| Versioning | Stored in the header, so a future format can be added |
| Key | Derived from the passphrase; the passphrase is never stored |
| Recovery | None. A forgotten passphrase means the backup is unreadable, and the app says so plainly at the point of creating it, not afterwards |

Argon2id parameters are chosen so a mid-range phone derives in about a second and a laptop in about 100 ms. The app shows the derivation cost in the passphrase dialog, because hiding it would be dishonest about a real trade-off.

## Backup triggers

| Trigger | Behaviour |
|---|---|
| Manual | Settings → "Backup erstellen", picks a destination via the system file picker |
| Before an app update that changes the schema | Prompted, once, with the reason stated |
| After a destructive-looking operation | Never — there are none |
| On a schedule | Off by default. Android's own backup covers the database; see below. |

## Android's own backup

`android:allowBackup` is `true` for the database and settings, with an explicit exclusion list:

```xml
<full-backup-content>
    <include domain="database" path="cc-android.db"/>
    <include domain="sharedpref" path="settings.preferences_pb"/>
    <exclude domain="file" path="keys/"/>          <!-- Keystore material -->
    <exclude domain="file" path="runtime/"/>       <!-- re-downloadable -->
    <exclude domain="file" path="logs/"/>
    <exclude domain="database" path="cc-android.db-wal"/>
</full-backup-content>
```

Keystore material is excluded because it is useless off the device — it is bound to this device's keystore. Including it would produce a restore that fails cryptically. The key profiles themselves are in the encrypted backup format if the user wants them, which is the right way: a passphrase the user chose, rather than a blob that silently does not work on the new phone.

## Restore

1. Pick a `.cca` file.
2. Enter the passphrase. Three failures and the app stops offering it — an unlock attempt limit is the one place a retry count genuinely matters.
3. Verify the manifest checksums. A mismatch names the item.
4. Show a **preview**: what will be imported, how many projects, how many runs, and what will conflict. Nothing is written yet.
5. Confirm.
6. Import item by item, each in its own transaction. A failure part-way leaves the imported items intact and the rest listed as not imported — it never rolls back the app to a half state.
7. Offer to open each restored project.

### Merge semantics

Restoring onto a populated app is supported and is the common case (a new phone after restoring the old one's Android backup).

| Item | Rule |
|---|---|
| Settings | Restored values overwrite; values not in the backup are kept |
| Project with no local counterpart | Imported |
| Project with a matching local one | The **local** one wins; the imported one is shown for comparison and the user chooses per field |
| Session | Imported as a new session; identifiers are rewritten to avoid collision |
| Run | Imported, linked to its session, marked as restored |
| Transparency log | Appended, never merged; a duplicate run ID gets a suffix |
| Skills | An installed skill that differs is shown as a diff; the user chooses |

The rule everywhere: **an import never overwrites newer local data with older imported data without asking.**

## What restore cannot do

Stated plainly in the UI, because a user who assumes otherwise will be badly surprised:

- It does not bring back a deleted project. Nothing deletes projects, so there is nothing to bring back.
- It does not restore working-tree files. Point the restored project at the directory.
- It does not restore keys from the device backup; only from an encrypted backup.
- It does not merge two projects' file trees. There is no file-level merge, and there should not be: a merge tool that guesses is worse than one that asks.

## Backup verification

A backup that has never been restored is a hope, not a backup. The app therefore:

- Verifies every item's checksum at export time, not only at import.
- Offers "Backup prüfen" in Settings, which decrypts a chosen backup and reports per-item integrity without importing anything.
- Records the last backup's item counts and timestamp in the settings, shown as "Letztes Backup: vor 3 Tagen, 4 Projekte, 118 Läufe" — because a number nobody looks at is a backup nobody verifies.
