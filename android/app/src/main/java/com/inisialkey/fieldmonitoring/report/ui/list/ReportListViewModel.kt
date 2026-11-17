package com.inisialkey.fieldmonitoring.report.ui.list

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inisialkey.fieldmonitoring.R
import com.inisialkey.fieldmonitoring.auth.data.SessionStore
import com.inisialkey.fieldmonitoring.core.ConnectivityMonitor
import com.inisialkey.fieldmonitoring.core.UserMessage
import com.inisialkey.fieldmonitoring.core.UserMessages
import com.inisialkey.fieldmonitoring.report.data.ReportRepository
import com.inisialkey.fieldmonitoring.report.domain.Report
import com.inisialkey.fieldmonitoring.report.domain.SyncState
import com.inisialkey.fieldmonitoring.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

enum class ReportFilter(@StringRes val label: Int) {
    ALL(R.string.filter_all),
    DRAFT(R.string.filter_draft),
    UNSENT(R.string.filter_unsent),
    WAITING(R.string.filter_waiting),
    EVALUATED(R.string.filter_evaluated),
    ;

    fun matches(report: Report): Boolean = when (this) {
        ALL -> true
        DRAFT -> report.syncState == SyncState.DRAFT
        UNSENT -> report.isUnsent
        WAITING -> report.syncState == SyncState.SYNCED && report.evaluation == null
        EVALUATED -> report.evaluation != null
    }
}

sealed interface Banner {
    data object SessionExpired : Banner
    data class Failed(val count: Int) : Banner
    data class Attention(val count: Int) : Banner
    data class OfflinePending(val count: Int) : Banner
    data object Offline : Banner
    data class Sending(val count: Int) : Banner
}

fun bannerFor(reports: List<Report>, online: Boolean, sessionExpired: Boolean): Banner? {
    val pending = reports.count { it.syncState == SyncState.PENDING_SYNC || it.syncState == SyncState.SYNCING }
    val failed = reports.count { it.syncState == SyncState.FAILED }
    val attention = reports.count { it.syncState == SyncState.NEEDS_ATTENTION }
    return when {
        sessionExpired -> Banner.SessionExpired
        failed > 0 -> Banner.Failed(failed)
        attention > 0 -> Banner.Attention(attention)
        !online && pending > 0 -> Banner.OfflinePending(pending)
        !online -> Banner.Offline
        pending > 0 -> Banner.Sending(pending)
        else -> null
    }
}

data class ReportListUiState(
    val loading: Boolean = true,
    val reports: List<Report> = emptyList(),
    val hasReports: Boolean = false,
    val filter: ReportFilter = ReportFilter.ALL,
    val counts: Map<ReportFilter, Int> = emptyMap(),
    val online: Boolean = true,
    val banner: Banner? = null,
    val refreshing: Boolean = false,
)

@HiltViewModel
class ReportListViewModel @Inject constructor(
    sessionStore: SessionStore,
    private val reports: ReportRepository,
    private val connectivity: ConnectivityMonitor,
    private val syncScheduler: SyncScheduler,
    private val userMessages: UserMessages,
) : ViewModel() {

    private val ownerId = sessionStore.session.value?.userId.orEmpty()
    private val filter = MutableStateFlow(ReportFilter.ALL)
    private val refreshing = MutableStateFlow(false)

    val messages: Flow<UserMessage> = userMessages.messages

    val state: StateFlow<ReportListUiState> = combine(
        reports.observeReports(ownerId),
        filter,
        connectivity.isOnline,
        sessionStore.session.map { it?.expired == true },
        refreshing,
    ) { all, filter, online, expired, refreshing ->
        ReportListUiState(
            loading = false,
            reports = all.filter(filter::matches),
            hasReports = all.isNotEmpty(),
            filter = filter,
            counts = ReportFilter.entries.associateWith { candidate -> all.count(candidate::matches) },
            online = online,
            banner = bannerFor(all, online, expired),
            refreshing = refreshing,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportListUiState())

    init {
        if (connectivity.isOnline.value) viewModelScope.launch { syncScheduler.syncNow(ownerId) }
    }

    fun onFilterChange(value: ReportFilter) {
        filter.value = value
    }

    fun refresh() {
        if (!connectivity.isOnline.value || refreshing.value) return
        viewModelScope.launch {
            refreshing.value = true
            syncScheduler.syncNow(ownerId)
            withTimeoutOrNull(20.seconds) {
                syncScheduler.observe(ownerId).first { infos -> infos.all { it.state.isFinished } }
            }
            refreshing.value = false
        }
    }

    fun retryFailed() {
        viewModelScope.launch {
            val count = reports.retryFailed(ownerId)
            if (count > 0) userMessages.show(UserMessage(R.string.message_retry, listOf(count)))
        }
    }
}
