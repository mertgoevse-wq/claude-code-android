package dev.ccandroid.core

import kotlinx.serialization.Serializable

/**
 * An opaque reference to a secret, per `docs/07-integrations/secrets.md`.
 *
 * A `SecretRef` is an id, never a key. It cannot be printed usefully — its
 * string form carries no key material, which removes a whole class of
 * accident: a logger, a crash report, or a diff that receives a `SecretRef`
 * receives nothing that matters. The value itself is resolved at the one
 * moment it is needed, through a [SecretStore].
 */
@Serializable
@JvmInline
public value class SecretRef(public val profileId: String) {

    /** Deliberately uninformative: no key material may ever be derivable from it. */
    override fun toString(): String = "SecretRef(${profileId.length} chars)"
}

/**
 * The only way anything in the app touches a secret's value.
 *
 * The reference lives everywhere; the value exists for the duration of one
 * call and is never assigned to a field. The interface is pure: it knows
 * nothing about the Keystore, Room, or Android, so every layer above it — and
 * every test — runs against the in-memory [InMemorySecretStore].
 *
 * The Keystore-backed implementation lives in `androidApp` (one file, per the
 * `KeystoreOnlyInOneFile` rule) and composes [CryptoGateway] with storage;
 * see `docs/07-integrations/secrets.md`.
 */
public interface SecretStore {

    /** Stores `plainText` under a fresh profile and returns its reference. */
    public suspend fun create(name: String, plainText: ByteArray): SecretRef

    /**
     * Resolves the value once. The caller consumes it immediately (typically
     * into a request header) and drops it; nothing may retain the result.
     */
    public suspend fun resolve(ref: SecretRef): ByteArray?

    /** Replaces the value. There is no old value afterwards — rotation is overwrite. */
    public suspend fun rotate(ref: SecretRef, newPlainText: ByteArray): Boolean

    /** Forgets the value. The profile row stays; the caller revokes at the provider. */
    public suspend fun forget(ref: SecretRef): Boolean

    /** A short, non-secret hint so profiles are tellable apart in the UI. */
    public suspend fun hint(ref: SecretRef): String?

    /** Values currently stored, mapped to their profile id, for the log redactor. */
    public suspend fun knownSecrets(): Map<String, String>
}

/**
 * Test double and non-Android fallback. Values are held in memory only; a
 * test that asserts "the key must not appear" has a real value to search for.
 */
public class InMemorySecretStore(
    private val ids: IdGenerator = DefaultIdGenerator(),
    private val time: TimeProvider = SystemTimeProvider(),
) : SecretStore {

    private data class Entry(val name: String, val value: ByteArray, val createdAt: Long)

    private val entries = mutableMapOf<String, Entry>()

    override suspend fun create(name: String, plainText: ByteArray): SecretRef {
        val id = ids.newId("prof")
        entries[id] = Entry(name, plainText.copyOf(), time.currentTimeMillis())
        return SecretRef(id)
    }

    override suspend fun resolve(ref: SecretRef): ByteArray? =
        entries[ref.profileId]?.value?.copyOf()

    override suspend fun rotate(ref: SecretRef, newPlainText: ByteArray): Boolean {
        val entry = entries[ref.profileId] ?: return false
        entries[ref.profileId] = entry.copy(value = newPlainText.copyOf())
        return true
    }

    override suspend fun forget(ref: SecretRef): Boolean = entries.remove(ref.profileId) != null

    override suspend fun hint(ref: SecretRef): String? =
        entries[ref.profileId]?.let { entry ->
            HINT_PREFIX + String(entry.value, Charsets.UTF_8).takeLast(HINT_LENGTH)
        }

    override suspend fun knownSecrets(): Map<String, String> =
        entries.entries
            // An empty value is noise, not a secret: it would match every
            // string in the redaction pass and no caller stores an empty
            // credential by intent.
            .filter { (_, entry) -> entry.value.isNotEmpty() }
            .associate { (id, entry) -> String(entry.value, Charsets.UTF_8) to id }

    private companion object {
        const val HINT_LENGTH = 4
        const val HINT_PREFIX = "••••"
    }
}
