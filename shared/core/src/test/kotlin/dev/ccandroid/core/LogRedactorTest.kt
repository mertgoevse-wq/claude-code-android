package dev.ccandroid.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fixture values for the redaction pass. Every fixture is constructed at
 * runtime from fragments, per `09-testing/test-data-safety.md`: a fixture
 * that resembles a credential in source would trip `check-no-secrets.sh`,
 * and more importantly nothing in a fixture may ever be a real credential.
 * All fragments are visibly fake (`FIXTURE` markers, `.invalid` hosts).
 */
private object Fixtures {
    fun anthropicKey() = "sk-ant-" + "FIXTURE" + "0123456789abcdef0123456789abcdef"
    fun openAiKey() = "sk-" + "FIXTURE0123456789abcdef0123456789abcdef"
    fun githubToken() = "ghp_" + "FIXTURE0123456789abcdef0123456789abcdef"
    fun githubPat() = "github_pat_" + "FIXTURE0123456789abcdefghij0123456789abcdefghij"
    fun googleKey() = "AIza" + "FIXTURE0123456789abcdef0123456789abcdef"
    fun awsKey() = "AKIA" + "FIXTURE0123456789ABCD"
    fun slackToken() = "xoxb-" + "FIXTURE0123456789"
    fun stripeKey() = "sk_live_" + "FIXTURE0123456789abcdef"
    fun googleOauth() = "GOCSPX-" + "FIXTURE0123456789abcdef"
    fun privateKey() = "-----BEGIN PRIVATE KEY-----\nFIXTURE\n-----END PRIVATE KEY-----"
    fun jwt() =
        "eyJ" + "FIXTUREheaderFIXTUREheaderFIXTURE" +
            ".eyJ" + "FIXTUREpayloadFIXTUREpayloadFIXTU" +
            ".Sfl" + "FIXTUREsignatureFIXTUREsignatureFIX"
    fun bearer() = "Bearer " + "FIXTURE0123456789abcdef0123456789abcdef"
    fun urlWithToken() = "https://api.invalid/v1?token=" + "FIXTURE0123456789" + "&x=1"
    fun urlWithKey() = "https://api.invalid/v1?key=" + "FIXTURE0123456789"
    fun urlWithSecret() = "https://api.invalid/v1?secret=" + "FIXTURE0123456789"
    fun urlWithAccessToken() = "https://api.invalid/v1?access_token=" + "FIXTURE0123456789"
    fun plainUrl() = "https://api.invalid/v1?limit=20&offset=0"
    fun versionString() = "1.2.3"
}

class LogRedactorTest {

    private val redactor = LogRedactor()

    // --- value shapes -------------------------------------------------------

    @Test
    fun `anthropic key is redacted with its prefix kept`() {
        val out = redactor.redactText("auth ${Fixtures.anthropicKey()} failed")
        assertFalse(out.contains("FIXTURE0123456789"))
        assertTrue(out.startsWith("auth sk-ant-…redacted"))
    }

    @Test
    fun `openai style key is redacted`() {
        val out = redactor.redactText("key ${Fixtures.openAiKey()}")
        assertFalse(out.contains("FIXTURE"))
        assertTrue(out.contains("sk-…redacted"))
    }

    @Test
    fun `github token is redacted`() {
        val out = redactor.redactText("push with ${Fixtures.githubToken()}")
        assertFalse(out.contains("FIXTURE"))
        assertTrue(out.contains("ghp_…redacted"))
    }

    @Test
    fun `github fine grained pat is redacted`() {
        val out = redactor.redactText("pat ${Fixtures.githubPat()}")
        assertFalse(out.contains("FIXTURE"))
        assertTrue(out.contains("github_pat_…redacted"))
    }

    @Test
    fun `google api key is redacted`() {
        val out = redactor.redactText("maps ${Fixtures.googleKey()}")
        assertFalse(out.contains("FIXTURE"))
        assertTrue(out.contains("AIza…redacted"))
    }

    @Test
    fun `aws access key is redacted`() {
        val out = redactor.redactText("s3 ${Fixtures.awsKey()}")
        assertFalse(out.contains("FIXTURE"))
        assertTrue(out.contains("AKIA…redacted"))
    }

    @Test
    fun `slack token is redacted`() {
        val out = redactor.redactText("webhook ${Fixtures.slackToken()}")
        assertFalse(out.contains("FIXTURE"))
        assertTrue(out.contains("xoxb-…redacted"))
    }

    @Test
    fun `stripe live key is redacted`() {
        val out = redactor.redactText("pay ${Fixtures.stripeKey()}")
        assertFalse(out.contains("FIXTURE"))
        assertTrue(out.contains("sk_live_…redacted"))
    }

    @Test
    fun `google oauth client secret is redacted`() {
        val out = redactor.redactText("oauth ${Fixtures.googleOauth()}")
        assertFalse(out.contains("FIXTURE"))
        assertTrue(out.contains("GOCSPX-…redacted"))
    }

