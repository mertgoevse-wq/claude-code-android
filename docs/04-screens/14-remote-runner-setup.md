# Screen 14 — Remote runner setup

Adding a machine that does the work when the phone should not.

## Purpose

Let somebody with a home server, or nobody with one at all, set up a free machine to run long jobs — with every free option, every limit, and every consequence stated before anything is set up.

## Structure

### Top bar

Title "Nachschubserver", a `+` button.

### The explanation card, at the top

Not a dismissible tip. A permanent card, because the alternative is a user discovering halfway through a two-hour build that the phone is the bottleneck.

> **Wofür das gut ist.** Lange Aufgaben und Builds laufen auf einem anderen Rechner. Dein Projekt wird dorthin kopiert, das Ergebnis kommt zurück, und du siehst hier alles wie sonst. Ohne Nachschubserver funktioniert die App vollständig — nur langsamer.

### The list of runners

A card per configured runner.

| Row | Content |
|---|---|
| Name | User-set: "Mein Server", "Oracle Free" |
| Kind | "SSH", "Oracle Cloud", "GitHub-Aktionen" |
| Host | The address, `monoSmall`. For GitHub Actions, the repository. |
| Status | `ONLINE` / `OFFLINE` / `UNREACHABLE` / `QUOTA_EXCEEDED` / `UNKNOWN`, with a colour, a glyph, and a word |
| Capabilities | "4 Kerne · 24 GB · 180 GB frei · Node 22 · Claude Code 2.1.283" — probed, never assumed |
| Last probe | "vor 4 Min." |
| Assigned to | "Alle Projekte" or project names |
| Free tier | "Kostenlos · Kontingent geprüft am 12. Sep. 2026" |

| Interaction | Behaviour |
|---|---|
| Tap | The runner detail sheet |
| Re-probe | A refresh action; the probe is the only source of the capabilities |
| Long press | Bearbeiten, Probing wiederholen, Aus Projekten entfernen. Removing a runner never removes anything on the machine. |

## Adding a runner

Tapping `+` opens a sheet with three routes and a comparison table.

### The comparison, before any setup

| | Eigenes Gerät | Zuhause (SSH) | Oracle Free | GitHub-Aktionen |
|---|---|---|---|---|
| Kosten | 0 | 0 | 0 | 0 im Kontingent |
| Bleibt dauerhaft an | Ja | Nur wenn der Rechner läuft | Ja | Nein |
| Volles Linux | Teilweise | Ja | Ja | Ja |
| Für lange Läufe | Eingeschränkt | Sehr gut | Sehr gut | Nicht geeignet |
| Einrichtung | — | Mittel | Mittel | Leicht |
| Privatsphäre | Alles bleibt lokal | Dein Rechner | Bei einem Anbieter | Bei GitHub |
| Konto nötig | Nein | Nein | Ja, mit Kartendatenprüfung | Ja, GitHub |
| Hält unbegrenzt | Ja | Ja | Nicht garantiert | Nein |

Two lines of this table matter and are highlighted:

**Konto nötig: Ja, mit Kartendatenprüfung.** A free cloud tier that verifies a card and charges nothing is a normal practice, and somebody who has been burned by it deserves to know before they are told.

**Hält unbegrenzt: Nicht garantiert.** Free tiers are withdrawn. The app does not promise permanence and the runner screen shows the date the terms were last checked.

### Route A — SSH (a home PC or server)

A four-step flow.

| Step | Content |
|---|---|
| 1 — Adresse | Host or IP, port, user. A hint that a key is needed, not a password. |
| 2 — Schlüssel | The app generates an **ed25519** key pair on the device. It shows the public key with a copy action and the one-line command to run on the target. The private key is stored in the Keystore and never displayed after creation. |
| 3 — Prüfen | The app runs a probe: connect, authenticate, `uname`, `free`, `df`, toolchain versions, and whether Claude Code is installed. Each result is a row. |
| 4 — Einrichten | If Claude Code is missing, the app offers to install it, showing the exact commands first. Not silently. |

**The password is never collected.** SSH key authentication only. A password typed into a phone is a password in a screenshot, in a keyboard's history, and in someone's memory; a key that never leaves the app's storage is strictly better. The screen says why.

The bootstrap command shown to the user is one line, copyable:

