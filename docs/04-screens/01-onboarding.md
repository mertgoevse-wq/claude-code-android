# Screen 01 — Onboarding

The first thing anyone sees. Its job is to be honest, be short, and end with a working app.

## Purpose

Take a first-time user from nothing to a working setup without asking them to understand anything about Linux, keys, or runtimes — while telling them exactly what is about to happen to their phone.

**Target: under four minutes, no scrolling required to understand, no dead ends.**

## Structure

Four steps in a horizontal pager. The step indicator is a thin accent line, four segments, the current one filled. No "1 of 4" text; the segments are enough.

### Step 1 — What this is

The animated mark at 72 dp, centred, idle.

| Element | Content |
|---|---|
| Greeting (serif, `displayLarge`) | "Bauen, ohne am Rechner zu sitzen." |
| Body (`bodyLarge`, max 5 lines) | What the app does, in three sentences |
| Unofficial notice (`labelSmall`, `textTertiary`) | One line, not dismissible |
| Action | Primary: "Weiter". No skip — the first step is information, and information is not skipped |

Copy, German:

> Du hast den echten Claude Code auf deinem Handy. Du schreibst, was du brauchst, und er baut es, prüft es und legt es in dein Repository — auch dann noch, wenn du das Handy weglegst.

English:

> You have the real Claude Code on your phone. Write what you need, and it builds it, checks it, and puts it in your repository — even after you put the phone down.

**The "real" is doing work in that sentence.** The most common doubt about an app like this is whether it is a simulation. Answering it in the first three words is worth more than a paragraph later.

### Step 2 — What happens on your phone

A vertical list, not a marketing list. Each row: a glyph, a title, one sentence with a real number.

| Row | German | English |
|---|---|---|
| Download | "Laufzeitumgebung herunterladen — etwa 400 MB" | "Download the runtime — about 400 MB" |
| Install | "Ein Linux-Programm wird eingerichtet, das Claude Code ausführt" | "Set up a Linux program that runs Claude Code" |
| Network | "Verbindungen zu deinem KI-Anbieter und zu GitHub" | "Connections to your AI provider and to GitHub" |
| Key | "Dein Schlüssel bleibt verschlüsselt auf dem Gerät" | "Your key stays encrypted on the device" |
| Data | "Deine Projekte verlassen das Handy nur, wenn du es verlangst" | "Your projects leave the phone only when you ask" |

| Element | Value |
|---|---|
| Title | "Was auf deinem Handy passiert" |
| Storage callout | "Du brauchst etwa 500 MB frei. Vorhandene Daten werden nicht gelöscht." |
| Action | Primary: "Einrichtung starten" |
| Secondary | "Später" — archives the app to the runtime setup screen, never skips the setup itself |

**"Vorhandene Daten werden nicht gelöscht" is not reassurance, it is a fact from the hard-block list.** It is here because a person who installs a 400 MB program on a phone is entitled to know the app will not make room by removing things.

### Step 3 — Choose a provider

Two paths, side by side, equal visual weight.

| Card | German |
|---|---|
| A | "Anthropic" / "Dein eigener Schlüssel von console.anthropic.com" / "Die beste Wahl für Claude-Modelle" |
| B | "OpenAI-kompatibel" / "OpenAI, OpenRouter, Groq, Together, Ollama, LM Studio" / "Jede API, die dieses Format spricht" |
| C | "Eigener Anbieter" / "Beliebige Adresse, beliebiges Modell" / "Für alles andere" |

Tapping a card opens the provider form inline, not a new screen, so the user can see their choice in context.

| Element | Field | German label |
|---|---|---|
| 1 | API-Schlüssel | "API-Schlüssel" — helper: "Wird verschlüsselt auf diesem Gerät gespeichert" |
| 2 | Adresse | "Server-Adresse" — prefilled per provider, editable, helper: "Nur ändern, wenn du einen eigenen Server nutzt" |
| 3 | Modell | "Modell" — a dropdown from `/v1/models` when available, otherwise a text field |
| 4 | — | A "Verbindung testen" button that runs automatically once 1–3 are filled |

