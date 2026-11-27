package com.inisialkey.fieldmonitoring.evaluation.ui.queue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inisialkey.fieldmonitoring.core.UserMessage
import com.inisialkey.fieldmonitoring.core.UserMessages
import com.inisialkey.fieldmonitoring.evaluation.data.EvaluationRepository
import com.inisialkey.fieldmonitoring.report.domain.Category
import com.inisialkey.fieldmonitoring.report.domain.Report
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

enum class LoadError { OFFLINE, SESSION_EXPIRED }

data class EvaluationQueueUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val reports: List<Report> = emptyList(),
    val nextCursor: String? = null,
    val category: Category? = null,
    val error: LoadError? = null,
)

fun Exception.toLoadError(): LoadError? = when (this) {
    is HttpException -> if (code() == 401) LoadError.SESSION_EXPIRED else LoadError.OFFLINE
    is IOException, is SerializationException -> LoadError.OFFLINE
    else -> null
}

@HiltViewModel
class EvaluationQueueViewModel @Inject constructor(
    private val repository: EvaluationRepository,
    userMessages: UserMessages,
) : ViewModel() {

    private val _state = MutableStateFlow(EvaluationQueueUiState())
    val state: StateFlow<EvaluationQueueUiState> = _state.asStateFlow()
    val messages: Flow<UserMessage> = userMessages.messages

    private var loadJob: Job? = null

    fun refresh(userInitiated: Boolean = false) {
        loadJob?.cancel()
        _state.update { it.copy(refreshing = userInitiated, loading = it.loading && it.reports.isEmpty()) }
        val category = _state.value.category
        loadJob = viewModelScope.launch {
            try {
                val page = repository.queue(category = category)
                _state.update {
                    it.copy(loading = false, refreshing = false, reports = page.reports, nextCursor = page.nextCursor, error = null)
                }
            } catch (e: Exception) {
                val error = e.toLoadError() ?: throw e
                _state.update { it.copy(loading = false, refreshing = false, error = error) }
            }
        }
    }

    fun selectCategory(category: Category?) {
        if (category == _state.value.category) return
        _state.update { it.copy(category = category, loading = true, reports = emptyList(), nextCursor = null, error = null) }
        refresh()
    }

    fun loadMore() {
        val current = _state.value
        val cursor = current.nextCursor ?: return
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            try {
                val page = repository.queue(cursor, current.category)
                _state.update { state ->
                    val known = state.reports.map { it.id }.toSet()
                    state.copy(reports = state.reports + page.reports.filterNot { it.id in known }, nextCursor = page.nextCursor)
                }
            } catch (e: Exception) {
                if (e.toLoadError() == null) throw e
            }
        }
    }
}
