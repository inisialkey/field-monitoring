package com.inisialkey.fieldmonitoring.evaluation.ui.queue

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.inisialkey.fieldmonitoring.R
import com.inisialkey.fieldmonitoring.auth.ui.AccountHeader
import com.inisialkey.fieldmonitoring.core.ui.components.AppCard
import com.inisialkey.fieldmonitoring.core.ui.components.EmptyState
import com.inisialkey.fieldmonitoring.core.ui.components.HeroHeader
import com.inisialkey.fieldmonitoring.core.ui.components.HeroText
import com.inisialkey.fieldmonitoring.core.ui.components.IconBadge
import com.inisialkey.fieldmonitoring.core.ui.components.ReportCard
import com.inisialkey.fieldmonitoring.core.ui.components.StatusBarScrim
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
    val pullState = rememberPullToRefreshState()
    val listState = rememberLazyListState()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
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
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState, modifier = Modifier.navigationBarsPadding()) },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = { viewModel.refresh(userInitiated = true) },
                state = pullState,
                indicator = {
                    PullToRefreshDefaults.Indicator(
                        state = pullState,
                        isRefreshing = state.refreshing,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding(),
                    )
                },
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp + bottomInset),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(key = "hero") { QueueHero(state, onSignedOut) }
                    item(key = "title") {
                        Text(
                            stringResource(R.string.queue_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .contentWidth()
                                .padding(start = 20.dp, end = 20.dp, top = 8.dp)
                                .semantics { heading() },
                        )
                    }
                    when {
                        state.loading -> items(3) { SkeletonCard(Modifier.contentWidth().padding(horizontal = 16.dp)) }
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
                                icon = R.drawable.ic_task_alt,
                                title = stringResource(R.string.queue_empty_title),
                                message = stringResource(R.string.queue_empty_message),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        else -> items(state.reports, key = { it.id }) { report ->
                            ReportCard(
                                report,
                                onClick = { onOpenReport(report.id) },
                                showReporter = true,
                                modifier = Modifier
                                    .animateItem()
                                    .contentWidth()
                                    .padding(horizontal = 16.dp),
                            )
                        }
                    }
                }
            }
            StatusBarScrim(Modifier.align(Alignment.TopCenter))
        }
    }
}

@Composable
private fun QueueHero(state: EvaluationQueueUiState, onSignedOut: () -> Unit) {
    val count = if (state.loading) "–" else state.reports.size.toString() + if (state.nextCursor != null) "+" else ""
    HeroHeader {
        AccountHeader(onSignedOut = onSignedOut)
        Surface(
            shape = MaterialTheme.shapes.large,
            color = Color.White.copy(alpha = 0.14f),
            contentColor = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(count, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.queue_waiting_label), style = MaterialTheme.typography.titleSmall)
                    HeroText(stringResource(R.string.queue_order_hint), modifier = Modifier.padding(top = 4.dp))
                }
                IconBadge(
                    R.drawable.ic_hourglass_empty,
                    size = 56.dp,
                    container = Color.White.copy(alpha = 0.18f),
                    content = Color.White,
                )
            }
        }
    }
}

@Composable
private fun SkeletonCard(modifier: Modifier = Modifier) {
    val loading = stringResource(R.string.loading_reports)
    val block = MaterialTheme.colorScheme.surfaceContainerHigh
    val pulse by rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 900), RepeatMode.Reverse),
        label = "skeleton-alpha",
    )
    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = loading },
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .alpha(pulse),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier
                    .size(88.dp)
                    .background(block, MaterialTheme.shapes.medium),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(0.5f, 0.9f, 0.7f, 0.4f).forEach { width ->
                    Box(
                        Modifier
                            .fillMaxWidth(width)
                            .height(14.dp)
                            .background(block, MaterialTheme.shapes.small),
                    )
                }
            }
        }
    }
}
