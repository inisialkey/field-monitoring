package com.inisialkey.fieldmonitoring.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.inisialkey.fieldmonitoring.R
import com.inisialkey.fieldmonitoring.auth.data.SessionStore
import com.inisialkey.fieldmonitoring.core.network.ApiService
import com.inisialkey.fieldmonitoring.core.network.CreateReportRequest
import com.inisialkey.fieldmonitoring.core.network.FinalizeRequest
import com.inisialkey.fieldmonitoring.core.network.LocationDto
import com.inisialkey.fieldmonitoring.core.network.OwnerMismatchException
import com.inisialkey.fieldmonitoring.core.network.ReportDto
import com.inisialkey.fieldmonitoring.core.network.ReportOwner
import com.inisialkey.fieldmonitoring.core.network.apiError
import com.inisialkey.fieldmonitoring.report.data.AttachmentEntity
import com.inisialkey.fieldmonitoring.report.data.ReportDao
import com.inisialkey.fieldmonitoring.report.data.ReportEntity
import com.inisialkey.fieldmonitoring.report.data.ReportRepository
import com.inisialkey.fieldmonitoring.report.data.ReportWithAttachments
import com.inisialkey.fieldmonitoring.report.domain.SyncState
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.io.IOException
import java.time.Instant
import kotlinx.serialization.SerializationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val sessionStore: SessionStore,
    private val dao: ReportDao,
    private val reports: ReportRepository,
    private val api: ApiService,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ownerId = inputData.getString(KEY_OWNER_ID) ?: return Result.failure()
        sessionStore.awaitLoaded()
        if (!canSyncFor(ownerId)) return Result.success()
        dao.resetInterrupted(ownerId)
        while (canSyncFor(ownerId)) {
            val item = dao.nextPending(ownerId) ?: break
            val failure = send(item) ?: continue
            val decision = SyncRules.decide(failure, item.report.attemptCount)
            apply(item.report, decision)
            when (decision) {
                is SyncDecision.Retry -> return Result.retry()
                SyncDecision.Pause -> return Result.success()
                else -> Unit
            }
        }
        if (canSyncFor(ownerId)) pull(ownerId)
        return Result.success()
    }

    private fun canSyncFor(ownerId: String): Boolean =
        sessionStore.session.value?.let { it.userId == ownerId && !it.expired } == true

    private suspend fun send(item: ReportWithAttachments): SyncFailure? {
        val report = item.report
        val owner = ReportOwner(report.ownerId)
        val attachments = item.attachments.sortedBy { it.addedAt }
        if (attachments.any { !it.uploaded && !it.hasLocalFile() }) return SyncFailure.PhotoMissing
        dao.updateSyncState(report.id, SyncState.SYNCING, report.attemptCount, null)
        return try {
            api.putReport(report.id, report.toRequest(), owner)
            upload(report.id, attachments.filterNot { it.uploaded }, owner)
            val finalized = finalize(report.id, attachments, owner)
            dao.markSynced(report.id, Instant.parse(finalized.receivedAt).toEpochMilli())
            null
        } catch (e: MissingPhotoException) {
            SyncFailure.PhotoMissing
        } catch (e: HttpException) {
            val error = e.apiError()
            SyncFailure.Http(e.code(), error?.code, error?.details?.firstOrNull()?.get("field"))
        } catch (e: OwnerMismatchException) {
            SyncFailure.SessionUnavailable
        } catch (e: IOException) {
            SyncFailure.Network
        } catch (e: SerializationException) {
            SyncFailure.Network
        }
    }

    private suspend fun finalize(reportId: String, attachments: List<AttachmentEntity>, owner: ReportOwner): ReportDto {
        val request = FinalizeRequest(attachments.map { it.id })
        return try {
            api.finalize(reportId, request, owner)
        } catch (e: HttpException) {
            val error = e.apiError()
            if (e.code() != 409 || error == null || error.code != "ATTACHMENTS_INCOMPLETE") throw e
            val missing = error.details.mapNotNull { it["attachmentId"] }.toSet()
            dao.setUploaded(missing.toList(), false)
            upload(reportId, attachments.filter { it.id in missing }, owner)
            api.finalize(reportId, request, owner)
        }
    }

    private suspend fun upload(reportId: String, attachments: List<AttachmentEntity>, owner: ReportOwner) {
        for (attachment in attachments) {
            if (!attachment.hasLocalFile()) throw MissingPhotoException()
            val file = File(checkNotNull(attachment.localPath))
            val part = MultipartBody.Part.createFormData("file", file.name, file.asRequestBody(JPEG))
            api.putAttachment(reportId, attachment.id, part, owner)
            dao.setUploaded(listOf(attachment.id), true)
        }
    }

    private suspend fun apply(report: ReportEntity, decision: SyncDecision) {
        when (decision) {
            is SyncDecision.Retry -> dao.updateSyncState(report.id, SyncState.PENDING_SYNC, decision.attempts, null)
            SyncDecision.GiveUp -> dao.updateSyncState(report.id, SyncState.FAILED, SyncRules.MAX_ATTEMPTS, null)
            SyncDecision.Pause -> dao.updateSyncState(report.id, SyncState.PENDING_SYNC, report.attemptCount, null)
            is SyncDecision.ReturnToDraft -> dao.updateSyncState(report.id, SyncState.DRAFT, 0, draftMessage(decision.field))
            is SyncDecision.NeedsAttention ->
                dao.updateSyncState(report.id, SyncState.NEEDS_ATTENTION, report.attemptCount, attentionMessage(decision))
        }
    }

    private suspend fun pull(ownerId: String) {
        val serverReports = try {
            api.myReports(ReportOwner(ownerId))
        } catch (e: IOException) {
            return
        } catch (e: HttpException) {
            return
        } catch (e: SerializationException) {
            return
        }
        reports.mergeServerReports(ownerId, serverReports)
    }

    private fun draftMessage(field: String?): String {
        val label = when (field) {
            "title" -> R.string.field_label_title
            "category" -> R.string.field_label_category
            "description" -> R.string.field_label_description
            "incidentAt" -> R.string.field_label_incident_time
            "location" -> R.string.field_label_location
            "locationNote" -> R.string.field_label_location_note
            else -> return applicationContext.getString(R.string.sync_error_validation_generic)
        }
        return applicationContext.getString(R.string.sync_error_validation, applicationContext.getString(label))
    }

    private fun attentionMessage(decision: SyncDecision.NeedsAttention): String = when (decision.reason) {
        AttentionReason.OWNER_MISMATCH -> applicationContext.getString(R.string.sync_error_owner)
        AttentionReason.FORBIDDEN -> applicationContext.getString(R.string.sync_error_forbidden)
        AttentionReason.PHOTO_MISSING -> applicationContext.getString(R.string.sync_error_photo_missing)
        AttentionReason.REJECTED -> applicationContext.getString(R.string.sync_error_rejected, decision.code.orEmpty())
    }

    private class MissingPhotoException : Exception()

    companion object {
        const val KEY_OWNER_ID = "owner_id"
        private val JPEG = "image/jpeg".toMediaType()
    }
}

private fun AttachmentEntity.hasLocalFile(): Boolean = localPath?.let { File(it).isFile } == true

private fun ReportEntity.toRequest() = CreateReportRequest(
    ownerUserId = ownerId,
    title = title.trim(),
    category = requireNotNull(category),
    description = description.trim(),
    incidentAt = Instant.ofEpochMilli(incidentAt).toString(),
    incidentOffsetMinutes = incidentOffsetMinutes,
    location = if (latitude != null && longitude != null && accuracyM != null && locationCapturedAt != null) {
        LocationDto(latitude, longitude, accuracyM.toDouble(), Instant.ofEpochMilli(locationCapturedAt).toString())
    } else {
        null
    },
    locationNote = locationNote.trim().ifEmpty { null },
    createdAt = Instant.ofEpochMilli(createdAt).toString(),
)