```
mkdir -p ~/.ssh && echo "ssh-ed25519 AAAA… cca@device" >> ~/.ssh/authorized_keys && chmod 600 ~/.ssh/authorized_keys
```

The user runs it on the machine they already have access to. No credentials of theirs are transmitted, and the app never asks for a password it could store badly.

| State | Behaviour |
|---|---|
| The host is unreachable | The specific failure: DNS, refused, timeout, wrong port. Not "connection failed". |
| The key is rejected | "Der Schlüssel wurde abgelehnt." with the command to re-run, and a check for a typo in the copy |
| The host is a Windows machine | Detected, with a note that WSL is required, and the WSL-specific bootstrap |
| The host has no Claude Code | Offered, with the commands shown and a confirm |
| Everything is fine | The runner is added and immediately assigned according to the chosen policy |

### Route B — Oracle Cloud Always Free

A guided flow with links, because the account creation cannot be automated and pretending otherwise would waste the user's time.

| Step | Content |
|---|---|
| 1 — Konto | "Erstelle ein Konto bei Oracle Cloud." with a button opening the registration. A note: "Zur Prüfung wird eine Kreditkarte verlangt. Es wird nichts abgebucht." |
| 2 — Instanz | Instructions with screenshots in the docs, not in the app. The exact values: an Ampere A1 shape, 2 OCPU, 12 GB, a 100 GB boot volume, Ubuntu. |
| 3 — Zugang | The public IP, and the SSH private key download. **The app never asks for the Oracle private key.** It generates its own key and the user adds the public key to the instance during creation or afterwards. |
| 4 — Prüfen | The same probe as route A |
| 5 — Kontingent | A live check of the account's remaining free quota, with the date. If the quota is exhausted: "Das kostenlose Kontingent ist aufgebraucht. Ein Upgrade ist in dieser App nicht möglich und auch nicht vorgesehen." |

| State | Behaviour |
|---|---|
| The region has no free capacity | "In dieser Region ist kein kostenloses Kontingent verfügbar. Wähle eine andere Region." This is common and the app treats it as a normal outcome with a next step, not an error. |
| The account is not verified | "Das Konto ist noch nicht bestätigt. Die Bestätigung kann 24 Stunden dauern." with a probe button that can be tapped again later. |
| The quota is exhausted | The specific remaining amount and the reset date, and the other routes |
| The instance is idle-reclaimed | "Die Instanz wurde vom Anbieter zurückgenommen. Offene Always-Free-Instanzen können nach langer Inaktivität entfernt werden." — a real behaviour of the free tier, stated plainly rather than as a bug |

### Route C — GitHub Actions

| Step | Content |
|---|---|
| 1 — Zweck | "Nur für kurze Aufgaben: Builds, Tests, einzelne Prüfläufe. Keine stundenlangen Sitzungen." |
| 2 — Repository | Picker from the connected account, private only |
| 3 — Einrichtung | A one-time workflow file is proposed, shown in full before writing, with a "Schreiben" confirm |
| 4 — Testen | A trivial workflow run to prove the wiring, with a link to the Actions page |

The proposed workflow is shown in full because the app is about to write a file into the user's repository, and that is a privileged action by the standard of the skill installer. The commit it makes is on a branch, never on the default branch, and it is announced.

| State | Behaviour |
|---|---|
| GitHub not connected | The route is present but routes to the connect screen |
| No private repositories | "Keine privaten Repositories sichtbar." |
| Actions disabled on the repository | "Aktionen sind für dieses Repository deaktiviert." with a link to the repository settings |
| The quota is nearly gone | "Nur noch {n} Minuten im Monatskontingent." with the date it resets |
| A workflow run fails | The job log, in the app, and a link to the Actions page |

## Offload policy

After adding a runner, the user chooses when to use it. This lives on the project and here, globally.

| Option | Behaviour |
|---|---|
| Nie | Always on the phone |
| Wenn schwer | Offload when the project needs a toolchain the phone lacks, or the task is estimated to exceed a threshold. The app says which condition triggered it. |
| Immer | Every run on the runner |

| Condition | How it is decided |
|---|---|
| A toolchain is missing | The capability probe on the phone says Gradle is absent and the project needs it |
| Estimated duration | A threshold, default 20 minutes, based on the project's historical run times |
| Battery | Below 15 % and charging is not detected |
| Thermal | The device is hot and throttling |
| The user chose it | Always honoured, regardless of the policy |

