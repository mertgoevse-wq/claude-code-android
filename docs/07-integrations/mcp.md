# MCP servers

Optional extra tools for Claude Code, from external programs. Supported, visible, and never required.

## What MCP is

Model Context Protocol: a standard way for Claude Code to discover and call tools provided by an external process. A database, a browser, an issue tracker, an internal API. Claude Code talks to it; the app does not need to understand it.

## Position

**Optional, additive, and never required for the build or for a run.**

| Rule | Enforcement |
|---|---|
| No MCP server is required for anything | The app's own E2E suite runs with zero servers configured. A test asserts it. |
| No MCP server is bundled | The app ships no server and no configuration for one |
| No MCP server is contacted by the app | Only the engine talks to them. The app never proxies one. |
| No MCP server is required for a run to succeed | A run with a broken server continues, with the failure reported |
| No MCP server can be installed without a preview | Same flow as a skill: fetch, parse, show, confirm |

## Configuration

Claude Code reads MCP servers from `.mcp.json` in the project, and from a user-level configuration.

| Location | Who writes it | Scope |
|---|---|---|
| `~/.claude/.mcp.json` | The user, in the app or in the terminal | Every project |
| `{project}/.mcp.json` | The user, or a run that needs one | One project |
| A `--mcp-config` flag | The app, for a single run | One run |

The app has a settings screen for MCP, per project and globally, which reads and writes those files through the same preview-and-confirm flow as everything else that writes into a project.

```json
{
  "mcpServers": {
    "database": {
      "command": "npx",
      "args": ["-y", "@some/db-mcp"],
      "env": { "DB_URL": "…" }
    },
    "remote": {
      "type": "http",
      "url": "https://mcp.example.com",
      "headers": { "Authorization": "Bearer …" }
    }
  }
}
```

## Credentials in an MCP configuration

An MCP server's `env` or `headers` routinely contain a credential. That is a secret, and it lands in a file in the project.

| Rule | Detail |
|---|---|
| A credential is stored as a reference, not a value | `{"$secret": "profile-id"}` in the file; the app resolves it at run time from the Keystore |
| The resolved file is never written | The raw value exists only in the process environment |
| A repository file is still a repository file | `.mcp.json` is committed by default in the Claude Code ecosystem. The app warns when a project's `.mcp.json` contains something that looks like a credential, and offers a reference instead. |
| `.mcp.json` with a credential | The configuration screen shows a `danger` row: "Diese Datei enthält einen Schlüssel im Klartext." with the reference option |
| A detected value | The redaction pass applies to the file's contents on read, and the warning names the pattern |

This is a genuine hazard in the MCP ecosystem, and it is the app's job to notice a user's own project file contains a plaintext token, because nobody notices that themselves.

## Discovery and status

| Aspect | Behaviour |
|---|---|
| Discovery | The engine's system-init event reports the connected servers |
| The status screen | A list: the name, the transport, whether it connected, when |
| A failed server | Shown as failed, with the error. Never hidden, because a missing tool that the agent was supposed to have produces a confusing failure much later. |
| A server that times out | A specific error, and a note that the tools it provides are unavailable |
| The tool list | Shown per server, so somebody can see what a server adds |
| A tool the app does not recognise | Listed with its name and description, and marked as unknown. The app does not try to interpret it. |

## During a run

| Event | Behaviour |
|---|---|
| A server connects | A `SYSTEM` log entry, and a line in the run header: "3 MCP-Server verbunden" |
| A tool from a server is called | A normal tool card, with the server's name shown as a qualifier: "database · query" |
| A server tool produces a large result | The same 2 KB preview cap as any other tool, with the full output in the log |
| A server tool fails | The card shows the error. The run continues. A third-party tool failing is not the run's fault. |
| A server is used for something the hard blocks forbid | `HardBlockPolicy` applies. A tool called `delete_workspace` is refused exactly like a bash `rm`, because the policy inspects intent rather than the tool's name or origin. |
| A server is compromised | The app cannot detect it. It is third-party code with the engine's permissions. Stated in the settings screen, plainly. |

## Health

| Check | When | Reported as |
|---|---|---|
| A server is configured | On screen open | Present |
| The command exists | On a test | "Befehl nicht gefunden: {cmd}" |
| The server starts | On a test, with a 10 s timeout | Connected, or the error |
| The server's tools are listed | After connecting | The count, and the names |
| It was connected during a run | From the run's system-init event | In the run's record |
| It was not connected | By the difference | "3 Server konfiguriert, 1 nicht verbunden" |

