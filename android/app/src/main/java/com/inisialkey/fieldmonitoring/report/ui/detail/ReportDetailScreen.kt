package com.inisialkey.fieldmonitoring.report.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.inisialkey.fieldmonitoring.R
import com.inisialkey.fieldmonitoring.ReportDetail
import com.inisialkey.fieldmonitoring.auth.data.SessionStore
import com.inisialkey.fieldmonitoring.core.ui.components.BackButton
import com.inisialkey.fieldmonitoring.core.ui.components.contentWidth
import com.inisialkey.fieldmonitoring.report.data.ReportRepository
import com.inisialkey.fieldmonitoring.report.domain.Report
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ReportDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    sessionStore: SessionStore,
    reports: ReportRepository,
) : ViewModel() {
    val report: StateFlow<Report?> = reports
        .observeReport(savedStateHandle.toRoute<ReportDetail>().reportId, sessionStore.session.value?.userId.orEmpty())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun ReportDetailScreen(onBack: () -> Unit, viewModel: ReportDetailViewModel = hiltViewModel()) {
    val report by viewModel.report.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.detail_title)) }, navigationIcon = { BackButton(onBack) })
        },
    ) { padding ->
        report?.let {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .contentWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                ReportDetailSections(it, forOwner = true)
            }
        }
    }
}
