package com.inisialkey.fieldmonitoring.sync

sealed interface SyncFailure {
    data object Network : SyncFailure
    data object SessionUnavailable : SyncFailure
    data object PhotoMissing : SyncFailure
    data class Http(val status: Int, val code: String? = null, val field: String? = null) : SyncFailure
}

enum class AttentionReason { OWNER_MISMATCH, FORBIDDEN, PHOTO_MISSING, REJECTED }

sealed interface SyncDecision {
    data class Retry(val attempts: Int) : SyncDecision
    data object GiveUp : SyncDecision
    data object Pause : SyncDecision
    data class ReturnToDraft(val field: String?) : SyncDecision
    data class NeedsAttention(val reason: AttentionReason, val code: String? = null) : SyncDecision
}

object SyncRules {
    const val MAX_ATTEMPTS = 5

    fun decide(failure: SyncFailure, attempts: Int): SyncDecision = when (failure) {
        SyncFailure.Network -> retryOrGiveUp(attempts)
        SyncFailure.SessionUnavailable -> SyncDecision.Pause
        SyncFailure.PhotoMissing -> SyncDecision.NeedsAttention(AttentionReason.PHOTO_MISSING)
        is SyncFailure.Http -> when (failure.status) {
            401 -> SyncDecision.Pause
            400, 422 -> SyncDecision.ReturnToDraft(failure.field)
            403 -> SyncDecision.NeedsAttention(
                if (failure.code == "OWNER_MISMATCH") AttentionReason.OWNER_MISMATCH else AttentionReason.FORBIDDEN,
            )
            408, 429 -> retryOrGiveUp(attempts)
            in 500..599 -> retryOrGiveUp(attempts)
            else -> SyncDecision.NeedsAttention(AttentionReason.REJECTED, failure.code ?: failure.status.toString())
        }
    }

    private fun retryOrGiveUp(attempts: Int): SyncDecision =
        if (attempts + 1 >= MAX_ATTEMPTS) SyncDecision.GiveUp else SyncDecision.Retry(attempts + 1)
}
