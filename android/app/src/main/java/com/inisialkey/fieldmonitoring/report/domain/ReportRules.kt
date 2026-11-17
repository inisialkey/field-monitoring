package com.inisialkey.fieldmonitoring.report.domain

import java.time.Duration

enum class ReportField { PHOTOS, LOCATION_NOTE, CATEGORY, TITLE, DESCRIPTION, INCIDENT_TIME }

enum class FieldError { REQUIRED, TOO_LONG, TOO_MANY, IN_FUTURE }

object ReportRules {
    const val TITLE_MAX = 100
    const val DESCRIPTION_MAX = 2000
    const val LOCATION_NOTE_MAX = 200
    const val MAX_PHOTOS = 5
    val FUTURE_TOLERANCE: Duration = Duration.ofMinutes(10)
    val DELETABLE_STATES = setOf(SyncState.DRAFT, SyncState.FAILED, SyncState.NEEDS_ATTENTION)
}
