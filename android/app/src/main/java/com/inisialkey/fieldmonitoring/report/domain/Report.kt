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
    val placeName: String? = null,
)

data class Photo(
    val id: String,
    val localPath: String?,
    val uploaded: Boolean,
)

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

    val canDelete: Boolean
        get() = syncState in ReportRules.DELETABLE_STATES

    fun validate(now: Instant): Map<ReportField, FieldError> = buildMap {
        when {
            photos.isEmpty() -> put(ReportField.PHOTOS, FieldError.REQUIRED)
            photos.size > ReportRules.MAX_PHOTOS -> put(ReportField.PHOTOS, FieldError.TOO_MANY)
        }
        when {
            location == null && locationNote.isBlank() -> put(ReportField.LOCATION_NOTE, FieldError.REQUIRED)
            locationNote.trim().length > ReportRules.LOCATION_NOTE_MAX -> put(ReportField.LOCATION_NOTE, FieldError.TOO_LONG)
        }
        if (category == null) put(ReportField.CATEGORY, FieldError.REQUIRED)
        textError(title, ReportRules.TITLE_MAX)?.let { put(ReportField.TITLE, it) }
        textError(description, ReportRules.DESCRIPTION_MAX)?.let { put(ReportField.DESCRIPTION, it) }
        if (incidentAt.isAfter(now.plus(ReportRules.FUTURE_TOLERANCE))) put(ReportField.INCIDENT_TIME, FieldError.IN_FUTURE)
    }

    fun submit(now: Instant): Report? =
        if (syncState == SyncState.DRAFT && validate(now).isEmpty()) copy(syncState = SyncState.PENDING_SYNC, lastError = null) else null

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

private fun textError(value: String, max: Int): FieldError? = when {
    value.isBlank() -> FieldError.REQUIRED
    value.trim().length > max -> FieldError.TOO_LONG
    else -> null
}
