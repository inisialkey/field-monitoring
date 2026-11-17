package com.inisialkey.fieldmonitoring.report.domain

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportRulesTest {

    private val now = Instant.parse("2026-11-16T02:00:00Z")

    private val complete = Report.newDraft("report-1", "user-1", now, 420).copy(
        title = "Jalan berlubang depan SD Negeri 3",
        category = Category.ROAD,
        description = "Lubang selebar satu meter di lajur kiri.",
        location = GeoPoint(-6.26152, 107.15283, 8f, now),
        photos = listOf(Photo("photo-1", "/data/photo-1.jpg", uploaded = false)),
    )

    @Test
    fun `complete report passes`() {
        assertTrue(ReportRules.validate(complete, now).isEmpty())
    }

    @Test
    fun `report needs one to five photos`() {
        assertEquals(FieldError.REQUIRED, ReportRules.validate(complete.copy(photos = emptyList()), now)[ReportField.PHOTOS])
        val sixPhotos = List(6) { Photo("photo-$it", "/data/photo-$it.jpg", uploaded = false) }
        assertEquals(FieldError.TOO_MANY, ReportRules.validate(complete.copy(photos = sixPhotos), now)[ReportField.PHOTOS])
    }

    @Test
    fun `location note is required only without coordinates`() {
        val withoutGps = complete.copy(location = null)
        assertEquals(FieldError.REQUIRED, ReportRules.validate(withoutGps, now)[ReportField.LOCATION_NOTE])
        assertTrue(ReportRules.validate(withoutGps.copy(locationNote = "Depan Pasar Baru"), now).isEmpty())
    }

    @Test
    fun `blank text fields and missing category are required`() {
        val errors = ReportRules.validate(complete.copy(title = "  ", description = "", category = null), now)
        assertEquals(FieldError.REQUIRED, errors[ReportField.TITLE])
        assertEquals(FieldError.REQUIRED, errors[ReportField.DESCRIPTION])
        assertEquals(FieldError.REQUIRED, errors[ReportField.CATEGORY])
    }

    @Test
    fun `text longer than the limit is rejected`() {
        val errors = ReportRules.validate(
            complete.copy(title = "a".repeat(101), description = "b".repeat(2001), locationNote = "c".repeat(201)),
            now,
        )
        assertEquals(FieldError.TOO_LONG, errors[ReportField.TITLE])
        assertEquals(FieldError.TOO_LONG, errors[ReportField.DESCRIPTION])
        assertEquals(FieldError.TOO_LONG, errors[ReportField.LOCATION_NOTE])
    }

    @Test
    fun `incident time may be at most ten minutes ahead`() {
        assertTrue(ReportRules.validate(complete.copy(incidentAt = now.plus(Duration.ofMinutes(10))), now).isEmpty())
        val errors = ReportRules.validate(complete.copy(incidentAt = now.plus(Duration.ofMinutes(11))), now)
        assertEquals(FieldError.IN_FUTURE, errors[ReportField.INCIDENT_TIME])
    }

    @Test
    fun `only reports the server has not accepted can be deleted`() {
        val deletable = SyncState.entries.filter { ReportRules.canDelete(complete.copy(syncState = it)) }
        assertEquals(listOf(SyncState.DRAFT, SyncState.FAILED, SyncState.NEEDS_ATTENTION), deletable)
    }

    @Test
    fun `errors follow the form order`() {
        val empty = Report.newDraft("report-2", "user-1", now, 420)
        assertEquals(
            listOf(ReportField.PHOTOS, ReportField.LOCATION_NOTE, ReportField.CATEGORY, ReportField.TITLE, ReportField.DESCRIPTION),
            ReportRules.validate(empty, now).keys.toList(),
        )
    }
}
