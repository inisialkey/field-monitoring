package com.inisialkey.fieldmonitoring.evaluation.data

import com.inisialkey.fieldmonitoring.core.network.ApiService
import com.inisialkey.fieldmonitoring.core.network.EvaluationRequest
import com.inisialkey.fieldmonitoring.report.data.toDomain
import com.inisialkey.fieldmonitoring.report.domain.Category
import com.inisialkey.fieldmonitoring.report.domain.Evaluation
import com.inisialkey.fieldmonitoring.report.domain.EvaluationResult
import com.inisialkey.fieldmonitoring.report.domain.Report
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

data class QueuePage(
    val reports: List<Report>,
    val nextCursor: String?,
)

@Singleton
class EvaluationRepository @Inject constructor(private val api: ApiService) {

    suspend fun queue(cursor: String? = null, category: Category? = null): QueuePage =
        api.evaluationQueue(cursor, category).let { page -> QueuePage(page.items.map { it.toDomain() }, page.nextCursor) }

    suspend fun report(id: String): Report = api.report(id).toDomain()

    suspend fun evaluate(reportId: String, result: EvaluationResult, notes: String): Evaluation =
        api.evaluate(reportId, EvaluationRequest(result, notes.trim().ifEmpty { null })).let {
            Evaluation(it.result, it.notes, it.evaluatorName, Instant.parse(it.createdAt))
        }
}
