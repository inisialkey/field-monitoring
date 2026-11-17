package com.inisialkey.fieldmonitoring.report.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
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
import com.inisialkey.fieldmonitoring.core.UserMessage
import com.inisialkey.fieldmonitoring.core.UserMessages
import com.inisialkey.fieldmonitoring.core.ui.components.BackButton
import com.inisialkey.fieldmonitoring.core.ui.components.DeleteDialog
import com.inisialkey.fieldmonitoring.core.ui.components.FullScreenPhoto
import com.inisialkey.fieldmonitoring.core.ui.components.contentWidth
import com.inisialkey.fieldmonitoring.report.data.ReportRepository
import com.inisialkey.fieldmonitoring.report.domain.Photo
import com.inisialkey.fieldmonitoring.report.domain.Report
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ReportDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    sessionStore: SessionStore,
    private val reports: ReportRepository,
    private val userMessages: UserMessages,
) : ViewModel() {
    val report: StateFlow<Report?> = reports
        .observeReport(savedStateHandle.toRoute<ReportDetail>().reportId, sessionStore.session.value?.userId.orEmpty())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    fun delete() {
        val current = report.value ?: return
        viewModelScope.launch {
            if (reports.deleteUnsent(current)) {
                userMessages.show(UserMessage(R.string.message_report_deleted))
                _deleted.value = true
            }
        }
    }
}

@Composable
fun ReportDetailScreen(onBack: () -> Unit, viewModel: ReportDetailViewModel = hiltViewModel()) {
    val report by viewModel.report.collectAsStateWithLifecycle()
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }
    var openPhoto by remember { mutableStateOf<Photo?>(null) }

    LaunchedEffect(deleted) { if (deleted) onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.detail_title)) },
                navigationIcon = { BackButton(onBack) },
                actions = {
                    if (report?.canDelete == true) {
                        IconButton(onClick = { confirmingDelete = true }) {
                            Icon(painterResource(R.drawable.ic_delete), contentDescription = stringResource(R.string.delete_report))
                        }
                    }
                },
            )
        },
    ) { padding ->
        report?.let {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .contentWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ReportDetailSections(it, forOwner = true, onOpenPhoto = { photo -> openPhoto = photo })
            }
        }
    }

    openPhoto?.let { FullScreenPhoto(it, onDismiss = { openPhoto = null }) }

    if (confirmingDelete) {
        DeleteDialog(
            title = stringResource(R.string.dialog_delete_report_title),
            message = stringResource(R.string.dialog_delete_report_message),
            onConfirm = {
                confirmingDelete = false
                viewModel.delete()
            },
            onDismiss = { confirmingDelete = false },
        )
    }
}