| State | Behaviour |
|---|---|
| Empty | "Noch kein Anbieter eingerichtet" as a note under the cards, not a modal |
| Testing | The button shows a 16 dp indicator, the label stays, the button does not resize |
| Passed | A `success` badge "Verbindung ok · {model}" |
| Failed | A `danger` line with the specific reason from `error-taxonomy.md` and a fix |
| Passed | The step advances automatically after 1.2 s. Nobody wants to tap Next after a green check. |

| Action | Value |
|---|---|
| Primary | "Weiter" |
| Secondary | "Später" |
| Link | "Schlüssel woher?" opens a sheet with a short, link-free explanation |

### Step 4 — GitHub, and the last choice

| Card A | GitHub — "Einmal anmelden, fertig. Wir fragen nur nach Zugriff auf deine privaten Repositories." Primary: "Mit GitHub anmelden". |
| Card B | GitHub-Token — "Du erstellst selbst einen Schlüssel. Nützlich, wenn du keinen Zugriff auf unsere App-Registrierung hast." |
| Card C | Ohne GitHub — "Du kannst lokale Ordner verwenden. Alles ausser GitHub-Funktionen funktioniert sofort." |

"Ohne GitHub" is a full-width card below the two GitHub options, with equal visual weight, because for the persona who has no GitHub account this is a supported path, not a fallback.

| Element | Value |
|---|---|
| Final | The app-lock toggle: "App mit Fingerabdruck sperren" — on by default, with a note that the key is stored in the system key store |
| Action | Primary: "Fertig" |

"**Fertig**" leads to the runtime setup screen, which is where the actual download happens. The word "Fertig" on the setup screen's predecessor is a promise that the app is ready, and it would be a lie if the runtime were not installed. Renamed: primary is "**Weiter zur Einrichtung**" on step 4. The user completes setup, sees it work, and then onboarding is finished.

## States

| State | Behaviour |
|---|---|
| First run | All four steps |
| Interrupted at step 3 | Returns to step 3 with the entered key still in the Keystore, never in a saved field |
| Runtime setup incomplete | Onboarding is not marked complete. The app shows a persistent, dismissible banner on the chat screen: "Einrichtung nicht abgeschlossen" with a button |
| A step fails | The step shows the error inline and stays. Onboarding never skips forward on a failure |
| Back from the last step | Returns to step 3, not to the app. The pager is not navigable by swipe past the end |
| Re-run | Settings has "Einrichtung wiederholen", which restarts from step 2 and preserves every setting |

## Behaviour notes

- The app is fully usable at any point in onboarding. A user who dismisses onboarding is not locked out; the runtime screen can be reached from Settings and from the banner.
- Nothing on this screen makes a network call except the connection test, and only after the user has entered a key.
- The key is written to the Keystore as soon as it is entered and validated, not at the end of onboarding. If onboarding is abandoned, the key is still there and still encrypted.
- A biometric prompt at the end of step 4, to confirm the lock works. A lock the user has never seen succeed is a lock they will disable when it annoys them.

## Accessibility

| Requirement | Implementation |
|---|---|
| Step announcement | Each step is a heading; TalkBack announces the step title on entry |
| The 4 % ambient wash | A decorative element, `clearAndSetSemantics {}` |
| The mark | "Claude Code. Setup-Schritt 1 von 4" |
| Back gesture | Confirms nothing; the pager is not a place where data can be lost |
| Font scale 1.3 | The step content scrolls; the action row stays pinned at the bottom |
| Minimum target | 48 dp on all three provider cards and both actions |

## Testing

| Test | Type |
|---|---|
| `OnboardingHappyPath` | E2E — completes all four steps, lands on a working chat |
| `OnboardingSkipGitHub` | E2E — completes without GitHub, local project works |
| `OnboardingProviderFailure` | UI — a failed connection test shows the specific reason and does not advance |
| `OnboardingInterrupted` | E2E — killed at each step, resumed, no state lost, no key lost |
| `OnboardingFontScale` | Screenshot at 1.3 |
| `OnboardingBothThemes` | Screenshot |
| `OnboardingTinyWidth` | Screenshot at 320 dp |
| `OnboardingNoNetworkBeforeTest` | Integration — asserts zero network calls until the user taps the test |
