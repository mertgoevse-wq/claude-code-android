package dev.ccandroid.domain.provider

import dev.ccandroid.domain.ProviderKind

/**
 * One entry in the provider picker.
 *
 * The user picks a provider from a list rather than typing a base URL, because
 * typing a base URL is where provider configuration usually goes wrong: a
 * missing `/v1`, a path that belongs to a gateway rather than the vendor, a
 * header the vendor does not use. The entry carries what the app knows so the
 * user does not have to.
 *
 * [baseUrl] is the root, never a full endpoint. [pathTemplate] is the rest, and
 * it differs per provider even when the dialect is the same — Groq serves
 * `/openai/v1/chat/completions` where DeepSeek serves `/v1/chat/completions`.
 * Collapsing those into one string is exactly the mistake this split prevents.
 */
public data class ProviderCatalogueEntry(
    /** Stable, lowercase, and part of the saved configuration. Never shown raw. */
    val id: String,
    /** The name the user reads in the list. */
    val displayName: String,
    val group: ProviderGroup,
    val kind: ProviderKind,
    /** Scheme and host only. A private-range host may be plain http. */
    val baseUrl: String,
    /** The completion path. Rendered with the placeholders [kind] implies. */
    val pathTemplate: String,
    /** Where the model list is discovered, when the provider has one. */
    val modelsPath: String? = "/v1/models",
    /** A one-line hint for the key field, so the user knows what is expected. */
    val keyHint: String,
    /** Where the key comes from. The app never guesses this. */
    val keyUrl: String? = null,
    /** The day these values were checked against the provider's own documentation. */
    val verifiedOn: String,
)

/** How the picker groups the list, in the order the groups appear. */
public enum class ProviderGroup {
    /** The vendor the engine was written for. */
    FIRST_PARTY,

    /** A hosted vendor that speaks one of the two dialects. */
    HOSTED,

    /** A gateway in front of several vendors, one key. */
    GATEWAY,

    /** The user's own machine or network. Usually plain http, and always local. */
    SELF_HOSTED,
}

/**
 * The provider list, per `docs/07-integrations/providers.md`.
 *
 * Every entry is a fact that was looked up on [verifiedOn] and written down with
 * where it was found, in `docs/10-build/provider-catalogue-sources.md`. A
 * plausible guess is a defect with a citation: a wrong base URL produces a
 * connection error the user cannot act on, and there is no way for them to tell
 * the app apart from a typo.
 *
 * A provider that is not here is still reachable. `CUSTOM` takes a base URL, a
 * path, and a dialect, and that path is the supported one for a server this
 * project has never heard of. Adding a vendor is one entry here plus one row in
 * the sources document.
 */
public object ProviderCatalogue {

    /** The day the entries below were checked. Bump it when the list changes. */
    public const val VERIFIED_ON: String = "2026-09-30"

