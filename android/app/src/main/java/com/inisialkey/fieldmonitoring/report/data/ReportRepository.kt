package com.inisialkey.fieldmonitoring.report.data

import android.net.Uri
import com.inisialkey.fieldmonitoring.core.network.ReportDto
import com.inisialkey.fieldmonitoring.report.domain.Photo
import com.inisialkey.fieldmonitoring.report.domain.Report
import com.inisialkey.fieldmonitoring.report.domain.ReportRules
import com.inisialkey.fieldmonitoring.report.domain.SyncState
import com.inisialkey.fieldmonitoring.sync.SyncScheduler
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class PendingCounts(
    val unsent: Int,
    val drafts: Int,
)

@Singleton
class ReportRepository @Inject constructor(
    private val dao: ReportDao,
    private val photos: PhotoStore,
    private val syncScheduler: SyncScheduler,
) {
    fun observeReports(ownerId: String): Flow<List<Report>> = dao.observeAll(ownerId).map { reports -> reports.map { it.toDomain() } }

    fun observeReport(id: String, ownerId: String): Flow<Report?> = dao.observe(id, ownerId).map { it?.toDomain() }

    suspend fun getReport(id: String, ownerId: String): Report? = dao.get(id, ownerId)?.toDomain()

    suspend fun saveDraft(report: Report): Boolean = dao.saveDraft(report.toEntity())

    suspend fun addPhoto(report: Report, source: Uri): Photo? {
        if (!dao.saveDraft(report.toEntity())) return null
        val id = UUID.randomUUID().toString()
        val file = photos.import(source, report.ownerId, id)
        dao.upsertAttachments(
            listOf(AttachmentEntity(id, report.id, report.ownerId, file.path, uploaded = false, addedAt = System.currentTimeMillis())),
        )
        return Photo(id, file.path, uploaded = false)
    }

    suspend fun removePhoto(photo: Photo, ownerId: String) {
        if (dao.deleteDraftAttachment(photo.id, ownerId) > 0) photos.delete(listOfNotNull(photo.localPath))
    }

    suspend fun submit(report: Report): Boolean {
        val submitted = report.submit(Instant.now()) ?: return false
        if (!dao.saveAndSubmit(submitted.toEntity())) return false
        syncScheduler.enqueue(report.ownerId)
        return true
    }

    suspend fun deleteUnsent(report: Report): Boolean {
        val paths = dao.localPhotoPaths(report.id)
        if (dao.deleteInStates(report.id, report.ownerId, ReportRules.DELETABLE_STATES) == 0) return false
        photos.delete(paths)
        return true
    }

    suspend fun retryFailed(ownerId: String): Int {
        val count = dao.retryFailed(ownerId)
        if (count > 0) syncScheduler.syncNow(ownerId)
        return count
    }

    suspend fun pendingCounts(ownerId: String): PendingCounts {
        val counts = dao.countByState(ownerId).associate { it.state to it.count }
        return PendingCounts(
            unsent = listOf(SyncState.PENDING_SYNC, SyncState.SYNCING, SyncState.FAILED, SyncState.NEEDS_ATTENTION)
                .sumOf { counts[it] ?: 0 },
            drafts = counts[SyncState.DRAFT] ?: 0,
        )
    }

    suspend fun deleteSynced(ownerId: String) {
        val paths = dao.syncedPhotoPaths(ownerId)
        dao.deleteSynced(ownerId)
        photos.delete(paths)
    }

    suspend fun mergeServerReports(ownerId: String, reports: List<ReportDto>) {
        reports.filter { it.ownerUserId == ownerId && it.finalizedAt != null }.forEach { dto ->
            val report = dto.toDomain()
            val receivedAt = report.receivedAt?.toEpochMilli() ?: System.currentTimeMillis()
            dao.mergeServerReport(
                report.toEntity(),
                report.photos.mapIndexed { index, photo ->
                    AttachmentEntity(photo.id, report.id, ownerId, localPath = null, uploaded = true, addedAt = receivedAt + index)
                },
            )
        }
    }
}
