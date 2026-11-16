package com.inisialkey.fieldmonitoring.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class SyncRulesTest {

    @Test
    fun `network errors retry and give up after five attempts`() {
        assertEquals(SyncDecision.Retry(1), SyncRules.decide(SyncFailure.Network, attempts = 0))
        assertEquals(SyncDecision.Retry(4), SyncRules.decide(SyncFailure.Network, attempts = 3))
        assertEquals(SyncDecision.GiveUp, SyncRules.decide(SyncFailure.Network, attempts = 4))
    }

    @Test
    fun `server errors timeouts and rate limits retry`() {
        listOf(408, 429, 500, 503).forEach { status ->
            assertEquals(SyncDecision.Retry(1), SyncRules.decide(SyncFailure.Http(status), attempts = 0))
        }
    }

    @Test
    fun `rejected input returns the report to draft with the field`() {
        assertEquals(
            SyncDecision.ReturnToDraft("incidentAt"),
            SyncRules.decide(SyncFailure.Http(422, "VALIDATION_ERROR", "incidentAt"), attempts = 2),
        )
        assertEquals(SyncDecision.ReturnToDraft(null), SyncRules.decide(SyncFailure.Http(400, "BAD_REQUEST"), attempts = 0))
    }

    @Test
    fun `forbidden needs attention and is not retried`() {
        assertEquals(
            SyncDecision.NeedsAttention(AttentionReason.OWNER_MISMATCH),
            SyncRules.decide(SyncFailure.Http(403, "OWNER_MISMATCH"), attempts = 0),
        )
        assertEquals(
            SyncDecision.NeedsAttention(AttentionReason.FORBIDDEN),
            SyncRules.decide(SyncFailure.Http(403, "FORBIDDEN"), attempts = 0),
        )
    }

    @Test
    fun `expired or switched session pauses sync`() {
        assertEquals(SyncDecision.Pause, SyncRules.decide(SyncFailure.Http(401, "UNAUTHORIZED"), attempts = 3))
        assertEquals(SyncDecision.Pause, SyncRules.decide(SyncFailure.SessionUnavailable, attempts = 0))
    }

    @Test
    fun `missing photo needs attention`() {
        assertEquals(
            SyncDecision.NeedsAttention(AttentionReason.PHOTO_MISSING),
            SyncRules.decide(SyncFailure.PhotoMissing, attempts = 0),
        )
    }

    @Test
    fun `other client errors need attention with the server code`() {
        assertEquals(
            SyncDecision.NeedsAttention(AttentionReason.REJECTED, "FILE_TOO_LARGE"),
            SyncRules.decide(SyncFailure.Http(413, "FILE_TOO_LARGE"), attempts = 0),
        )
        assertEquals(
            SyncDecision.NeedsAttention(AttentionReason.REJECTED, "404"),
            SyncRules.decide(SyncFailure.Http(404), attempts = 0),
        )
    }
}