    @Test
    fun `pem private key block is removed entirely`() {
        val out = redactor.redactText("key:\n${Fixtures.privateKey()}\nend")
        assertFalse(out.contains("FIXTURE"))
        assertFalse(out.contains("PRIVATE KEY"))
    }

    @Test
    fun `jwt is removed entirely`() {
        val out = redactor.redactText("session ${Fixtures.jwt()} expired")
        assertFalse(out.contains("FIXTURE"))
        assertTrue(out.contains("session …redacted expired"))
    }

    @Test
    fun `bearer header value is redacted`() {
        val out = redactor.redactText("Authorization: ${Fixtures.bearer()}")
        assertFalse(out.contains("FIXTURE"))
        assertTrue(out.lowercase().contains("bearer …redacted"))
    }

    @Test
    fun `version numbers are not mistaken for jwts`() {
        val text = "engine ${Fixtures.versionString()} installed"
        assertEquals(text, redactor.redactText(text))
    }

    @Test
    fun `plain query parameters survive`() {
        val text = "GET ${Fixtures.plainUrl()}"
        assertEquals(text, redactor.redactText(text))
    }

    // --- url query parameters -----------------------------------------------

    @Test
    fun `token query parameter is redacted`() {
        val out = redactor.redactText(Fixtures.urlWithToken())
        assertFalse(out.contains("FIXTURE"))
        assertTrue(out.contains("token=…redacted"))
        assertTrue(out.contains("&x=1"))
    }

    @Test
    fun `key query parameter is redacted`() {
        val out = redactor.redactText(Fixtures.urlWithKey())
        assertFalse(out.contains("FIXTURE"))
        assertTrue(out.contains("key=…redacted"))
    }

    @Test
    fun `secret query parameter is redacted`() {
        val out = redactor.redactText(Fixtures.urlWithSecret())
        assertFalse(out.contains("FIXTURE"))
        assertTrue(out.contains("secret=…redacted"))
    }

    @Test
    fun `access_token query parameter is redacted`() {
        val out = redactor.redactText(Fixtures.urlWithAccessToken())
        assertFalse(out.contains("FIXTURE"))
        assertTrue(out.contains("access_token=…redacted"))
    }

    // --- attributes by name ---------------------------------------------------

    @Test
    fun `apiKey attribute is replaced unconditionally`() {
        val out = redactor.redactAttribute("apiKey", "harmless-looking-value")
        assertEquals("…redacted", out)
    }

    @Test
    fun `token attribute is replaced unconditionally`() {
        assertEquals("…redacted", redactor.redactAttribute("token", "value"))
    }

    @Test
    fun `secret attribute is replaced unconditionally`() {
        assertEquals("…redacted", redactor.redactAttribute("secret", "value"))
    }

    @Test
    fun `password attribute is replaced unconditionally`() {
        assertEquals("…redacted", redactor.redactAttribute("password", "hunter2"))
    }

    @Test
    fun `authorization attribute is replaced unconditionally`() {
        assertEquals("…redacted", redactor.redactAttribute("authorization", "Basic abc"))
    }

    @Test
    fun `cookie attribute is replaced unconditionally`() {
        assertEquals("…redacted", redactor.redactAttribute("cookie", "session=abc"))
    }

    @Test
    fun `email attribute is redacted even on debug builds`() {
        assertEquals("…redacted", redactor.redactAttribute("email", "person@invalid"))
    }

    @Test
    fun `userName attribute is redacted`() {
        assertEquals("…redacted", redactor.redactAttribute("userName", "mert"))
    }

    @Test
    fun `fullName attribute is redacted`() {
        assertEquals("…redacted", redactor.redactAttribute("fullName", "Mert"))
    }

    @Test
    fun `content attribute becomes an omission marker with the original length`() {
        val content = "x".repeat(300)
        assertEquals("<omitted: 300 chars>", redactor.redactAttribute("content", content))
    }

    @Test
    fun `prompt attribute becomes an omission marker`() {
        assertEquals("<omitted: 5 chars>", redactor.redactAttribute("prompt", "hello"))
    }

    @Test
    fun `body attribute becomes an omission marker`() {
        assertEquals("<omitted: 5 chars>", redactor.redactAttribute("body", "hello"))
    }

    @Test
    fun `diff attribute becomes an omission marker`() {
        assertEquals("<omitted: 4 chars>", redactor.redactAttribute("diff", "line"))
    }

    @Test
    fun `tool input attribute is replaced`() {
        assertEquals("…redacted", redactor.redactAttribute("input", "cat secrets.txt"))
    }

    @Test
    fun `tool output attribute is replaced`() {
        assertEquals("…redacted", redactor.redactAttribute("output", "api_key=abc"))
    }

    @Test
    fun `path attribute is reduced to its last two segments`() {
        val out = redactor.redactAttribute("path", "/storage/emulated/0/Android/data/dev.ccandroid/files/cca/project/src/main.kt")
        assertFalse(out.contains("storage"))
        assertEquals("src/main.kt", out)
    }

