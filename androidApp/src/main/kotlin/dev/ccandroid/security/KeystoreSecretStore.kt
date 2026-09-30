package dev.ccandroid.security

import dev.ccandroid.core.CryptoGateway
import dev.ccandroid.core.DefaultIdGenerator
import dev.ccandroid.core.IdGenerator
import dev.ccandroid.core.SecretRef
import dev.ccandroid.core.SecretStore
import dev.ccandroid.core.TimeProvider
import dev.ccandroid.data.dao.SecretProfileDao
import dev.ccandroid.data.entity.SecretProfileEntity
import java.util.Base64
import kotlinx.coroutines.flow.first

/**
 * The production [SecretStore]: values encrypted through the Keystore
 * ([CryptoGateway]), facts persisted as `SecretProfile` rows through
 * [SecretProfileDao].
 *
 * The split follows `docs/07-integrations/secrets.md`: the database stores
 * ciphertext and never sees a plaintext; the Keystore key never leaves the
 * hardware. This class is the only place the two meet.
 *
 * Rotation overwrites the ciphertext — there is no old value afterwards.
 * Forgetting removes the Keystore key and blanks the ciphertext, but keeps
 * the row: the app cannot know whether the key still exists at the provider,
 * so the profile stays until the user deletes it.
 */
public class KeystoreSecretStore(
    private val crypto: CryptoGateway,
    private val dao: SecretProfileDao,
    private val ids: IdGenerator = DefaultIdGenerator(),
    private val time: TimeProvider = dev.ccandroid.core.SystemTimeProvider(),
) : SecretStore {

    override suspend fun create(name: String, plainText: ByteArray): SecretRef {
        val id = ids.newId("prof")
        val alias = ALIAS_PREFIX + id
        val combined = crypto.encrypt(alias, plainText)
        dao.insertSecretProfile(
            SecretProfileEntity(
                id = id,
                name = name,
                cipherText = encode(combined.copyOfRange(IV_LENGTH, combined.size)),
                iv = encode(combined.copyOfRange(0, IV_LENGTH)),
                keyAlias = alias,
                hint = hintOf(plainText),
                createdAt = time.currentTimeMillis(),
                lastUsedAt = null,
            ),
        )
        return SecretRef(id)
    }

    override suspend fun resolve(ref: SecretRef): ByteArray? {
        val row = dao.getSecretProfileById(ref.profileId) ?: return null
        // Forgotten: the value is gone, the row remembers the profile.
        if (row.cipherText.isEmpty()) return null
        val combined = decode(row.iv) + decode(row.cipherText)
        return crypto.decrypt(row.keyAlias, combined)
    }

    override suspend fun rotate(ref: SecretRef, newPlainText: ByteArray): Boolean {
        val row = dao.getSecretProfileById(ref.profileId) ?: return false
        val combined = crypto.encrypt(row.keyAlias, newPlainText)
        dao.updateSecretProfile(
            row.copy(
                cipherText = encode(combined.copyOfRange(IV_LENGTH, combined.size)),
                iv = encode(combined.copyOfRange(0, IV_LENGTH)),
                hint = hintOf(newPlainText),
            ),
        )
        return true
    }

    override suspend fun forget(ref: SecretRef): Boolean {
        val row = dao.getSecretProfileById(ref.profileId) ?: return false
        crypto.deleteKey(row.keyAlias)
        // An overwrite, not a delete: the row stays so a referencing provider
        // shows as broken instead of silently disappearing.
        dao.updateSecretProfile(row.copy(cipherText = "", iv = "", hint = MASK))
        return true
    }

    override suspend fun hint(ref: SecretRef): String? =
        dao.getSecretProfileById(ref.profileId)?.hint

    override suspend fun knownSecrets(): Map<String, String> =
        dao.observeAllSecretProfiles().first()
            .filter { it.cipherText.isNotEmpty() }
            .mapNotNull { row ->
                val value = resolve(SecretRef(row.id)) ?: return@mapNotNull null
                String(value, Charsets.UTF_8) to row.id
            }
            .toMap()

    private fun hintOf(plainText: ByteArray): String =
        MASK + String(plainText, Charsets.UTF_8).takeLast(HINT_LENGTH)

    private fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    private fun decode(text: String): ByteArray = Base64.getDecoder().decode(text)

    private companion object {
        const val ALIAS_PREFIX = "cca_secret_"
        const val MASK = "••••"
        const val HINT_LENGTH = 4
        const val IV_LENGTH = 12
    }
}
