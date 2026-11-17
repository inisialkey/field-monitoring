package com.inisialkey.fieldmonitoring.report.domain

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportTest {

    private val now = Instant.parse("2026-11-16T02:00:00Z")

    private val complete = Report.newDraft("report-1", "user-1", now, 420).copy(
        title = "Penanaman mangrove blok B",
        category = Category.MANGROVE,
        description = "Sebanyak 500 bibit tertanam, 20 bibit mati terbawa arus.",
        location = GeoPoint(-6.26152, 107.15283, 8f, now),
        photos = listOf(Photo("photo-1", "/data/photo-1.jpg", uploaded = false)),
    )

    @Test
    fun `complete report passes`() {
        assertTrue(complete.validate(now).isEmpty())
    }

    @Test
    fun `report needs one to five photos`() {
        assertEquals(FieldError.REQUIRED, complete.copy(photos = emptyList()).validate(now)[ReportField.PHOTOS])
        val sixPhotos = List(6) { Photo("photo-$it", "/data/photo-$it.jpg", uploaded = false) }
        assertEquals(FieldError.TOO_MANY, complete.copy(photos = sixPhotos).validate(now)[ReportField.PHOTOS])
    }

    @Test
    fun `location note is required only without coordinates`() {
        val withoutGps = complete.copy(location = null)
        assertEquals(FieldError.REQUIRED, withoutGps.validate(now)[ReportField.LOCATION_NOTE])
        assertTrue(withoutGps.copy(locationNote = "Petak 3, Desa Pantai Bahagia").validate(now).isEmpty())
    }

    @Test
    fun `blank text fields and missing category are required`() {
        val errors = complete.copy(title = "  ", description = "", category = null).validate(now)
        assertEquals(FieldError.REQUIRED, errors[ReportField.TITLE])
        assertEquals(FieldError.REQUIRED, errors[ReportField.DESCRIPTION])
        assertEquals(FieldError.REQUIRED, errors[ReportField.CATEGORY])
    }

    @Test
    fun `text longer than the limit is rejected`() {
        val errors = complete
            .copy(title = "a".repeat(101), description = "b".repeat(2001), locationNote = "c".repeat(201))
            .validate(now)
        assertEquals(FieldError.TOO_LONG, errors[ReportField.TITLE])
        assertEquals(FieldError.TOO_LONG, errors[ReportField.DESCRIPTION])
        assertEquals(FieldError.TOO_LONG, errors[ReportField.LOCATION_NOTE])
    }

    @Test
    fun `incident time may be at most ten minutes ahead`() {
        assertTrue(complete.copy(incidentAt = now.plus(Duration.ofMinutes(10))).validate(now).isEmpty())
        val errors = complete.copy(incidentAt = now.plus(Duration.ofMinutes(11))).validate(now)
        assertEquals(FieldError.IN_FUTURE, errors[ReportField.INCIDENT_TIME])
    }

    @Test
    fun `only reports the server has not accepted can be deleted`() {
        val deletable = SyncState.entries.filter { complete.copy(syncState = it).canDelete }
        assertEquals(listOf(SyncState.DRAFT, SyncState.FAILED, SyncState.NEEDS_ATTENTION), deletable)
    }

    @Test
    fun `errors follow the form order`() {
        val empty = Report.newDraft("report-2", "user-1", now, 420)
        assertEquals(
            listOf(ReportField.PHOTOS, ReportField.LOCATION_NOTE, ReportField.CATEGORY, ReportField.TITLE, ReportField.DESCRIPTION),
            empty.validate(now).keys.toList(),
        )
    }

    @Test
    fun `submitting a complete draft queues it and clears the old error`() {
        val submitted = complete.copy(lastError = "Periksa judul lalu kirim ulang.").submit(now)
        assertEquals(SyncState.PENDING_SYNC, submitted?.syncState)
        assertNull(submitted?.lastError)
    }

    @Test
    fun `an incomplete draft cannot be submitted`() {
        assertNull(complete.copy(title = " ").submit(now))
        assertNull(complete.copy(photos = emptyList()).submit(now))
    }

    @Test
    fun `a report that already left the draft state cannot be submitted again`() {
        (SyncState.entries - SyncState.DRAFT).forEach { assertNull(complete.copy(syncState = it).submit(now)) }
    }
}
