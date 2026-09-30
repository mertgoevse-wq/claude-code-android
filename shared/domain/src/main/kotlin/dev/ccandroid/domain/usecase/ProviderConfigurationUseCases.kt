package dev.ccandroid.domain.usecase

import dev.ccandroid.core.AppError
import dev.ccandroid.core.ErrorCode
import dev.ccandroid.core.Outcome
import dev.ccandroid.domain.ProviderKind
import dev.ccandroid.domain.provider.AuthFormFor
import dev.ccandroid.domain.provider.AuthHeaderForm
import dev.ccandroid.domain.provider.AuthHeaders
import dev.ccandroid.domain.provider.EndpointUrl
import dev.ccandroid.domain.provider.PathTemplates
import dev.ccandroid.domain.provider.ProviderCatalogue
import dev.ccandroid.domain.provider.ProviderCatalogueEntry

/**
 * Provider configuration, per `docs/07-integrations/providers.md` and task P2-11.
 *
 * Every rule here is a rule about what would go wrong later, not a style
 * preference. A refused field names what would have happened and what to do
 * instead, because the user is at a keyboard on a phone and "invalid" tells
 * them nothing they did not already suspect.
 *
 * Nothing here touches the network and nothing here touches a key. The key is a
 * `SecretRef` in, and a key value never appears in a result.
 */

/** What the user typed, before it is a saved provider. */
public data class ProviderDraft(
    val name: String,
    val kind: ProviderKind,
    val baseUrl: String,
    val pathTemplate: String? = null,
    val authForm: AuthHeaderForm? = null,
    val extraHeaders: Map<String, String> = emptyMap(),
    val model: String? = null,
    /** The catalogue id, when the provider came from the list rather than typed. */
    val catalogueId: String? = null,
    /** Whether the user has been shown, and accepted, a plaintext local address. */
    val localNetworkConfirmed: Boolean = false,
)

/** A draft that passed every rule, with the values resolved. */
public data class ValidatedProvider(
    val name: String,
    val kind: ProviderKind,
    val baseUrl: String,
    val pathTemplate: String,
    val authForm: AuthHeaderForm,
    val extraHeaders: Map<String, String>,
    val completionUrl: String,
    val modelsUrl: String?,
    val model: String?,
)

/**
 * Aliases that name a moving target.
 *
 * `latest` is not a model. A run recorded against it cannot be reproduced,
 * cannot be costed, and cannot be compared to the next run. The provider's
 * unpinned family aliases have the same problem, so they are refused by name
 * and the message says which id to type instead.
 */
public object UnpinnedModelAliases {
    private val ALIASES = setOf(
        "latest",
        "gpt-4o-latest",
        "gpt-4.1-latest",
        "gpt-5-latest",
        "claude-3-5-sonnet-latest",
        "claude-sonnet-4-latest",
        "claude-opus-4-latest",
    )

    public fun isUnpinned(modelId: String): Boolean =
        ALIASES.contains(modelId.trim().lowercase()) || modelId.trim().lowercase().endsWith("-latest")
}

public object ValidateProviderDraft {

    public operator fun invoke(draft: ProviderDraft): Outcome<ValidatedProvider> {
        val name = draft.name.trim()
        if (name.isEmpty()) return invalid("PROVIDER_NAME_EMPTY", "Gib dem Anbieter einen Namen.", "Give the provider a name.")
        if (name.length > MAX_NAME_LENGTH) {
            return invalid(
                "PROVIDER_NAME_TOO_LONG",
                "Der Name darf höchstens $MAX_NAME_LENGTH Zeichen haben.",
                "The name may be at most $MAX_NAME_LENGTH characters.",
            )
        }

        val reservedHeader = draft.extraHeaders.keys.firstOrNull { it.lowercase() in AuthHeaders.RESERVED }
        if (reservedHeader != null) {
            // Naming the collision matters: a shadowed key surfaces at the
            // provider as a rejected key, which sends the user to the wrong page.
            return invalid(
                "PROVIDER_HEADER_RESERVED",
                "„$reservedHeader“ setzt die App selbst. Der Schlüssel wäre nicht mehr wirksam.",
                "\"$reservedHeader\" is set by the app itself, so the key would no longer take effect.",
            )
        }

        val url = EndpointUrl.parse(draft.baseUrl)
            ?: return invalid(
                "PROVIDER_URL_INVALID",
                "Die Adresse ist ungültig. Sie braucht Schema und Host, zum Beispiel https://api.example.com",
                "That address is not valid. It needs a scheme and a host, for example https://api.example.com",
            )

        if (url.isPlaintext && !url.isLocal) {
            return invalid(
                "PROVIDER_URL_PLAINTEXT_PUBLIC",
                "„${url.host}“ ist nicht im lokalen Netz. Ein Schlüssel ginge im Klartext über die Leitung.",
                "\"${url.host}\" is not on your local network, so the key would travel in clear text.",
            )
        }
        if (url.isPlaintext && !draft.localNetworkConfirmed) {
            // Not an error. A local model is a real use; the user is asked once.
            return Outcome.Failure(
                AppError.Simple(
                    code = ErrorCode.PROVIDER_UNREACHABLE,
                    messageDe = "„${url.origin}“ liegt in deinem lokalen Netz. Verbindung ohne Verschlüsselung.",
                    messageEn = "\"${url.origin}\" is on your local network. Connecting without encryption.",
                    retryable = false,
                    details = REASON_LOCAL_PLAINTEXT,
                ),
            )
        }

        val pathTemplate = PathTemplates.render(
            draft.pathTemplate?.takeIf { it.isNotBlank() } ?: PathTemplates.defaultFor(draft.kind),
            url,
        )

        val model = draft.model?.trim()?.takeIf { it.isNotEmpty() }
        if (model != null && UnpinnedModelAliases.isUnpinned(model)) {
            return invalid(
                "PROVIDER_MODEL_UNPINNED",
                "„$model“ ist ein Alias, kein Modell. Ohne genaue ID ist der Lauf später nicht nachvollziehbar.",
                "\"$model\" is an alias, not a model. Without an exact id the run cannot be reproduced later.",
            )
        }

        val catalogue = draft.catalogueId?.let(ProviderCatalogue::byId)
        return Outcome.Success(
            ValidatedProvider(
                name = name,
                kind = draft.kind,
                baseUrl = url.origin,
                pathTemplate = pathTemplate,
                authForm = AuthFormFor.forKind(draft.kind, draft.authForm),
                extraHeaders = draft.extraHeaders,
                completionUrl = url.resolve(pathTemplate),
                modelsUrl = catalogue?.modelsPath?.let(url::resolve),
                model = model,
            ),
        )
    }

