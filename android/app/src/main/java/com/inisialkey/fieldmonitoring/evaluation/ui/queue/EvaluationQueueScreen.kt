package com.inisialkey.fieldmonitoring.evaluation.ui.queue

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.inisialkey.fieldmonitoring.R
import com.inisialkey.fieldmonitoring.auth.ui.AccountMenu
import com.inisialkey.fieldmonitoring.core.ui.components.EmptyState
import com.inisialkey.fieldmonitoring.core.ui.components.ReportCard
import com.inisialkey.fieldmonitoring.core.ui.components.UserMessageEffect
import com.inisialkey.fieldmonitoring.core.ui.components.contentWidth

@Composable
fun EvaluationQueueScreen(
    onOpenReport: (String) -> Unit,
    onSignIn: () -> Unit,
    onSignedOut: () -> Unit,
    viewModel: EvaluationQueueViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyListState()
    val nearEnd by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= listState.layoutInfo.totalItemsCount - 3
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    LaunchedEffect(nearEnd, state.nextCursor) { if (nearEnd && state.nextCursor != null) viewModel.loadMore() }
    UserMessageEffect(viewModel.messages, snackbarHostState)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.queue_title)) },
                actions = { AccountMenu(onSignedOut = onSignedOut) },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = { viewModel.refresh(userInitiated = true) },
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .contentWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when {
                    state.loading -> items(3) { SkeletonCard() }
                    state.error == LoadError.SESSION_EXPIRED -> item {
                        EmptyState(
                            icon = R.drawable.ic_lock,
                            title = stringResource(R.string.session_expired_title),
                            message = stringResource(R.string.session_expired_message),
                            actionLabel = stringResource(R.string.action_sign_in),
                            onAction = onSignIn,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    state.error == LoadError.OFFLINE && state.reports.isEmpty() -> item {
                        EmptyState(
                            icon = R.drawable.ic_cloud_off,
                            title = stringResource(R.string.offline_title),
                            message = stringResource(R.string.offline_message),
                            actionLabel = stringResource(R.string.action_retry),
                            onAction = { viewModel.refresh(userInitiated = true) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    state.reports.isEmpty() -> item {
                        EmptyState(
                            icon = R.drawable.ic_check_circle,
                            title = stringResource(R.string.queue_empty_title),
                            message = stringResource(R.string.queue_empty_message),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    else -> items(state.reports, key = { it.id }) { report ->
                        ReportCard(report, onClick = { onOpenReport(report.id) }, showReporter = true, modifier = Modifier.animateItem())
                    }
                }
            }
        }
    }
}

@Composable
private fun SkeletonCard() {
    val loading = stringResource(R.string.loading_reports)
    val block = MaterialTheme.colorScheme.surfaceContainerHigh
    OutlinedCard(
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = loading },
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(block),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0.9f, 0.6f, 0.75f).forEach { width ->
                    Box(
                        Modifier
                            .fillMaxWidth(width)
                            .height(14.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(block),
                    )
                }
            }
        }
    }
}
