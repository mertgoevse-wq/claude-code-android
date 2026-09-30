package dev.ccandroid.core

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P2-9, per `docs/07-integrations/secrets.md` §Testing: the store is tested
 * where the interface lives, against the in-memory double. The Keystore
 * implementation is a thin `CryptoGateway` composition behind the same
 * interface and is exercised on the device.
 *
 * Fixtures are runtime-constructed per `docs/09-testing/test-data-safety.md`:
 * no credential-shaped literal in source.
 */
class SecretStoreTest {

    private fun store(): InMemorySecretStore = InMemorySecretStore()

    @Test
    fun `round trip - stored value resolves`() = runTest {
        val store = store()
        val ref = store.create("work", FIXTURE_KEY_1.encodeToByteArray())

        val resolved = store.resolve(ref)
        assertEquals(FIXTURE_KEY_1, resolved?.decodeToString())
    }

    @Test
    fun `resolve of a forgotten ref is null`() = runTest {
        val store = store()
        val ref = store.create("work", FIXTURE_KEY_1.encodeToByteArray())
        assertTrue(store.forget(ref))

        assertNull(store.resolve(ref))
        assertFalse("forgetting twice is a bug in the caller, not a no-op to hide", store.forget(ref))
    }

    @Test
    fun `resolve of an unknown ref is null, not an exception`() = runTest {
        val store = store()
        assertNull(store.resolve(SecretRef("prof_does_not_exist")))
    }

    @Test
    fun `rotate overwrites - no old value remains`() = runTest {
        val store = store()
        val ref = store.create("work", FIXTURE_KEY_1.encodeToByteArray())

        assertTrue(store.rotate(ref, FIXTURE_KEY_2.encodeToByteArray()))

        assertEquals(FIXTURE_KEY_2, store.resolve(ref)?.decodeToString())
        assertNotEquals(FIXTURE_KEY_1, store.resolve(ref)?.decodeToString())
    }

    @Test
    fun `rotate of a forgotten ref reports false`() = runTest {
        val store = store()
        val ref = store.create("work", FIXTURE_KEY_1.encodeToByteArray())
        store.forget(ref)

        assertFalse(store.rotate(ref, FIXTURE_KEY_2.encodeToByteArray()))
    }

    @Test
    fun `hint is the last four characters only, with a mask prefix`() = runTest {
        val store = store()
        val ref = store.create("work", FIXTURE_KEY_1.encodeToByteArray())

        val hint = store.hint(ref)
        assertEquals("••••KEY9", hint)
        assertFalse("a hint never carries the whole value", hint!!.contains(FIXTURE_KEY_1))
    }

    @Test
    fun `hint of an unknown ref is null`() = runTest {
        val store = store()
        assertNull(store.hint(SecretRef("prof_missing")))
    }

    @Test
    fun `SecretRef toString carries no key material`() = runTest {
        val store = store()
        val ref = store.create("work", FIXTURE_KEY_1.encodeToByteArray())

        val printed = ref.toString()
        assertFalse(printed.contains(FIXTURE_KEY_1))
        assertFalse(printed.contains(FIXTURE_KEY_1.takeLast(4)))
        assertTrue(printed.contains("SecretRef"))
    }

    @Test
    fun `knownSecrets maps values to profile ids for the log redactor`() = runTest {
        val store = store()
        val first = store.create("work", FIXTURE_KEY_1.encodeToByteArray())
        val second = store.create("home", FIXTURE_KEY_2.encodeToByteArray())

        val known = store.knownSecrets()
        assertEquals(2, known.size)
        assertEquals(first.profileId, known[FIXTURE_KEY_1])
        assertEquals(second.profileId, known[FIXTURE_KEY_2])
    }

    @Test
    fun `resolved value is a copy - mutating it does not corrupt the store`() = runTest {
        val store = store()
        val ref = store.create("work", FIXTURE_KEY_1.encodeToByteArray())

        val resolved = store.resolve(ref)!!
        resolved[0] = 'X'.code.toByte()

        assertEquals(FIXTURE_KEY_1, store.resolve(ref)?.decodeToString())
    }

    @Test
    fun `empty value can be stored but contributes no known secret`() = runTest {
        val store = store()
        val ref = store.create("empty", ByteArray(0))

        assertEquals(0, store.resolve(ref)?.size)
        assertFalse(store.knownSecrets().containsKey(""))
    }

    private companion object {
        private const val TAIL = "KEY9"
        private val FIXTURE_KEY_1 = "FIXTURE-sk-" + "a".repeat(20) + TAIL
        private val FIXTURE_KEY_2 = "FIXTURE-sk-" + "b".repeat(24)
    }
}
