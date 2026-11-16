package com.inisialkey.fieldmonitoring.evaluation.ui.evaluate

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.inisialkey.fieldmonitoring.Evaluate
import com.inisialkey.fieldmonitoring.R
import com.inisialkey.fieldmonitoring.core.UserMessage
import com.inisialkey.fieldmonitoring.core.UserMessages
import com.inisialkey.fieldmonitoring.evaluation.data.EvaluationRepository
import com.inisialkey.fieldmonitoring.evaluation.domain.EvaluationField
import com.inisialkey.fieldmonitoring.evaluation.domain.EvaluationRules
import com.inisialkey.fieldmonitoring.evaluation.ui.queue.LoadError
import com.inisialkey.fieldmonitoring.evaluation.ui.queue.toLoadError
import com.inisialkey.fieldmonitoring.report.domain.EvaluationResult
import com.inisialkey.fieldmonitoring.report.domain.FieldError
import com.inisialkey.fieldmonitoring.report.domain.Report
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

sealed interface EvaluateOutcome {
    data class OpenNext(val reportId: String) : EvaluateOutcome
    data object BackToQueue : EvaluateOutcome
}

data class EvaluateUiState(
    val loading: Boolean = true,
    val report: Report? = null,
    val notFound: Boolean = false,
    val error: LoadError? = null,
    val result: EvaluationResult? = null,
    val errors: Map<EvaluationField, FieldError> = emptyMap(),
    val confirming: Boolean = false,
    val submitting: Boolean = false,
    val submitFailed: Boolean = false,
    val alreadyEvaluated: Boolean = false,
    val outcome: EvaluateOutcome? = null,
)

@HiltViewModel
class EvaluateViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: EvaluationRepository,
    private val userMessages: UserMessages,
) : ViewModel() {

    private val reportId = savedStateHandle.toRoute<Evaluate>().reportId
    val notes = TextFieldState()
    val messages: Flow<UserMessage> = userMessages.messages

    private val _state = MutableStateFlow(EvaluateUiState())
    val state: StateFlow<EvaluateUiState> = _state.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            snapshotFlow { notes.text.toString() }.drop(1).collect { revalidate() }
        }
    }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val report = repository.report(reportId)
                _state.update { it.copy(loading = false, report = report) }
            } catch (e: Exception) {
                if (e is HttpException && e.code() == 404) {
                    _state.update { it.copy(loading = false, notFound = true) }
                } else {
                    val error = e.toLoadError() ?: throw e
                    _state.update { it.copy(loading = false, error = error) }
                }
            }
        }
    }

    fun onResultSelected(result: EvaluationResult) {
        _state.update { it.copy(result = result) }
        revalidate()
    }

    fun onSubmitClick() {
        val errors = EvaluationRules.validate(_state.value.result, notes.text.toString())
        _state.update { it.copy(errors = errors, confirming = errors.isEmpty()) }
    }

    fun onConfirmDismissed() {
        _state.update { it.copy(confirming = false) }
    }

    fun onSubmitFailedShown() {
        _state.update { it.copy(submitFailed = false) }
    }

    fun onConfirmed() {
        val result = _state.value.result ?: return
        _state.update { it.copy(confirming = false, submitting = true) }
        viewModelScope.launch {
            try {
                repository.evaluate(reportId, result, notes.text.toString())
                openNext()
            } catch (e: HttpException) {
                if (e.code() == 409) {
                    _state.update { it.copy(submitting = false, alreadyEvaluated = true) }
                } else {
                    _state.update { it.copy(submitting = false, submitFailed = true) }
                }
            } catch (e: Exception) {
                if (e.toLoadError() == null) throw e
                _state.update { it.copy(submitting = false, submitFailed = true) }
            }
        }
    }

    fun onViewExistingResult() {
        _state.update { it.copy(alreadyEvaluated = false) }
        load()
    }

    private suspend fun openNext() {
        val next = try {
            repository.queue().reports.firstOrNull { it.id != reportId }
        } catch (e: Exception) {
            if (e.toLoadError() == null) throw e
            null
        }
        if (next != null) {
            userMessages.show(UserMessage(R.string.message_evaluation_next))
            _state.update { it.copy(outcome = EvaluateOutcome.OpenNext(next.id)) }
        } else {
            userMessages.show(UserMessage(R.string.message_evaluation_sent))
            _state.update { it.copy(outcome = EvaluateOutcome.BackToQueue) }
        }
    }

    private fun revalidate() {
        _state.update {
            if (it.errors.isEmpty()) it else it.copy(errors = EvaluationRules.validate(it.result, notes.text.toString()))
        }
    }
}
