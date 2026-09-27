# Test data safety

The tests must never contain a real key, touch a real repository, or see a real person's data. This document is the rule set and the enforcement.

## The three prohibitions

| Prohibition | Why | Enforcement |
|---|---|---|
| No real credentials in the repository, in any form, ever | A leaked key is a bill someone else pays | Secret scan in CI, pattern check on fixtures, pre-commit hook |
| No real network access from any test | A test that reaches the internet is slow, flaky, and can cost money | `MockWebServer`/`MockEngine` only; a network-blocked emulator image for instrumented tests |
| No real user data on a test device | Privacy, and a legal obligation we cannot audit from outside | Per-test data directory, teardown assertion, no shared device state |

## Credentials

**Where a test key comes from.** Nowhere. Tests use a `FakeSecretStore` that returns a fixed placeholder:

```kotlin
object FakeSecretStore : SecretStore {
    override suspend fun get(profileId: String): String = "sk-ant-test-0000000000000000"
}
```

**The manual-test escape hatch.** Occasionally a developer wants to run the app against a real provider on a real device. That is a manual build with `-PmanualKey`, which reads the key from the environment and never writes it anywhere the app will persist it outside the Keystore. The build prints a warning while it is active, and the release manifest excludes it.

**What is never done.** No key in a test constant "just for now". No key in a fixture file. No key in a CI secret referenced by a test — CI secrets are for the release pipeline, and a test that can read a CI secret is one commit away from printing it.

**The pattern check.** CI scans the whole repository, including history, for:

| Pattern | Regex sketch |
|---|---|
| Anthropic key | `sk-ant-[A-Za-z0-9_-]{20,}` |
| OpenAI key | `sk-(proj-)?[A-Za-z0-9]{32,}` |
| GitHub classic PAT | `ghp_[A-Za-z0-9]{36}` |
| GitHub fine-grained PAT | `github_pat_[A-Za-z0-9_]{50,}` |
| Google API key | `AIza[0-9A-Za-z_-]{35}` |
| AWS access key | `AKIA[0-9A-Z]{16}` |
| Private key block | `-----BEGIN [A-Z ]*PRIVATE KEY-----` |
| Slack token | `xox[baprs]-[A-Za-z0-9-]{10,}` |
| Stripe secret | `sk_live_[A-Za-z0-9]{20,}` |
| Google OAuth client secret | `GOCSPX-[A-Za-z0-9_-]{20,}` |

A match anywhere except `docs/`, which quotes the patterns themselves, fails the build. The scanner is `gitleaks` with a configuration in the repo, so the rules are reviewable.

**Placeholders must be obviously fake.** A fixture key is `sk-ant-test-` followed by zeros. It is allowed to *look* like a key so the redaction logic can be tested, and it can never be confused with one because the prefix and length differ from a real format.

## Repositories

**Tests never clone from the internet.** Every repository a test touches is one of the fixtures in `fixtures-and-test-data.md`, copied into the test's own temporary directory.

**Tests never touch the developer's own repositories.** A test that reads `~/dev/my-project` passes on one machine and fails on every other, and on the one machine it passes on, it may well be modifying real work. Tests run in a temp directory created per test and deleted after it.

**Tests never push.** Even to a fixture remote. The "push" path is exercised against a local bare repository created in the temp directory, and the assertions confirm that nothing reached a real remote — the hard block is tested in a way that proves the *absence* of a side effect.

## User data

| Rule | Detail |
|---|---|
| No personal data in fixtures | No real names, no real email addresses, no real file paths, no real issue text. `example.com`, `test@example.invalid`, `/tmp/project`. |
| Path redaction in screenshots | Golden images use a fixture project, so no personal path ever appears. Where a path is rendered, the app shows a path relative to the project root, not an absolute one. |
| No real data in crash reports from tests | Test builds report to a local sink, never to a hosted one. |
| Log output is asserted, not trusted | A test asserting on log content also asserts that no `sk-`-prefixed string appears in the output. |

## Network isolation

| Layer | Mechanism |
|---|---|
| Unit tests | The HTTP client is constructed by a test factory; the production factory is never reachable from `src/test/`. A unit test that instantiates the real client fails to compile-by-convention (the constructor is `internal` and the test source set is denied by a lint rule). |
| Instrumented tests | The test manifest sets a `NetworkSecurityConfig` that denies cleartext and a test rule installs a `MockWebServer`. The emulator image used in CI has no route to the internet. |
| E2E | The `FakeEngine` and the fake SSH transport; nothing resolves DNS. |
| The runtime download | The one place a test may reach out — and it does not: the download is `MockWebServer`-served from a fixture archive, and the checksum verification path is tested against a deliberately wrong checksum. |

The "no DNS" property is asserted, not assumed: one test asserts that resolving `api.anthropic.com` fails inside the test process.

## Scanning the test sources themselves

A meta-test runs on the JVM and fails if any string literal in `src/test/` or `src/androidTest/` matches a key pattern **and is not in the allowlist of known placeholders**. It is cheap, runs in the normal unit suite, and catches a paste that a git hook missed.

```kotlin
@Test
fun `no credential-shaped string in test sources`() {
    val offenders = scanSourcesForKeyPatterns()
    assertThat(offenders).isEmpty()
}
```

## What happens when one of these fails

A leak is an incident, not a ticket:

1. The build fails.
2. If it was a real key, it is **revoked first**, then the history is cleaned. Revocation before cleanup, because a rewritten history is not a secret anyone else still has.
3. The cause is recorded in `11-operations/` as a process fix — usually a missing hook, a wrong `.gitignore`, or an over-broad secret scope.
4. The check that would have caught it is added to CI.

## The `.gitignore` that backs this up

```
*.jks
*.keystore
keystore.properties
local.properties
.env
.env.*
*.apk
*.aab
build/
captures/
.externalNativeBuild/
.cxx/
```

Note what is *not* ignored: test fixtures, sanitised recordings, and the golden screenshots. They are the evidence that the tests are real, and hiding them would make the safety rules unverifiable.
