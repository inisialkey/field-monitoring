package com.inisialkey.fieldmonitoring.evaluation.domain

import com.inisialkey.fieldmonitoring.report.domain.EvaluationResult
import com.inisialkey.fieldmonitoring.report.domain.FieldError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EvaluationRulesTest {

    @Test
    fun `a result must be chosen`() {
        assertEquals(FieldError.REQUIRED, EvaluationRules.validate(null, "")[EvaluationField.RESULT])
    }

    @Test
    fun `notes are optional for valid and required for invalid`() {
        assertTrue(EvaluationRules.validate(EvaluationResult.VALID, "").isEmpty())
        assertEquals(FieldError.REQUIRED, EvaluationRules.validate(EvaluationResult.INVALID, " ")[EvaluationField.NOTES])
        assertTrue(EvaluationRules.validate(EvaluationResult.INVALID, "Foto tidak sesuai lokasi").isEmpty())
    }

    @Test
    fun `notes longer than 1000 characters are rejected`() {
        assertEquals(FieldError.TOO_LONG, EvaluationRules.validate(EvaluationResult.VALID, "a".repeat(1001))[EvaluationField.NOTES])
    }
}
