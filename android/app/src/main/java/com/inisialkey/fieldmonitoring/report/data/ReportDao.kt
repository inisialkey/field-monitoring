package com.inisialkey.fieldmonitoring.report.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.inisialkey.fieldmonitoring.report.domain.EvaluationResult
import com.inisialkey.fieldmonitoring.report.domain.SyncState
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {
    @Transaction
    @Query("SELECT * FROM reports WHERE owner_id = :ownerId ORDER BY created_at DESC")
    fun observeAll(ownerId: String): Flow<List<ReportWithAttachments>>

    @Transaction
    @Query("SELECT * FROM reports WHERE id = :id AND owner_id = :ownerId")
    fun observe(id: String, ownerId: String): Flow<ReportWithAttachments?>

    @Transaction
    @Query("SELECT * FROM reports WHERE id = :id AND owner_id = :ownerId")
    suspend fun get(id: String, ownerId: String): ReportWithAttachments?

    @Transaction
    @Query("SELECT * FROM reports WHERE owner_id = :ownerId AND sync_state = 'PENDING_SYNC' ORDER BY created_at LIMIT 1")
    suspend fun nextPending(ownerId: String): ReportWithAttachments?

    @Query("SELECT sync_state FROM reports WHERE id = :id AND owner_id = :ownerId")
    suspend fun syncState(id: String, ownerId: String): SyncState?

    @Query("SELECT owner_id FROM reports WHERE id = :id")
    suspend fun ownerOf(id: String): String?

    @Upsert
    suspend fun upsert(report: ReportEntity)

    @Upsert
    suspend fun upsertAttachments(attachments: List<AttachmentEntity>)

    @Query(
        "DELETE FROM attachments WHERE id = :id AND owner_id = :ownerId " +
            "AND report_id IN (SELECT id FROM reports WHERE sync_state = 'DRAFT')",
    )
    suspend fun deleteDraftAttachment(id: String, ownerId: String): Int

    @Query("UPDATE reports SET sync_state = :state, attempt_count = :attempts, last_error = :error WHERE id = :id")
    suspend fun updateSyncState(id: String, state: SyncState, attempts: Int, error: String?)

    @Query("UPDATE reports SET sync_state = 'SYNCED', attempt_count = 0, last_error = NULL, received_at = :receivedAt WHERE id = :id")
    suspend fun markSynced(id: String, receivedAt: Long)

    @Query("UPDATE attachments SET uploaded = :uploaded WHERE id IN (:ids)")
    suspend fun setUploaded(ids: List<String>, uploaded: Boolean)

    @Query("UPDATE reports SET sync_state = 'PENDING_SYNC' WHERE owner_id = :ownerId AND sync_state = 'SYNCING'")
    suspend fun resetInterrupted(ownerId: String)

    @Query(
        "UPDATE reports SET sync_state = 'PENDING_SYNC', attempt_count = 0, last_error = NULL " +
            "WHERE owner_id = :ownerId AND sync_state = 'FAILED'",
    )
    suspend fun retryFailed(ownerId: String): Int

    @Query(
        "UPDATE reports SET evaluation_result = :result, evaluation_notes = :notes, evaluator_name = :evaluatorName, " +
            "evaluated_at = :evaluatedAt WHERE id = :id AND sync_state = 'SYNCED'",
    )
    suspend fun updateEvaluation(id: String, result: EvaluationResult?, notes: String?, evaluatorName: String?, evaluatedAt: Long?)

    @Query("SELECT sync_state AS state, COUNT(*) AS count FROM reports WHERE owner_id = :ownerId GROUP BY sync_state")
    suspend fun countByState(ownerId: String): List<StateCount>

    @Query(
        "SELECT local_path FROM attachments WHERE local_path IS NOT NULL AND report_id IN " +
            "(SELECT id FROM reports WHERE owner_id = :ownerId AND sync_state = 'SYNCED')",
    )
    suspend fun syncedPhotoPaths(ownerId: String): List<String>

    @Query("DELETE FROM reports WHERE owner_id = :ownerId AND sync_state = 'SYNCED'")
    suspend fun deleteSynced(ownerId: String)

    @Transaction
    suspend fun saveDraft(report: ReportEntity): Boolean {
        val owner = ownerOf(report.id)
        if (owner != null && (owner != report.ownerId || syncState(report.id, owner) != SyncState.DRAFT)) return false
        upsert(report.copy(syncState = SyncState.DRAFT, attemptCount = 0))
        return true
    }

    @Transaction
    suspend fun saveAndSubmit(report: ReportEntity): Boolean {
        if (!saveDraft(report)) return false
        updateSyncState(report.id, SyncState.PENDING_SYNC, 0, null)
        return true
    }

    @Transaction
    suspend fun mergeServerReport(report: ReportEntity, attachments: List<AttachmentEntity>) {
        when (ownerOf(report.id)) {
            null -> {
                upsert(report)
                upsertAttachments(attachments)
            }
            report.ownerId -> if (syncState(report.id, report.ownerId) == SyncState.SYNCED) {
                updateEvaluation(report.id, report.evaluationResult, report.evaluationNotes, report.evaluatorName, report.evaluatedAt)
            }
        }
    }
}
