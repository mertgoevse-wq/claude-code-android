# Screen 13 — Settings

Everything configurable, in one place, with the things that matter most near the top and the things nobody should have to touch at the bottom.

## Purpose

Be complete and honest. Every setting changes real behaviour, every disabled setting says why, and the screen never becomes a wall of switches.

## Structure

A scrolling list of grouped sections, each with a `titleMedium` header. Rows are 56 dp, with a label, a value where there is one, and a chevron where the row navigates.

Advanced settings live behind a single "Erweitert" row at the bottom of the main screen, so the first-run experience is not a form.

## The main screen

### Anbieter

| Row | Value | Screen |
|---|---|---|
| Standardanbieter | "Anthropic · Sonnet 4.5" | Provider list |
| Schlüssel | "2 Profile · Privat, Arbeit" | Key profiles |

Tapping the section header opens the provider list. A `danger` note appears here if a provider's last connection test failed, with the reason, because a broken key discovered at the start of a run is a bad discovery.

### Autonomie

| Row | Value | Notes |
|---|---|---|
| Standardrechte | "Auf riskante Schritte fragen" | Sets the default for new projects. Existing projects keep their own. |
| Wiederholungen | "10 Versuche" | The default retry budget for new projects |
| Kostenwarnung | "Aus" | A soft advisory. The row's help text: "Eine Warnung, kein Stopp." |

The hard blocks are stated once, here, as a non-interactive paragraph rather than as four disabled switches:

> **Immer gesperrt.** Löschen, Bezahlen, Veröffentlichen, Hochladen auf den Hauptzweig. Diese Regeln lassen sich in keinem Modus abschalten.

Making them visible as permanently-locked settings is honest. Hiding them would suggest they are merely off.

### Darstellung

| Row | Value |
|---|---|
| Erscheinungsbild | "System", "Hell", "Dunkel" |
| Sprache | "System", "Deutsch", "English" |
| Textgrösse | "System" with a live preview of the new-chat greeting |
| Bewegung | "System" with the current effective state: "Systemeinstellung: reduziert" or "Systemeinstellung: normal" |

The motion row has no on/off. The system setting is the single source of truth; a second toggle produces a state where the user has set the wrong one. The row shows the *effective* value, so somebody who suspects their setting is not being honoured can see whether it is.

### Benachrichtigungen

| Row | Value | Notes |
|---|---|---|
| Lauf fertig | Schalter | On by default |
| Fehler und blockiert | Schalter | On by default |
| GitHub | Schalter | Off until GitHub is connected, with the reason on the row |
| Warte auf Erlaubnis | Schalter | On by default; this is the one that matters when the phone is in a pocket |
| Akku-Schwelle | "Unter 20 % keine GitHub-Abfrage" | A slider |

### Sicherheit

| Row | Value | Screen |
|---|---|---|
| App-Sperre | "Fingerabdruck, nach 5 Min." | Biometric settings |
| Schlüsseltresor | "Aktiv · {n} Schlüssel" | Key management |
| Sichern | "Aus" | Whether the device backup may include projects |
| Bildschirmschutz | "Aus" | `FLAG_SECURE`, so the recents screenshot is blank |
| Sitzungsdauer | "Nie automatisch abmelden" | Not implemented; shown as disabled with the reason, rather than absent |

### Speicher

| Row | Value |
|---|---|
| Belegt | "1,2 GB von 12 GB · Projekte 840 MB · Laufzeit 420 MB" |
| Protokolle | "48 MB" with a "Leeren" action |
| Verlauf aufbewahren | "Alle Läufe" or a retention period |
| Lokale Projektdaten löschen | The one delete, in the danger zone, per `08-project-detail.md` |

Nothing here deletes silently. "Leeren" clears the activity log and says exactly what that means: "Das Protokoll wird geleert. Deine Projekte und Unterhaltungen bleiben."

### Über

| Row | Value |
|---|---|
| Version | "1.0.0 (42)" with the build type and a copy action |
| Laufzeitprofil | "Native" or "Ubuntu (proot)" |
| Claude Code | "2.1.283 · selbst aktualisiert" with a "Jetzt prüfen" action |
| Updates | "Automatisch prüfen" with the last check time |
| Datenschutz | The privacy summary, linking to `11-operations/privacy.md` |
| Mitwirken | The repository link |
| Über diese App | The unofficial notice, in full, as a paragraph |

## The provider editor

A separate screen, reached from the Anbieter section.

| Field | Behaviour |
|---|---|
| Name | Free text. Defaults to the provider's name. |
| Typ | A segmented control: Anthropic, OpenAI Chat, OpenAI Responses, Benutzerdefiniert. Changing it resets the path template and warns. |
| Server-Adresse | Prefilled per type, editable, with validation |
| Pfadvorlage | Only for "Benutzerdefiniert". Advanced; behind the "Erweitert" row. |
| API-Schlüssel | A picker for the key profile, plus "Neues Profil" |
| Modell | A dropdown from discovery, or a text field. A manual list is always available. |
| Modelle suchen | Runs `/v1/models` and offers a picker, or explains why discovery is unavailable |
| Weitere Header | A key-value editor, for gateways |
| Verbindung testen | Runs, reports a specific pass or fail, records the last result on the row |
| Ist Standard | A switch, single-select across providers |

