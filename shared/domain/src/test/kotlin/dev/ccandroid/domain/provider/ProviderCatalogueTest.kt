package dev.ccandroid.domain.provider

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The catalogue's own invariants.
 *
 * The list is data someone typed from a provider's documentation, so the risk is
 * not that the code is wrong but that an entry is. A wrong base URL produces a
 * connection error the user cannot act on, and there is no way for them to tell
 * the app apart from a typo. These tests are what stops a plausible guess from
 * shipping.
 */
class ProviderCatalogueTest {

    private val entries = ProviderCatalogue.entries

    @Test
    fun `the list is long enough to be worth a picker`() {
        assertTrue(
            "A picker with three entries is a form with a dropdown, which is the " +
                "thing this feature exists to avoid. Found ${entries.size}.",
            entries.size >= 20,
        )
    }

    @Test
    fun `every id is unique and lowercase, because the id is saved with the provider`() {
        val ids = entries.map { it.id }

        assertEquals("Duplicate provider ids: ${ids.groupBy { it }.filter { it.value.size > 1 }.keys}", ids.size, ids.toSet().size)
        entries.forEach { assertTrue("Id '${it.id}' is not lowercase", it.id == it.id.lowercase()) }
    }

    @Test
    fun `every display name is unique, so the list has no two identical rows`() {
        val names = entries.map { it.displayName }

        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun `every base url parses, or is deliberately left to the user`() {
        entries.forEach { entry ->
            if (entry.baseUrl.isEmpty()) return@forEach
            assertNotNull(
                "'${entry.id}' has a base url that will not parse: ${entry.baseUrl}",
                EndpointUrl.parse(entry.baseUrl),
            )
        }
    }

    @Test
    fun `every path starts with a slash and carries no host of its own`() {
        entries.forEach { entry ->
            assertTrue("'${entry.id}' path '${entry.pathTemplate}' must start with a slash", entry.pathTemplate.startsWith("/"))
            assertTrue(
                "'${entry.id}' path '${entry.pathTemplate}' must not contain a scheme",
                !entry.pathTemplate.contains("://"),
            )
        }
    }

    @Test
    fun `every entry says when it was checked, so a stale one is visible`() {
        entries.forEach { entry ->
            assertTrue("'${entry.id}' has no verification date", entry.verifiedOn.isNotBlank())
            assertEquals(
                "'${entry.id}' was checked on a different day than the rest. Bump VERIFIED_ON " +
                    "or correct the entry, so the date means something.",
                ProviderCatalogue.VERIFIED_ON,
                entry.verifiedOn,
            )
        }
    }

    @Test
    fun `the first party entry is Anthropic, and it speaks the Anthropic dialect`() {
        val anthropic = ProviderCatalogue.byId("anthropic")

        assertNotNull(anthropic)
        assertEquals(ProviderGroup.FIRST_PARTY, anthropic?.group)
        assertEquals(dev.ccandroid.domain.ProviderKind.ANTHROPIC, anthropic?.kind)
        assertEquals("/v1/messages", anthropic?.pathTemplate)
    }

    @Test
    fun `every self-hosted entry is local, or it is asking for a plaintext key to the internet`() {
        entries.filter { it.group == ProviderGroup.SELF_HOSTED }.forEach { entry ->
            val url = EndpointUrl.parse(entry.baseUrl)
            assertNotNull("'${entry.id}' is self-hosted but has no base url", url)
            assertTrue(
                "'${entry.id}' is offered as self-hosted but points at ${entry.baseUrl}, " +
                    "which is not a local address",
                url?.isLocal == true,
            )
        }
    }

    @Test
    fun `a provider that is not in the list is not a dead end`() {
        assertNull(ProviderCatalogue.byId("a-provider-nobody-has-heard-of"))
    }

    @Test
    fun `the groups come out in a fixed order, so the list does not reshuffle`() {
        val groups = ProviderCatalogue.grouped().map { it.first }

        assertEquals(groups, groups.sortedBy { it.ordinal })
        assertEquals(groups.distinct(), groups)
    }
}