    @Test
    fun `short path attribute survives`() {
        assertEquals("src/main.kt", redactor.redactAttribute("path", "src/main.kt"))
    }

    // --- attributes by shape --------------------------------------------------

    @Test
    fun `unnamed attribute carrying a key shape is still redacted`() {
        val out = redactor.redactAttribute("note", "uses ${Fixtures.openAiKey()} today")
        assertFalse(out.contains("FIXTURE"))
    }

    @Test
    fun `over long attribute is truncated with a marker`() {
        val value = "y".repeat(600)
        val out = redactor.redactAttribute("note", value)
        assertTrue(out.length < 600)
        assertTrue(out.endsWith("…truncated (600 chars)"))
    }

    @Test
    fun `attribute name matching is case insensitive`() {
        assertEquals("…redacted", redactor.redactAttribute("APIKEY", "value"))
        assertEquals("…redacted", redactor.redactAttribute("Token", "value"))
    }

    // --- known secrets from the store ------------------------------------------

    @Test
    fun `a stored secret value is replaced with its profile id`() {
        val withStore = LogRedactor(knownSecrets = mapOf(Fixtures.openAiKey() to "prof_01"))
        val out = withStore.redactText("call with ${Fixtures.openAiKey()} failed")
        assertTrue(out.contains("<secret:prof_01>"))
        assertFalse(out.contains("FIXTURE"))
    }

    @Test
    fun `an empty known secret value is ignored`() {
        val withStore = LogRedactor(knownSecrets = mapOf("" to "prof_01"))
        assertEquals("hello", withStore.redactText("hello"))
    }

    // --- whole records ---------------------------------------------------------

    @Test
    fun `record message attributes and throwable are all redacted`() {
        val record = LogRecord(
            timestampMillis = 1000L,
            level = LogLevel.WARN,
            tag = "Provider",
            message = "reject ${Fixtures.anthropicKey()}",
            throwable = IllegalStateException("bad key ${Fixtures.openAiKey()}"),
            attributes = mapOf("path" to "/a/b/c/d/main.kt", "prompt" to "top secret prompt"),
        )
        val out = LogRedactor().redact(record)
        assertFalse(out.message.contains("FIXTURE"))
        assertFalse(out.throwable!!.message!!.contains("FIXTURE"))
        assertEquals("d/main.kt", out.attributes["path"])
        assertEquals("<omitted: 17 chars>", out.attributes["prompt"])
    }

    @Test
    fun `record without secrets is passed through unchanged`() {
        val record = LogRecord(
            timestampMillis = 1000L,
            level = LogLevel.INFO,
            tag = "Runtime",
            message = "Bootstrap reached READY",
            attributes = mapOf("version" to "2.1.211"),
        )
        val out = LogRedactor().redact(record)
        assertEquals("Bootstrap reached READY", out.message)
        assertEquals("2.1.211", out.attributes["version"])
    }

    // --- the tree: redaction is structural --------------------------------------

    @Test
    fun `writer behind the tree never sees a raw secret`() {
        val written = mutableListOf<LogRecord>()
        val tree = RedactingLogTree(delegate = { written.add(it) })
        tree.write(
            LogRecord(
                timestampMillis = 1L,
                level = LogLevel.DEBUG,
                tag = "T",
                message = "key ${Fixtures.githubToken()}",
            )
        )
        assertEquals(1, written.size)
        assertFalse(written.single().message.contains("FIXTURE"))
        assertTrue(written.single().message.contains("ghp_…redacted"))
    }

    @Test
    fun `writer behind the tree sees the redacted throwable and attributes`() {
        val written = mutableListOf<LogRecord>()
        val tree = RedactingLogTree(delegate = { written.add(it) })
        tree.write(
            LogRecord(
                timestampMillis = 1L,
                level = LogLevel.ERROR,
                tag = "T",
                message = "failed",
                throwable = RuntimeException("leak ${Fixtures.slackToken()}"),
                attributes = mapOf("output" to "token=abc"),
            )
        )
        val record = written.single()
        assertFalse(record.throwable!!.message!!.contains("FIXTURE"))
        assertEquals("…redacted", record.attributes["output"])
    }

    @Test
    fun `tree with a custom redactor passes the redactor through`() {
        val written = mutableListOf<LogRecord>()
        val custom = LogRedactor(knownSecrets = mapOf("FIXTURE-STORED-VALUE" to "prof_02"))
        val tree = RedactingLogTree(delegate = { written.add(it) }, redactor = custom)
        tree.write(
            LogRecord(
                timestampMillis = 1L,
                level = LogLevel.WARN,
                tag = "T",
                message = "stored FIXTURE-STORED-VALUE used",
            )
        )
        assertTrue(written.single().message.contains("<secret:prof_02>"))
    }
}
