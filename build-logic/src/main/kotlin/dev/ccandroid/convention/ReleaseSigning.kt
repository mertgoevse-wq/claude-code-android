package dev.ccandroid.convention

import java.io.File
import java.util.Properties

/**
 * Release signing material, as specified in
 * `docs/10-build/signing-and-keystores.md`.
 *
 * The build reads `keystore.properties` **only if it exists**. A developer
 * building locally has no such file and gets an unsigned `release` output; the
 * release pipeline injects the real key at tag time and deletes the file
 * afterwards. The key hierarchy is deliberate: the release key is a CI secret
 * and is never persisted to disk outside a release job.
 */
internal data class ReleaseSigningMaterial(
    val storeFile: File,
    val storePassword: String,
    val keyAlias: String,
    val keyPassword: String,
)

/**
 * Returns the signing material, or null when there is none.
 *
 * Null rather than an exception is the point: a build with no signing material
 * must still succeed, producing an unsigned APK, so that a fresh clone compiles
 * without a setup step. Any property that is absent, or a store file that does
 * not exist, is treated the same way rather than half-applying a config.
 */
internal fun loadReleaseSigning(rootDir: File): ReleaseSigningMaterial? {
    val propertiesFile = File(rootDir, "keystore.properties")
    if (!propertiesFile.isFile) return null

    val properties = Properties().apply {
        propertiesFile.inputStream().use { load(it) }
    }

    val storePath = properties.getProperty("storeFile") ?: return null
    val storePassword = properties.getProperty("storePassword") ?: return null
    val keyAlias = properties.getProperty("keyAlias") ?: return null
    val keyPassword = properties.getProperty("keyPassword") ?: return null

    val storeFile = File(storePath)
    if (!storeFile.isFile) return null

    return ReleaseSigningMaterial(storeFile, storePassword, keyAlias, keyPassword)
}
