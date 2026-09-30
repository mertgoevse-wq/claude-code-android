package dev.ccandroid.domain.usecase

import dev.ccandroid.core.Outcome
import dev.ccandroid.core.SecretRef
import dev.ccandroid.domain.SecretProfile
import kotlinx.coroutines.flow.Flow

/**
 * The facts *about* secrets: names, ciphertext, timestamps, the hint.
 *
 * Deliberately separate from [SecretRefStore]: a repository that could also
 * resolve values would put the plaintext one call away from every mapper and
 * flow in the app. Here the value path is a different interface with no
 * method that returns a profile.
 */
public interface SecretProfileRepository {

    public fun observeAll(): Flow<List<SecretProfile>>

    public suspend fun getById(id: String): Outcome<SecretProfile?>

    public suspend fun insert(profile: SecretProfile): Outcome<Unit>

    public suspend fun update(profile: SecretProfile): Outcome<Unit>
}

/**
 * The value path, per `docs/07-integrations/secrets.md`.
 *
 * A value is resolved at the one moment it is needed — typically into a
 * request header — and dropped. No method here returns anything a caller
 * could sensibly store: `resolve` answers "what do I send right now", never
 * "what is the key".
 */
public interface SecretRefStore {

    /** The reference for a profile id, or a typed NotFound for a broken provider. */
    public suspend fun getProfileRef(profileId: String): Outcome<SecretRef>

    public suspend fun create(name: String, plainText: ByteArray): Outcome<SecretRef>

    public suspend fun resolve(ref: SecretRef): Outcome<ByteArray?>

    /** Rotation is overwrite: after this returns true there is no old value. */
    public suspend fun rotate(ref: SecretRef, newPlainText: ByteArray): Outcome<Boolean>

    /** Forgets the value; the profile row stays and the user revokes at the provider. */
    public suspend fun forget(ref: SecretRef): Outcome<Boolean>
}
