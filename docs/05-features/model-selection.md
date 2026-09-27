# Model selection

Which model runs the work, what it costs, and how to pick without becoming an expert in model names.

## The principle

A person who has never heard of a frontier model should be able to leave the default alone and never think about it. A person who has should be able to reach a specific model in two taps. Both must be possible at the same time, and the default must be a good choice rather than a safe one.

## The default

| Property | Value |
|---|---|
| What | The current recommended model for the configured provider |
| Where | `AppSetting.defaultModelId`, set when a provider is configured, refreshed when the provider is tested |
| Where it applies | New projects, and any run that does not pick otherwise |
| Visible | In the composer's action row, always, as the current model |

The default is a specific model id, not "the latest". "The latest" is a moving target that changes under a running project, and a cost that changes silently is a cost nobody can budget.

## Where a model is chosen

| Place | Scope |
|---|---|
| The composer's `⊕Modell` button | This run |
| The project detail's Modell row | The project, as a default |
| Settings, Standard-Modell | New projects |
| Onboarding, step 3 | The provider's model, which becomes the default |

Two levels of override, in increasing order of specificity: the global default, the project default, the run's choice. A run's choice wins. The others are shown so it is always clear which is in effect, and a sheet that changes the run's model says "Gilt nur für diesen Lauf."

## The model sheet

A grouped list, one group per configured provider.

```
Anthropic
● claude-sonnet-4-5        Standard        ~$3 / $15 je Mio. Tokens
  claude-opus-4-1          Leistung        ~$15 / $75
  claude-haiku-4-5         Schnell         ~$1 / $5

OpenAI-kompatibel
  gpt-4o                    Standard        ~$2,50 / $10
  gpt-4o-mini               Schnell         ~$0,15 / $0,60

▸ Modell eingeben…          · eigene ID eintragen
```

| Row | Content |
|---|---|
| Radio | The current one is marked, with a `success` glyph, not only a colour |
| Name | The model id, in `monoSmall` |
| Tag | Standard / Leistung / Schnell, when the provider's metadata says so. Never invented. |
| Cost | An estimate, labelled as one, in USD per million input and output tokens |
| A model the app has not seen | Listed normally. The app does not filter to a known list, because a self-hosted model is not on any list. |
| A model whose id the user types | Accepted, validated by a connection test, and remembered as a manual entry |

## Prices

| Source | Priority |
|---|---|
| The user typed them in the provider's settings | Highest. A self-hosted model has no published price, and a private endpoint may have a different one. |
| A bundled table | For the known models, dated |
| A fetched price list | If the provider exposes one |
| Nothing | Shown as "Preis unbekannt", and the cost meter falls back to token counts with no dollar figure |

**No price is ever invented.** If the app does not know a price, the cost meter shows tokens and says "Preis für dieses Modell nicht hinterlegt". A plausible-looking number for a model whose price changed last week is worse than no number.

The bundled table carries a date. When it is more than 90 days old, the Settings row says so and offers to refresh it.

## What the app does not do

| Not | Reason |
|---|---|
| Recommend a model based on the task | The app does not know the task well enough, and a wrong recommendation costs the user money |
| Switch models mid-run | A run is bound to a model. Switching changes the provenance of the answer, and the cost accounting gets murky |
| Hide a model because it is unknown | Self-hosted models are the point of the custom-provider support |
| Fall back to a cheaper model when a run is long | Silent. A user who wants a cheaper model picks it |
| Track a per-provider quota for cost reasons | The provider enforces it. The app shows the rate-limit headers it receives. |

## Capability differences

A model can differ in ways that matter, and the app surfaces only what it can verify.

| Capability | How it is known | What the app does |
|---|---|---|
| Vision | From the bundled table, or the user marking a custom model as vision-capable | The attach button is enabled for images only when the model supports them, and says so if it is disabled |
| Tool use | Assumed for every model, because a coding agent without tools is useless | — |
| Context length | From the table, or the user | The context bar's scale uses it, so "70 % full" means something real |
| Extended thinking | From the table | The thinking display is enabled only for models that stream it |
| Prompt caching | From the table | The cost sheet shows cache read and creation separately, and a "0" is shown as "—" when a gateway omits the field |