    private fun invalid(code: String, de: String, en: String): Outcome<ValidatedProvider> =
        Outcome.Failure(
            AppError.Simple(
                code = ErrorCode.PROVIDER_BAD_REQUEST,
                messageDe = de,
                messageEn = en,
                retryable = false,
                details = code,
            ),
        )

    public const val REASON_LOCAL_PLAINTEXT: String = "LOCAL_PLAINTEXT_NEEDS_CONFIRMATION"
    private const val MAX_NAME_LENGTH = 60
}

/**
 * The change that has to be announced rather than applied.
 *
 * Changing the dialect resets the path, because a path from one dialect means
 * nothing to another: `/v1/messages` on an OpenAI-shaped server is a 404 with a
 * message nobody reads. Silently resetting it would look like the app forgot
 * the user's setting, so it is returned as a pending change the screen shows.
 */
public data class KindChange(
    val kind: ProviderKind,
    val pathTemplate: String,
    val authForm: AuthHeaderForm,
    val headersToDrop: List<String>,
)

public object ChangeProviderKind {

    public operator fun invoke(from: ProviderKind, to: ProviderKind): Outcome<KindChange> {
        if (from == to) {
            return Outcome.Success(
                KindChange(
                    kind = to,
                    pathTemplate = PathTemplates.defaultFor(to),
                    authForm = AuthFormFor.forKind(to),
                    headersToDrop = emptyList(),
                ),
            )
        }
        val dialectHeader = when (to) {
            ProviderKind.ANTHROPIC -> AuthHeaders.X_API_KEY
            else -> AuthHeaders.AUTHORIZATION
        }
        return Outcome.Success(
            KindChange(
                kind = to,
                pathTemplate = PathTemplates.defaultFor(to),
                authForm = AuthFormFor.forKind(to),
                headersToDrop = listOf(dialectHeader),
            ),
        )
    }
}

/**
 * The default model out of a discovered list.
 *
 * A short built-in list of ids the app has seen, then the first from the server
 * in the order the server sent it. Never `latest`, because a default that moves
 * makes every cost record after it a guess.
 */
public object ChooseDefaultModel {

    private val KNOWN = listOf(
        "claude-sonnet-4-5",
        "claude-opus-4-1",
        "gpt-5",
        "gpt-4.1",
        "o3",
        "llama-3.3-70b-versatile",
        "qwen2.5-coder-32b-instruct",
    )

    public operator fun invoke(discovered: List<String>): String? {
        if (discovered.isEmpty()) return null
        val usable = discovered.map { it.trim() }.filter { it.isNotEmpty() && !UnpinnedModelAliases.isUnpinned(it) }
        if (usable.isEmpty()) return null
        return KNOWN.firstOrNull { known -> usable.any { it.equals(known, ignoreCase = true) } }
            ?: usable.first()
    }
}

/** The catalogue as the picker reads it, so the screen has one source. */
public object BrowseProviderCatalogue {

    public operator fun invoke(): List<Pair<dev.ccandroid.domain.provider.ProviderGroup, List<ProviderCatalogueEntry>>> =
        ProviderCatalogue.grouped()
}
