package com.inisialkey.fieldmonitoring.report.domain

import java.time.Duration
import java.time.Instant

enum class ReportField { PHOTOS, LOCATION_NOTE, CATEGORY, TITLE, DESCRIPTION, INCIDENT_TIME }

enum class FieldError { REQUIRED, TOO_LONG, TOO_MANY, IN_FUTURE }

object ReportRules {
    const val TITLE_MAX = 100
    const val DESCRIPTION_MAX = 2000
    const val LOCATION_NOTE_MAX = 200
    const val MAX_PHOTOS = 5
    private val FUTURE_TOLERANCE: Duration = Duration.ofMinutes(10)

    fun validate(report: Report, now: Instant): Map<ReportField, FieldError> = buildMap {
        when {
            report.photos.isEmpty() -> put(ReportField.PHOTOS, FieldError.REQUIRED)
            report.photos.size > MAX_PHOTOS -> put(ReportField.PHOTOS, FieldError.TOO_MANY)
        }
        when {
            report.location == null && report.locationNote.isBlank() -> put(ReportField.LOCATION_NOTE, FieldError.REQUIRED)
            report.locationNote.trim().length > LOCATION_NOTE_MAX -> put(ReportField.LOCATION_NOTE, FieldError.TOO_LONG)
        }
        if (report.category == null) put(ReportField.CATEGORY, FieldError.REQUIRED)
        textError(report.title, TITLE_MAX)?.let { put(ReportField.TITLE, it) }
        textError(report.description, DESCRIPTION_MAX)?.let { put(ReportField.DESCRIPTION, it) }
        if (report.incidentAt.isAfter(now.plus(FUTURE_TOLERANCE))) put(ReportField.INCIDENT_TIME, FieldError.IN_FUTURE)
    }

    private fun textError(value: String, max: Int): FieldError? = when {
        value.isBlank() -> FieldError.REQUIRED
        value.trim().length > max -> FieldError.TOO_LONG
        else -> null
    }
}