**A capability that is unknown is not assumed.** If the app does not know whether a model supports vision, the attach button still works, and if the provider rejects the image the run reports the provider's error. Refusing a capability the app is merely unsure about would break self-hosted models, which is the opposite of the intent.

## Unknown and self-hosted models

| Aspect | Behaviour |
|---|---|
| Adding one | Type the id. It is accepted and validated with a cheap request. |
| Metadata | Optional. The user can mark a model as vision-capable, extended-thinking, with a context length, and a price. |
| Where the metadata lives | With the provider, in the app, never on a remote server |
| Discovery | `/v1/models` when the endpoint supports it. A server that does not is not a broken server. |
| A model list that is enormous | Shown with a filter and a search, not a 400-item list |
| A model that disappears | The run reports the provider's error. It does not silently switch to another model. |

## The cost display

Per model, the cost sheet shows the rate in effect and where it came from.

```
Kosten · Lauf 01HQ…
────────────────────────────────────────
Modell          claude-sonnet-4-5
Anbieter        Anthropic
Preis           3,00 $ / 1 Mio. Eingabe
                15,00 $ / 1 Mio. Ausgabe
                Hinterlegt am 12. Sep. 2026
────────────────────────────────────────
Eingabe         142.318 Tokens      0,43 $
Cache gelesen   1.204.882 Tokens    0,36 $
Ausgabe          8.104 Tokens      0,12 $
────────────────────────────────────────
Gesamt                            0,91 $
Geschätzt bis der Anbieter die Nutzung meldet: 0,88 $
```

| Rule | Detail |
|---|---|
| The price's provenance is shown | "Hinterlegt am {date}" or "Von dir angegeben" |
| An estimate is labelled | With `~` and, in the sheet, with the line above |
| Cache savings are shown separately | They are often the largest part of a coding bill, and a user who does not see them does not know to look for them |
| An unknown price | "Preis für {model} nicht hinterlegt" and the total is omitted rather than estimated as zero |
| Never | A total that silently changes. The estimate-to-actual transition is animated and visible. |

## Model changes and history

| Concern | Behaviour |
|---|---|
| A run's model | Recorded at the start, in the run, the log, the commit message, and the PR body |
| A project's model change | Logged with before and after |
| A global default change | Logged, and the About screen notes which projects were affected |
| Old records | Never re-costed. The rate used at the time is stored per record. |
| A price table update | Affects estimates from the update forward. Records already made keep their rate. |

## Testing

| Test | Type |
|---|---|
| `ModelDefault` | Integration — configuring a provider sets a specific model id, never "latest" |
| `ModelSheetGrouping` | Screenshot with two providers, a current model marked with a glyph, and a manual entry row |
| `ModelPriceProvenance` | UI — a bundled price shows its date, a user price shows "von dir angegeben" |
| `ModelUnknownPrice` | UI — no invented total; "nicht hinterlegt" is shown and the dollar figure is absent |
| `ModelPriceTableAge` | UI — a table older than 90 days triggers the notice |
| `ModelNeverInvented` | Unit — a model absent from the table yields no price, and a test asserts the absence rather than a zero |
| `ModelManualEntry` | E2E — a self-hosted model id is accepted after a validation request |
| `ModelMetadataOptional` | E2E — a model with no metadata works; a run against it reaches the provider |
| `ModelNoMidRunSwitch` | E2E — the sheet during a run says "gilt nur für diesen Lauf" and a run's model never changes |
| `ModelRecordedEverywhere` | E2E — the model appears in the run, the log, the commit message, and the PR body |
| `ModelNoReCost` | Unit — a price table update does not change an existing record's cost |
| `ModelVisionGate` | UI — for a model marked vision-capable the attach button is enabled; for one known not to be, it is disabled with a reason; for an unknown one, it is enabled |
| `ModelDiscoveryAbsent` | E2E — a server without `/v1/models` falls back to manual entry without an error state |
| `ModelHugeList` | Performance — 400 models filter without a frame drop |
