package dev.ccandroid.data.repository

import dev.ccandroid.core.AppError
import dev.ccandroid.core.ErrorCode
import dev.ccandroid.core.Outcome
import dev.ccandroid.core.SecretRef
import dev.ccandroid.core.SecretStore
import dev.ccandroid.core.tryCatch
import dev.ccandroid.data.dao.SecretProfileDao
import dev.ccandroid.data.entity.SecretProfileEntity
import dev.ccandroid.domain.SecretProfile
import dev.ccandroid.domain.usecase.SecretProfileRepository
import dev.ccandroid.domain.usecase.SecretRefStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The Room-backed secret profile storage, composed with the [SecretStore]
 * that holds the values.
 *
 * The split is the design, not a convenience. This class owns the *facts
 * about* secrets — names, ciphertext, timestamps, the hint — while the
 * [SecretStore] owns the values themselves. Nothing here ever holds a
 * plaintext: the [SecretRefStore] implementation resolves through the store
 * at the moment of use and drops the value (rule 8), and [store] never sees
 * the plaintext at all.
 *
 * Rotation overwrites: there is no old value after [rotate], so there is
 * nothing to fall back to (`RotationNoOldValue`). Forgetting the value leaves
 * the profile row — the app cannot know whether the key still exists at the
 * provider, so the row stays until the user deletes the profile.
 */
public class RoomSecretProfileService(
    private val dao: SecretProfileDao,
    private val store: SecretStore,
) : SecretProfileRepository, SecretRefStore {

    override fun observeAll(): Flow<List<SecretProfile>> =
        dao.observeAllSecretProfiles().map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: String): Outcome<SecretProfile?> = tryCatch {
        dao.getSecretProfileById(id)?.toDomain()
    }

    override suspend fun insert(profile: SecretProfile): Outcome<Unit> = tryCatch {
        dao.insertSecretProfile(SecretProfileEntity.fromDomain(profile))
    }

    override suspend fun update(profile: SecretProfile): Outcome<Unit> = tryCatch {
        dao.updateSecretProfile(SecretProfileEntity.fromDomain(profile))
    }

    /**
     * A profile whose row was deleted while a provider still references it.
     * A provider that quietly stopped working would fail mid-run, so this is
     * a named, typed state instead of an empty resolution.
     */
    override suspend fun getProfileRef(profileId: String): Outcome<SecretRef> {
        val profile = dao.getSecretProfileById(profileId)
            ?: return Outcome.Failure(
                AppError.NotFound(
                    code = ErrorCode.SECRET_PROFILE_NOT_FOUND,
                    messageDe = "Schlüsselprofil nicht gefunden. Weise dem Anbieter ein Schlüssel zu.",
                    messageEn = "Key profile not found. Assign a key to the provider.",
                    resourceId = profileId,
                ),
            )
        return Outcome.Success(SecretRef(profile.id))
    }

    override suspend fun create(name: String, plainText: ByteArray): Outcome<SecretRef> =
        tryCatch { store.create(name, plainText) }

    override suspend fun resolve(ref: SecretRef): Outcome<ByteArray?> = tryCatch {
        // The timestamp is best-effort bookkeeping; a missed touch never
        // blocks the resolution itself.
        runCatching { dao.markUsed(ref.profileId) }
        store.resolve(ref)
    }

    override suspend fun rotate(ref: SecretRef, newPlainText: ByteArray): Outcome<Boolean> = tryCatch {
        val updated = store.rotate(ref, newPlainText)
        if (updated) {
            val profile = dao.getSecretProfileById(ref.profileId)
            profile?.let { dao.updateSecretProfile(it.copy(hint = store.hint(ref) ?: it.hint)) }
        }
        updated
    }

    override suspend fun forget(ref: SecretRef): Outcome<Boolean> = tryCatch {
        store.forget(ref)
    }
}