    public val entries: List<ProviderCatalogueEntry> = listOf(
        // --- First party ---------------------------------------------------
        ProviderCatalogueEntry(
            id = "anthropic",
            displayName = "Anthropic",
            group = ProviderGroup.FIRST_PARTY,
            kind = ProviderKind.ANTHROPIC,
            baseUrl = "https://api.anthropic.com",
            pathTemplate = "/v1/messages",
            modelsPath = "/v1/models",
            keyHint = "sk-ant-…",
            keyUrl = "https://console.anthropic.com/settings/keys",
            verifiedOn = VERIFIED_ON,
        ),

        // --- Gateways ------------------------------------------------------
        ProviderCatalogueEntry(
            id = "openrouter",
            displayName = "OpenRouter",
            group = ProviderGroup.GATEWAY,
            kind = ProviderKind.OPENAI_CHAT,
            baseUrl = "https://openrouter.ai",
            pathTemplate = "/api/v1/chat/completions",
            modelsPath = "/api/v1/models",
            keyHint = "sk-or-…",
            keyUrl = "https://openrouter.ai/keys",
            verifiedOn = VERIFIED_ON,
        ),
        ProviderCatalogueEntry(
            id = "litellm",
            displayName = "LiteLLM Proxy",
            group = ProviderGroup.GATEWAY,
            kind = ProviderKind.OPENAI_CHAT,
            baseUrl = "",
            pathTemplate = "/v1/chat/completions",
            modelsPath = "/v1/models",
            keyHint = "sk-… oder leer für einen lokalen Proxy",
            verifiedOn = VERIFIED_ON,
        ),

        // --- Hosted, OpenAI chat dialect -----------------------------------
        ProviderCatalogueEntry(
            id = "openai",
            displayName = "OpenAI",
            group = ProviderGroup.HOSTED,
            kind = ProviderKind.OPENAI_CHAT,
            baseUrl = "https://api.openai.com",
            pathTemplate = "/v1/chat/completions",
            modelsPath = "/v1/models",
            keyHint = "sk-…",
            keyUrl = "https://platform.openai.com/api-keys",
            verifiedOn = VERIFIED_ON,
        ),
        ProviderCatalogueEntry(
            id = "groq",
            displayName = "Groq",
            group = ProviderGroup.HOSTED,
            kind = ProviderKind.OPENAI_CHAT,
            baseUrl = "https://api.groq.com",
            pathTemplate = "/openai/v1/chat/completions",
            modelsPath = "/openai/v1/models",
            keyHint = "gsk_…",
            keyUrl = "https://console.groq.com/keys",
            verifiedOn = VERIFIED_ON,
        ),
        ProviderCatalogueEntry(
            id = "deepseek",
            displayName = "DeepSeek",
            group = ProviderGroup.HOSTED,
            kind = ProviderKind.OPENAI_CHAT,
            baseUrl = "https://api.deepseek.com",
            pathTemplate = "/v1/chat/completions",
            modelsPath = "/v1/models",
            keyHint = "sk-…",
            keyUrl = "https://platform.deepseek.com/api_keys",
            verifiedOn = VERIFIED_ON,
        ),
        ProviderCatalogueEntry(
            id = "mistral",
            displayName = "Mistral",
            group = ProviderGroup.HOSTED,
            kind = ProviderKind.OPENAI_CHAT,
            baseUrl = "https://api.mistral.ai",
            pathTemplate = "/v1/chat/completions",
            modelsPath = "/v1/models",
            keyHint = "…",
            keyUrl = "https://console.mistral.ai/api-keys",
            verifiedOn = VERIFIED_ON,
        ),
        ProviderCatalogueEntry(
            id = "together",
            displayName = "Together AI",
            group = ProviderGroup.HOSTED,
            kind = ProviderKind.OPENAI_CHAT,
            baseUrl = "https://api.together.xyz",
            pathTemplate = "/v1/chat/completions",
            modelsPath = "/v1/models",
            keyHint = "…",
            keyUrl = "https://api.together.ai/settings/api-keys",
            verifiedOn = VERIFIED_ON,
        ),
        ProviderCatalogueEntry(
            id = "fireworks",
            displayName = "Fireworks AI",
            group = ProviderGroup.HOSTED,
            kind = ProviderKind.OPENAI_CHAT,
            baseUrl = "https://api.fireworks.ai",
            pathTemplate = "/inference/v1/chat/completions",
            modelsPath = "/inference/v1/models",
            keyHint = "…",
            keyUrl = "https://fireworks.ai/api-keys",
            verifiedOn = VERIFIED_ON,
        ),
        ProviderCatalogueEntry(
            id = "cerebras",
            displayName = "Cerebras",
            group = ProviderGroup.HOSTED,
            kind = ProviderKind.OPENAI_CHAT,
            baseUrl = "https://api.cerebras.ai",
            pathTemplate = "/v1/chat/completions",
            modelsPath = "/v1/models",
            keyHint = "…",
            keyUrl = "https://inference.cerebras.ai/manage/api-keys",
            verifiedOn = VERIFIED_ON,
        ),
        ProviderCatalogueEntry(
            id = "xai",
            displayName = "xAI",
            group = ProviderGroup.HOSTED,
            kind = ProviderKind.OPENAI_CHAT,
            baseUrl = "https://api.x.ai",
            pathTemplate = "/v1/chat/completions",
            modelsPath = "/v1/models",
            keyHint = "xai-…",
            keyUrl = "https://console.x.ai",
            verifiedOn = VERIFIED_ON,
        ),
        ProviderCatalogueEntry(
            id = "sambanova",
            displayName = "SambaNova",
            group = ProviderGroup.HOSTED,
            kind = ProviderKind.OPENAI_CHAT,
            baseUrl = "https://api.sambanova.ai",
            pathTemplate = "/v1/chat/completions",
            modelsPath = "/v1/models",
            keyHint = "…",
            keyUrl = "https://cloud.sambanova.ai/apis",
            verifiedOn = VERIFIED_ON,
        ),
        ProviderCatalogueEntry(
            id = "baseten",
            displayName = "Baseten",
            group = ProviderGroup.HOSTED,
            kind = ProviderKind.OPENAI_CHAT,
            baseUrl = "https://inference.baseten.co",
            pathTemplate = "/v1/chat/completions",
            modelsPath = "/v1/models",
            keyHint = "…",
            keyUrl = "https://app.baseten.co/settings/organization/api-keys",
            verifiedOn = VERIFIED_ON,
        ),
        ProviderCatalogueEntry(
            id = "deepinfra",
            displayName = "DeepInfra",
            group = ProviderGroup.HOSTED,
            kind = ProviderKind.OPENAI_CHAT,
            baseUrl = "https://api.deepinfra.com",
            pathTemplate = "/v1/openai/chat/completions",
            modelsPath = "/v1/openai/models",
            keyHint = "…",
            keyUrl = "https://deepinfra.com/dash/api_keys",
            verifiedOn = VERIFIED_ON,
        ),
        ProviderCatalogueEntry(
            id = "cohere",
            displayName = "Cohere",
            group = ProviderGroup.HOSTED,
            kind = ProviderKind.OPENAI_CHAT,
            baseUrl = "https://api.cohere.ai",
            pathTemplate = "/compatibility/v1/chat/completions",
            modelsPath = "/compatibility/v1/models",
            keyHint = "…",
            keyUrl = "https://dashboard.cohere.com/api-keys",
            verifiedOn = VERIFIED_ON,
        ),
        ProviderCatalogueEntry(
            id = "huggingface",
            displayName = "Hugging Face Router",
            group = ProviderGroup.HOSTED,
            kind = ProviderKind.OPENAI_CHAT,
            baseUrl = "https://router.huggingface.co",
            pathTemplate = "/v1/chat/completions",
            modelsPath = "/v1/models",
            keyHint = "hf_…",
            keyUrl = "https://huggingface.co/settings/tokens",
            verifiedOn = VERIFIED_ON,
        ),

        // --- Self-hosted. Usually plain http on the user's own network ------
        ProviderCatalogueEntry(
            id = "lmstudio",
            displayName = "LM Studio",
            group = ProviderGroup.SELF_HOSTED,
            kind = ProviderKind.OPENAI_CHAT,
            baseUrl = "http://127.0.0.1:1234",
            pathTemplate = "/v1/chat/completions",
            modelsPath = "/v1/models",
            keyHint = "meist leer",
            verifiedOn = VERIFIED_ON,
        ),
        ProviderCatalogueEntry(
            id = "llamacpp",
            displayName = "llama.cpp Server",
            group = ProviderGroup.SELF_HOSTED,
            kind = ProviderKind.OPENAI_CHAT,
            baseUrl = "http://127.0.0.1:8080",
            pathTemplate = "/v1/chat/completions",
            modelsPath = "/v1/models",
            keyHint = "meist leer",
            verifiedOn = VERIFIED_ON,
        ),
        ProviderCatalogueEntry(
            id = "vllm",
            displayName = "vLLM",
            group = ProviderGroup.SELF_HOSTED,
            kind = ProviderKind.OPENAI_CHAT,
            baseUrl = "http://127.0.0.1:8000",
            pathTemplate = "/v1/chat/completions",
            modelsPath = "/v1/models",
            keyHint = "meist leer",
            verifiedOn = VERIFIED_ON,
        ),
        ProviderCatalogueEntry(
            id = "ollama",
            displayName = "Ollama",
            group = ProviderGroup.SELF_HOSTED,
            kind = ProviderKind.OPENAI_CHAT,
            baseUrl = "http://127.0.0.1:11434",
            pathTemplate = "/v1/chat/completions",
            modelsPath = "/v1/models",
            keyHint = "meist leer",
            verifiedOn = VERIFIED_ON,
        ),
        ProviderCatalogueEntry(
            id = "jan",
            displayName = "Jan",
            group = ProviderGroup.SELF_HOSTED,
            kind = ProviderKind.OPENAI_CHAT,
            baseUrl = "http://127.0.0.1:1337",
            pathTemplate = "/v1/chat/completions",
            modelsPath = "/v1/models",
            keyHint = "meist leer",
            verifiedOn = VERIFIED_ON,
        ),
    )

    public fun byId(id: String): ProviderCatalogueEntry? = entries.firstOrNull { it.id == id }

    /** The list as the picker shows it: groups in order, entries alphabetical inside. */
    public fun grouped(): List<Pair<ProviderGroup, List<ProviderCatalogueEntry>>> =
        ProviderGroup.entries.map { group -> group to entries.filter { it.group == group }.sortedBy { it.displayName } }
            .filter { it.second.isNotEmpty() }
}
