package com.inisialkey.fieldmonitoring.report.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.inisialkey.fieldmonitoring.core.network.ReportDto
import com.inisialkey.fieldmonitoring.report.domain.Category
import com.inisialkey.fieldmonitoring.report.domain.Evaluation
import com.inisialkey.fieldmonitoring.report.domain.EvaluationResult
import com.inisialkey.fieldmonitoring.report.domain.GeoPoint
import com.inisialkey.fieldmonitoring.report.domain.Photo
import com.inisialkey.fieldmonitoring.report.domain.Report
import com.inisialkey.fieldmonitoring.report.domain.SyncState
import java.time.Instant

@Entity(tableName = "reports", indices = [Index("owner_id", "sync_state")])
data class ReportEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "owner_id") val ownerId: String,
    val title: String,
    val category: Category?,
    val description: String,
    @ColumnInfo(name = "incident_at") val incidentAt: Long,
    @ColumnInfo(name = "incident_offset_minutes") val incidentOffsetMinutes: Int,
    val latitude: Double?,
    val longitude: Double?,
    @ColumnInfo(name = "accuracy_m") val accuracyM: Float?,
    @ColumnInfo(name = "location_captured_at") val locationCapturedAt: Long?,
    @ColumnInfo(name = "location_note") val locationNote: String,
    @ColumnInfo(name = "sync_state") val syncState: SyncState,
    @ColumnInfo(name = "attempt_count") val attemptCount: Int,
    @ColumnInfo(name = "last_error") val lastError: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "received_at") val receivedAt: Long?,
    @ColumnInfo(name = "evaluation_result") val evaluationResult: EvaluationResult?,
    @ColumnInfo(name = "evaluation_notes") val evaluationNotes: String?,
    @ColumnInfo(name = "evaluator_name") val evaluatorName: String?,
    @ColumnInfo(name = "evaluated_at") val evaluatedAt: Long?,
)

@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = ReportEntity::class,
            parentColumns = ["id"],
            childColumns = ["report_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("report_id")],
)
data class AttachmentEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "report_id") val reportId: String,
    @ColumnInfo(name = "owner_id") val ownerId: String,
    @ColumnInfo(name = "local_path") val localPath: String?,
    val uploaded: Boolean,
    @ColumnInfo(name = "added_at") val addedAt: Long,
)

data class ReportWithAttachments(
    @Embedded val report: ReportEntity,
    @Relation(parentColumn = "id", entityColumn = "report_id") val attachments: List<AttachmentEntity>,
)

data class StateCount(val state: SyncState, val count: Int)

fun ReportWithAttachments.toDomain(): Report = with(report) {
    Report(
        id = id,
        ownerId = ownerId,
        title = title,
        category = category,
        description = description,
        incidentAt = Instant.ofEpochMilli(incidentAt),
        incidentOffsetMinutes = incidentOffsetMinutes,
        location = if (latitude != null && longitude != null && accuracyM != null && locationCapturedAt != null) {
            GeoPoint(latitude, longitude, accuracyM, Instant.ofEpochMilli(locationCapturedAt))
        } else {
            null
        },
        locationNote = locationNote,
        syncState = syncState,
        lastError = lastError,
        createdAt = Instant.ofEpochMilli(createdAt),
        receivedAt = receivedAt?.let(Instant::ofEpochMilli),
        photos = attachments.sortedBy { it.addedAt }.map { Photo(it.id, it.localPath, it.uploaded) },
        evaluation = evaluationResult?.let {
            Evaluation(it, evaluationNotes, evaluatorName.orEmpty(), Instant.ofEpochMilli(evaluatedAt ?: 0))
        },
    )
}

fun Report.toEntity(): ReportEntity = ReportEntity(
    id = id,
    ownerId = ownerId,
    title = title,
    category = category,
    description = description,
    incidentAt = incidentAt.toEpochMilli(),
    incidentOffsetMinutes = incidentOffsetMinutes,
    latitude = location?.latitude,
    longitude = location?.longitude,
    accuracyM = location?.accuracyM,
    locationCapturedAt = location?.capturedAt?.toEpochMilli(),
    locationNote = locationNote,
    syncState = syncState,
    attemptCount = 0,
    lastError = lastError,
    createdAt = createdAt.toEpochMilli(),
    receivedAt = receivedAt?.toEpochMilli(),
    evaluationResult = evaluation?.result,
    evaluationNotes = evaluation?.notes,
    evaluatorName = evaluation?.evaluatorName,
    evaluatedAt = evaluation?.evaluatedAt?.toEpochMilli(),
)

fun ReportDto.toDomain(): Report = Report(
    id = id,
    ownerId = ownerUserId,
    title = title,
    category = category,
    description = description,
    incidentAt = Instant.parse(incidentAt),
    incidentOffsetMinutes = incidentOffsetMinutes,
    location = location?.let { GeoPoint(it.latitude, it.longitude, it.accuracyM.toFloat(), Instant.parse(it.capturedAt)) },
    locationNote = locationNote.orEmpty(),
    syncState = SyncState.SYNCED,
    lastError = null,
    createdAt = Instant.parse(createdAt),
    receivedAt = Instant.parse(receivedAt),
    photos = attachments.filter { it.kind == "PHOTO" }.map { Photo(it.id, localPath = null, uploaded = true) },
    evaluation = evaluation?.let { Evaluation(it.result, it.notes, it.evaluatorName, Instant.parse(it.createdAt)) },
    reporterName = reporterName,
)
