package com.inisialkey.fieldmonitoring.evaluation.domain

import com.inisialkey.fieldmonitoring.report.domain.EvaluationResult
import com.inisialkey.fieldmonitoring.report.domain.FieldError

enum class EvaluationField { RESULT, NOTES }

object EvaluationRules {
    const val NOTES_MAX = 1000

    fun validate(result: EvaluationResult?, notes: String): Map<EvaluationField, FieldError> = buildMap {
        if (result == null) put(EvaluationField.RESULT, FieldError.REQUIRED)
        when {
            notes.trim().length > NOTES_MAX -> put(EvaluationField.NOTES, FieldError.TOO_LONG)
            result == EvaluationResult.INVALID && notes.isBlank() -> put(EvaluationField.NOTES, FieldError.REQUIRED)
        }
    }
}