**The app always says which condition triggered an offload**, in the run header and the log. A job that silently moved to someone else's server would be a surprise, and this app does not surprise.

## Privacy before the first offload

One confirm, once per runner, before the first use:

> Dein Code wird auf **{host}** kopiert. Dort liegt er auf einem fremden Rechner. Nach dem Lauf wird die Arbeitskopie dort entfernt — gelöscht wird nur die Kopie, nie dein Repository.

| Property | Rule |
|---|---|
| Shown once per runner | Not per run. Repeating it would be nagging; once is enough to be informed. |
| The host is named | Never a generic "der Server" |
| The copy is removed after the run | And the removal is recorded in the log |
| The repository is untouched | The runner's copy is a working copy. Pushing happens from the runner, and only to a feature branch. |
| Refusing is permanent per runner | There is no "always allow" that hides this. |

## States

| State | Behaviour |
|---|---|
| No runners | `empty-terminal.svg`, "Kein Nachschubserver", and the three routes. The note: "Die App funktioniert auch ohne." |
| A runner is offline | The card says so, with the last successful probe time. Runs assigned to it are not started. |
| A runner's quota is exhausted | The card says so, with the date. Assigning a new run to it is refused with a suggestion. |
| A run is on a runner | The chat and project screens show the host in the run header: "Oracle Free · 4 Kerne" |
| A runner is removed while a run is on it | The run is not killed. The app offers: let it finish and orphan the result, or interrupt it. Removing a runner never kills work. |
| A probe fails repeatedly | Three failures mark it `UNREACHABLE` and stop the automatic probing. The card says when it was last reachable. |
| Terms changed | The card's date is stale; the probe flags it: "Bedingungen zuletzt geprüft am {date} · {n} Tage alt" |

## Accessibility

| Requirement | Implementation |
|---|---|
| The comparison table | A real table, with column headers announced. A screen reader user comparing free tiers needs the structure. |
| Probe results | A list; each row announces its value and pass or fail |
| The SSH key | The public key is selectable text with a copy action. The private key is never in the tree, even after a reveal. |
| Runner status | "Online, letzte Prüfung vor 4 Minuten", not a coloured dot |
| The privacy confirm | Focus moves in, the host is read out, the three options follow |
| The quota | Announced with the date, because "the quota is gone" is useless without a date |
| Font scale 1.3 | The table scrolls horizontally, which is the one reflow exemption here, and it is announced as scrollable |
| Target | 48 dp on every control, including the small links in the steps |

## Testing

| Test | Type |
|---|---|
| `RunnerListEmpty` | Screenshot with the three routes and the "works without" note |
| `RunnerListMixed` | Screenshot with an online, an offline, and a quota-exhausted runner |
| `RunnerComparison` | Screenshot, and a UI test asserting the card-verification and no-permanence rows exist |
| `RunnerSshKeyGeneration` | E2E — a key is generated, the public key is copyable, and the private key is never in the accessibility tree or a screenshot |
| `RunnerSshProbe` | E2E — against a local SSH server, the probe returns real capabilities and the app displays them |
| `RunnerSshUnreachable` | Screenshot with the specific failure, not a generic one |
| `RunnerOracleFlow` | Screenshot of each step, with the card-verification note on step 1 and the no-upgrade statement on step 5 |
| `RunnerOracleNoCapacity` | Screenshot — a normal outcome with a next step, not an error |
| `RunnerActionsWorkflow` | E2E — the workflow file is shown in full, confirmed, written to a branch, and a trivial run succeeds |
| `RunnerOffloadTrigger` | E2E — a project missing Gradle is offloaded, and the reason is shown in the run header |
| `RunnerPrivacyOnce` | E2E — the confirm appears on the first offload and not on the second, and the host is named in both |
| `RunnerCopyRemoved` | E2E — the working copy is removed after the run and the removal is logged |
| `RunnerRemoveDuringRun` | E2E — the run is not killed, and the two options work |
| `RunnerNoUpgradePath` | UI — asserts no card field and no upgrade action exists anywhere in this flow |
| `RunnerFontScale` | Screenshot at 1.3 |