A provider that has never been tested shows a `warning` chip next to its name in the list. A provider whose test failed shows a `danger` chip with the reason, tappable for the detail.

## The biometric settings screen

| Row | Value |
|---|---|
| App-Sperre | On / off, with a note about what it protects: the keys and the project names |
| Methode | Fingerabdruck / Gesicht / Geräte-PIN, whichever the device supports |
| Verzögerung | "Sofort", "Nach 1 Min.", "Nach 5 Min.", "Nach 15 Min." |
| Bei Hintergrundwechsel | On / off. Off means the app locks every time it leaves the foreground. |

If no lock is configured on the device, the switch is disabled and the row says: "Es ist keine Bildschirmsperre eingerichtet." with a link to the system settings. It does not silently fall back to no lock.

## The key management screen

| Row | Behaviour |
|---|---|
| Key profiles | Each shows its name, a hint of the last four characters, the provider, and when it was last used |
| Reveal | Requires a fresh biometric authentication and hides again after 15 s |
| Test a key | Runs a cheap request against the provider |
| Add a profile | Name plus key |
| The key itself | Never displayed in full after the first entry. Saving masks it, permanently. |

There is no export of keys, and no backup of keys. A key that leaves this screen leaves the device.

## Advanced settings

Behind "Erweitert", because a person who does not know what they are changing should not change it.

| Row | Purpose |
|---|---|
| Standard-Modell | The model for new projects |
| Kontextgrenze | A warning threshold, default 70 % |
| Kompaktierung | Manual, or automatic at a threshold |
| Unteragenten | Enabled, and how deeply they may nest, default 2 |
| Vorabprüfung | Whether to run a cheap provider request before a long run |
| DNS-Überschreibung | The opt-in public-resolver fallback, with the full explanation from `11-operations/security-threat-model.md` |
| Worktree-Isolation | Whether each run gets its own git worktree |
| Protokollstufe | Info / Debug, with a warning that Debug logs commands and paths |
| Experimentelle Funktionen | Behind a switch each, each with a one-line explanation and a "might break" note |

Every experimental row states what breaks if it is on. A feature flag without that sentence is a trap.

## States

| State | Behaviour |
|---|---|
| No provider configured | The Anbieter section shows a `warning` row: "Kein Anbieter eingerichtet" linking to setup |
| A key fails after being saved | The key profile shows `danger` with the reason |
| No biometric lock on the device | The row is disabled with the reason and the settings link |
| A runtime profile is missing | The Über section shows it, with a link to setup |
| An update is available | A row at the top of the screen: "Version 1.1.0 verfügbar" with "Ansehen" — never auto-installed |
| The app is on a new version and a setting changed | A `info` row: "In dieser Version wurde {setting} geändert." with the old value |
| Settings changed while a run is active | Changes apply to the next run, and the snackbar says "Gilt ab dem nächsten Lauf." |
| A setting is unavailable on this device | Shown, disabled, with the reason. Never hidden. A setting that is missing cannot be diagnosed. |

**The "in dieser Version geändert" row is a small feature with a large effect.** Silent setting changes are how an app loses a user's trust: they change a preference, upgrade, and behaviour differs for reasons they cannot reconstruct.

## Accessibility

| Requirement | Implementation |
|---|---|
| Sections | Real headings, so TalkBack can jump between them |
| Switch rows | One merged node: "{setting}, {value}, Schalter, ein/aus" |
| Navigation rows | "{setting}: {value}. Öffnen." |
| Disabled rows | "Nicht verfügbar: {reason}" — the reason is announced, not just the disabled state |
| The hard-block paragraph | Announced where it sits in the list. It is text, not decoration, and it is readable. |
| Key profiles | The hint is the last four characters. The key itself is never announced, even after a reveal, so a screen reader cannot read a secret aloud. |
| The "gilt ab dem nächsten Lauf" snackbar | Announced, because it changes the user's expectation about the current run |
| Font scale 1.3 | Sections stack, values wrap below labels rather than truncating |
| Target | 56 dp rows, above the 48 dp minimum, because these are the most-tapped controls in the app |

## Testing

| Test | Type |
|---|---|
| `SettingsMain` | Screenshot — every section, both themes, German and English |
| `SettingsHardBlocks` | UI — the paragraph is present, the rows are non-interactive, and no control toggles them |
| `SettingsSections` | UI — every row navigates to its screen, and the value shown matches the stored value |
| `SettingsProviderEditor` | E2E — add a provider of each type, test it, set it as default |
| `SettingsProviderTestFailure` | Screenshot — a `danger` chip with the specific reason |
| `SettingsKeys` | E2E — add, use, reveal with biometric, and confirm the key never appears in the accessibility tree or a screenshot |
| `SettingsBiometricFallback` | Screenshot with no device lock, showing the disabled row and the reason |
| `SettingsAdvanced` | Screenshot — every experimental row carries a "might break" note |
| `SettingsUpdateAvailable` | Screenshot — a row at the top, never auto-installed |
| `SettingsChangedInVersion` | Screenshot of the informational row |
| `SettingsAppliesNextRun` | E2E — a setting changed during a run does not affect that run and the snackbar says so |
| `SettingsFontScale` | Screenshot at 1.3 |
| `SettingsNoDeadRows` | E2E — taps every row in the hierarchy; every one either navigates or changes a stored value. A row that does nothing fails this test. |
