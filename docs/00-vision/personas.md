# Personas

Two personas. They disagree about almost everything, and the product has to serve both without splitting into two products.

---

## Persona 1 — The owner

**Mira, 34.** Runs a small business. Not technical, and does not want to be. She can follow a setup guide if it has pictures and no surprises. She has a GitHub account because a developer friend put one there for her years ago and it has three private repositories she does not fully understand.

She has an Anthropic API key. She bought $20 of credit in February and has used $4 of it.

**What she wants:** an app. She describes it — "something where my customers can book appointments and I can see the week" — and she wants a working thing at the end, not a repository.

**How she behaves:**

- She reads every confirmation dialog, because she has been burned by installers.
- She does not read error messages; she screenshots them and asks someone.
- She will not configure anything that is not offered as a choice between named options.
- She leaves the phone on a table and comes back in 20 minutes, expecting the app to still be working.
- She does not read diffs. She reads whether the app says it worked, and she trusts that.

**What she must never have to do:** write a command, choose a model, name a branch, or understand what a dependency is.

**What she notices when it is wrong:** it asks her a question she cannot answer; it says "done" and nothing changed; it costs her money she did not expect; she cannot find where something went.

**Success for Mira:** she describes the app she wants, walks away, and comes back to a link she can open on her phone.

---

## Persona 2 — The builder

**Jonas, 29.** Writes code for a living, mostly TypeScript and Go, some Kotlin. Uses Claude Code daily on a laptop. Has opinions about terminal emulators, and they are not gentle ones. Owns a Pixel and a Mac.

He has several API keys, a Cloudflare tunnel, a home server, and a $200 Oracle Cloud Always Free instance that he got for a side project and still pays $0 for.

**What he wants:** the laptop workflow, in his pocket, with the ergonomics intact. He will notice if the terminal drops a keypress, if the streaming stalls, or if the app guesses a model.

**How he behaves:**

- He configures everything once and then never again.
- He reads the log. He has found real bugs by reading logs.
- He tests on his real repository, not a toy one.
- He will run the app at `FULL_AUTO` and walk away for two hours.
- He checks the cost after every run and knows roughly what it should be.
- He will open the diff. He will revert half of it.

**What he must be able to do:** read raw output, choose the model, set the exact verification commands, pin a dependency, change the retry budget, switch key profiles, run a task on his own server, and see the exact process invocation.

**What he notices when it is wrong:** anything that hides a command; a wrong default; a UI that guesses; a feature that cannot be turned off.

**Success for Jonas:** he cannot tell he is on a phone, except that he is.

---

## Where they collide, and what we do

| Collision | Resolution |
|---|---|
| Mira does not read diffs; Jonas always does | The diff is one tap from the summary and is never gated. Mira reads the summary; Jonas reads the diff. Neither is forced. |
| Jonas wants configuration; Mira is frightened by it | Every setting has a sane default and a plain-language name. Configuration is a separate screen, never a dialog inside a task flow. |
| Mira never reads errors; Jonas always does | Errors are always shown, with a plain-language first line and the raw detail collapsed beneath. Mira needs only the first line; Jonas expands the rest. |
| Mira wants one button; Jonas wants per-project control | The one button is the path. The controls are one tap deeper and pre-set sensibly. Neither has to touch the other. |
| Jonas wants `FULL_AUTO`; Mira would be terrified | Autonomy is per project, visible on the project card, and requires a deliberate choice to raise. Raising it to full-auto is a two-step confirm. Lowering it is always one tap, during a run, with no warning. |
| Both want honesty | This is the one place they do not conflict. Everything they do not understand is recorded and visible to both of them. |

## What each persona implies for the design

From Mira: **named choices over configuration, never-ending visibility of state, no expert-only screens that block progress, plain-language error first lines, cost always on screen.**

From Jonas: **a real terminal with no shortcuts, raw output one tap away, exact command visibility, every default visible and changeable, no feature that cannot be disabled, correct behaviour under long runs and interruption.**

Neither implies: a settings screen full of switches nobody can explain. If a setting cannot be explained in one sentence to a non-technical person, it does not go in the main settings; it goes behind "Erweitert".
