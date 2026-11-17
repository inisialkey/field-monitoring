package com.inisialkey.fieldmonitoring.report.domain

import java.time.Instant

enum class Category {
    FOREST_REHABILITATION,
    MANGROVE,
    PEATLAND_RESTORATION,
    SOCIAL_FORESTRY,
    RENEWABLE_ENERGY,
    WASTE_MANAGEMENT,
    BIODIVERSITY,
    OTHER,
}

enum class SyncState { DRAFT, PENDING_SYNC, SYNCING, SYNCED, FAILED, NEEDS_ATTENTION }

enum class EvaluationResult { VALID, INVALID }

data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
    val accuracyM: Float,
    val capturedAt: Instant,
)

data class Photo(val id: String, val localPath: String?, val uploaded: Boolean)

data class Evaluation(
    val result: EvaluationResult,
    val notes: String?,
    val evaluatorName: String,
    val evaluatedAt: Instant,
)

data class Report(
    val id: String,
    val ownerId: String,
    val title: String,
    val category: Category?,
    val description: String,
    val incidentAt: Instant,
    val incidentOffsetMinutes: Int,
    val location: GeoPoint?,
    val locationNote: String,
    val syncState: SyncState,
    val lastError: String?,
    val createdAt: Instant,
    val receivedAt: Instant?,
    val photos: List<Photo>,
    val evaluation: Evaluation?,
    val reporterName: String? = null,
) {
    val isUnsent: Boolean
        get() = syncState in setOf(SyncState.PENDING_SYNC, SyncState.SYNCING, SyncState.FAILED, SyncState.NEEDS_ATTENTION)

    companion object {
        fun newDraft(id: String, ownerId: String, now: Instant, offsetMinutes: Int) = Report(
            id = id,
            ownerId = ownerId,
            title = "",
            category = null,
            description = "",
            incidentAt = now,
            incidentOffsetMinutes = offsetMinutes,
            location = null,
            locationNote = "",
            syncState = SyncState.DRAFT,
            lastError = null,
            createdAt = now,
            receivedAt = null,
            photos = emptyList(),
            evaluation = null,
        )
    }
}