The distinction between "configured" and "connected" is always visible. A server that silently fails to start and a server that is not installed look identical from the agent's side, and the resulting error message points at the agent rather than at the configuration.

## Adding and removing

| Action | Flow |
|---|---|
| Add | Paste a configuration, or a URL, or pick a template. Validated as JSON, the shape checked, the command's existence verified, a connection test, then written through the preview-and-confirm flow. |
| Edit | The same. The diff against the current file is shown. |
| Remove | A confirm, showing the file it will edit and the exact lines it will remove. **The app never deletes a file**, so removing a server means removing its entry, not its configuration file. If the file becomes empty, it is left in place with an empty object, and the app says so. |
| Disable | Rather than remove. A switch. The entry stays, and the app does not pass the config to the engine. |
| Per run | An ad-hoc configuration for one run, never written to a file, with a preview and a confirm |

## Repository trust

A project's `.mcp.json` is repository content, and repository content is untrusted.

| Rule | Detail |
|---|---|
| Read before running | The file is parsed and shown before the first run that would use it |
| Approval | A project's `.mcp.json` requires a one-time approval, per project, with the full contents displayed |
| Never silently | A project with an unapproved `.mcp.json` runs with it disabled, and says why |
| Changes are re-approved | A changed file invalidates the approval and asks again |
| The approval is recorded | In the log, with the file's digest |

This is the same posture the CLI takes, and the same one a reasonable person would want from a tool that executes code from a repository.

## The screen

```
MCP-Server · claude-code-android              [+ Server]
──────────────────────────────────────────────────────
✓ database          stdio   verbunden · 12 Werkzeuge
✓ issues            http    verbunden · 6 Werkzeuge
✕ browser           stdio   nicht verbunden
                    Befehl nicht gefunden: npx
                    [Konfiguration ansehen]  [Erneut testen]

⚠ .mcp.json enthält einen Schlüssel im Klartext
  Zeile 14 · Authorization
  [ In eine Geheimreferenz umwandeln ]

Nicht verbunden: 1 von 3
```

## What is not built

| Not | Reason |
|---|---|
| Bundled servers | The app ships none. A database server or a browser server is a choice about what the user has, not something the app should assume. |
| A marketplace of servers | Same reasoning as the skill marketplace, plus a third-party code supply chain we do not want to curate. |
| Proxied connections | The engine talks to servers directly. A proxy would put the app in the path of every tool call, for no benefit. |
| Server health monitoring over time | A test on demand is enough. A background poll of somebody else's server is not the app's job. |
| Editing a server's code | The app configures servers; it does not develop them. |

## Testing

| Test | Type |
|---|---|
| `ZeroServersWorks` | E2E — the entire E2E suite passes with no MCP server configured. This is the test that matters. |
| `ConfigValidation` | Unit — the JSON shape, the command's existence, the URL's scheme |
| `WriteThroughPreview` | E2E — a configuration is written only after a preview and a confirm, and the diff is shown |
| `RemoveLeavesFile` | E2E — removing the last server leaves the file in place with an empty object, and says so |
| `RemoveNeverDeletes` | E2E — no file is deleted by any MCP operation |
| `RepositoryApproval` | E2E — a project with an unapproved `.mcp.json` runs with it disabled and says why |
| `ApprovalInvalidated` | E2E — a changed file re-asks |
| `ApprovalLogged` | Integration — the approval, with the file's digest, is in the log |
| `SecretReference` | E2E — a `$secret` reference resolves from the Keystore, and the raw value never reaches the file |
| `PlaintextWarning` | E2E — a `.mcp.json` with a token produces the `danger` row naming the line, with a conversion offer |
| `StatusDistinguishesConfigured` | UI — "3 konfiguriert, 1 nicht verbunden" is shown, and the failing one has its error |
| `ToolFailureDoesNotEndRun` | E2E — a failing server tool is recorded and the run continues |
| `HardBlockApplies` | E2E — a destructive tool from a server is refused exactly like a local one, at every level |
| `ServerNamedInToolCard` | UI — a tool card from a server shows the server's name |
| `AdHocConfigNotPersisted` | E2E — a per-run configuration is not written to any file |
| `LargeToolResult` | E2E — a server returning 1 MB is previewed at 2 KB with the full output in the log |
| `NoBundledServers` | Static — the repository contains no MCP server implementation and no default configuration |
| `NoProxy` | Static — no code in the app connects to a server on the engine's behalf |
