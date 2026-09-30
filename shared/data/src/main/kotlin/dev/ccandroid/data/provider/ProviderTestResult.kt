package dev.ccandroid.data.provider

import kotlinx.serialization.Serializable

/**
 * The outcome of one step of the connection test.
 *
 * Every step is reported separately, and none of them collapses into a single
 * "connection failed". A provider that is reachable but has the wrong model is
 * the case where one boolean is least useful, and it is a single "failed" that
 * every other tool in this space produces.
 */
@Serializable
public sealed interface StepOutcome {

    /** The step ran and worked. [detailDe] is what the user reads, e.g. "340 ms". */
    @Serializable
    public data class Passed(val detailDe: String) : StepOutcome

    /** The step ran and did not work. The message says what to do about it. */
    @Serializable
    public data class Failed(
        val reasonDe: String,
        val reasonEn: String,
        val fixDe: String,
        val fixEn: String,
    ) : StepOutcome

    /**
     * The step was not applicable. Not a failure and not a pass: a server with
     * no model endpoint is a perfectly good server.
     */
    @Serializable
    public data class Skipped(val reasonDe: String) : StepOutcome

    /**
     * The server does not offer this at all. Reported as "nicht unterstützt",
     * never as a failure, and the app offers the manual route instead.
     */
    @Serializable
    public data class Unsupported(val reasonDe: String) : StepOutcome
}

/** The eight steps, in the order they run. */
@Serializable
public enum class TestStep {
    URL,
    REACHABILITY,
    TLS,
    AUTHENTICATION,
    MODEL_DISCOVERY,
    MINIMAL_COMPLETION,
    VISION,
    STREAMING,
}

/** One step and what it found. */
@Serializable
public data class StepResult(
    val step: TestStep,
    val outcome: StepOutcome,
    /** The provider's own value, kept so the user can quote it to the provider. */
    val rawDetail: String? = null,
)

/**
 * The whole test.
 *
 * [verdict] is never a bare boolean. An untested provider is
 * [ProviderVerdict.UNTESTED], which is a different state from a failed one, and
 * neither is a green check: the user has to be able to tell "we did not try"
 * from "we tried and it does not work".
 */
@Serializable
public data class ProviderTestResult(
    val verdict: ProviderVerdict,
    val steps: List<StepResult>,
    val discoveredModels: List<String> = emptyList(),
    val durationMs: Long = 0,
) {
    public fun step(step: TestStep): StepResult? = steps.firstOrNull { it.step == step }
}

@Serializable
public enum class ProviderVerdict {
    /** Every step that could run, ran, and worked. */
    OK,

    /** Something works and something does not. The steps say which. */
    PARTIAL,

    /** Nothing usable. The first failing step says why. */
    FAILED,

    /**
     * Not tested. Shown as a neutral state, never a check. A provider the app
     * has never spoken to is not a provider that works.
     */
    UNTESTED,
}
