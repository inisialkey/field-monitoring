package com.inisialkey.fieldmonitoring.report.ui.list

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.inisialkey.fieldmonitoring.R
import com.inisialkey.fieldmonitoring.auth.ui.AccountHeader
import com.inisialkey.fieldmonitoring.core.ui.components.ConnectivityPill
import com.inisialkey.fieldmonitoring.core.ui.components.EmptyState
import com.inisialkey.fieldmonitoring.core.ui.components.HeroHeader
import com.inisialkey.fieldmonitoring.core.ui.components.IconBadge
import com.inisialkey.fieldmonitoring.core.ui.components.ReportCard
import com.inisialkey.fieldmonitoring.core.ui.components.StatTile
import com.inisialkey.fieldmonitoring.core.ui.components.StatusBarScrim
import com.inisialkey.fieldmonitoring.core.ui.components.UserMessageEffect
import com.inisialkey.fieldmonitoring.core.ui.components.contentWidth
import com.inisialkey.fieldmonitoring.core.ui.theme.LocalAppColors
import com.inisialkey.fieldmonitoring.report.domain.Report

private val SummaryTiles = listOf(
    ReportFilter.DRAFT to R.drawable.ic_edit_note,
    ReportFilter.UNSENT to R.drawable.ic_cloud_upload,
    ReportFilter.WAITING to R.drawable.ic_hourglass_empty,
    ReportFilter.EVALUATED to R.drawable.ic_task_alt,
)

@Composable
fun ReportListScreen(
    onCreateReport: () -> Unit,
    onOpenReport: (Report) -> Unit,
    onSignIn: () -> Unit,
    onSignedOut: () -> Unit,
    viewModel: ReportListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val pullState = rememberPullToRefreshState()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    UserMessageEffect(viewModel.messages, snackbarHostState)

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateReport,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.navigationBarsPadding(),
            ) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = null)
                Text(stringResource(R.string.create_report), modifier = Modifier.padding(start = 12.dp))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState, modifier = Modifier.navigationBarsPadding()) },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = viewModel::refresh,
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
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 112.dp + bottomInset),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(key = "hero") {
                        SummaryHero(state, onFilter = viewModel::onFilterChange, onSignedOut = onSignedOut)
                    }
                    state.banner?.let { banner ->
                        item(key = "banner") {
                            ConnectivityBanner(
                                banner,
                                onRetry = viewModel::retryFailed,
                                onSignIn = onSignIn,
                                modifier = Modifier
                                    .contentWidth()
                                    .padding(horizontal = 16.dp),
                            )
                        }
                    }
                    if (!state.loading && state.hasReports) {
                        item(key = "title") { ListTitle(state, onShowAll = { viewModel.onFilterChange(ReportFilter.ALL) }) }
                    }
                    when {
                        state.loading -> Unit
                        !state.hasReports -> item(key = "empty") {
                            EmptyState(
                                icon = R.drawable.ic_edit_note,
                                title = stringResource(R.string.reports_empty_title),
                                message = stringResource(R.string.reports_empty_message),
                                actionLabel = stringResource(R.string.create_report),
                                onAction = onCreateReport,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        state.reports.isEmpty() -> item(key = "filtered-empty") {
                            EmptyState(
                                icon = R.drawable.ic_task_alt,
                                title = stringResource(state.filter.label),
                                message = stringResource(R.string.reports_filter_empty),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        else -> items(state.reports, key = { it.id }) { report ->
                            ReportCard(
                                report,
                                onClick = { onOpenReport(report) },
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
private fun SummaryHero(state: ReportListUiState, onFilter: (ReportFilter) -> Unit, onSignedOut: () -> Unit) {
    HeroHeader {
        AccountHeader(onSignedOut = onSignedOut)
        ConnectivityPill(state.online, modifier = Modifier.padding(top = 14.dp))
        Text(
            stringResource(R.string.reports_summary),
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            modifier = Modifier
                .padding(top = 24.dp, bottom = 12.dp)
                .semantics { heading() },
        )
        SummaryTiles.chunked(2).forEachIndexed { index, row ->
            Row(
                modifier = Modifier.padding(top = if (index == 0) 0.dp else 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEach { (filter, icon) ->
                    StatTile(
                        count = state.counts[filter] ?: 0,
                        label = stringResource(filter.label),
                        icon = icon,
                        selected = state.filter == filter,
                        onClick = { onFilter(if (state.filter == filter) ReportFilter.ALL else filter) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ListTitle(state: ReportListUiState, onShowAll: () -> Unit) {
    Row(
        modifier = Modifier
            .contentWidth()
            .padding(start = 20.dp, end = 8.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(if (state.filter == ReportFilter.ALL) R.string.reports_title else state.filter.label),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(R.string.report_count, state.reports.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (state.filter != ReportFilter.ALL) {
            TextButton(onClick = onShowAll) { Text(stringResource(R.string.show_all)) }
        }
    }
}

private data class BannerStyle(
    val container: Color,
    val content: Color,
    @DrawableRes val icon: Int,
    val text: String,
)

@Composable
private fun ConnectivityBanner(banner: Banner, onRetry: () -> Unit, onSignIn: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val app = LocalAppColors.current
    val style = when (banner) {
        Banner.SessionExpired -> BannerStyle(
            colors.errorContainer, colors.onErrorContainer, R.drawable.ic_lock, stringResource(R.string.banner_session_expired),
        )
        is Banner.Failed -> BannerStyle(
            colors.errorContainer, colors.onErrorContainer, R.drawable.ic_error, stringResource(R.string.banner_failed, banner.count),
        )
        is Banner.Attention -> BannerStyle(
            colors.errorContainer, colors.onErrorContainer, R.drawable.ic_block, stringResource(R.string.banner_attention, banner.count),
        )
        is Banner.OfflinePending -> BannerStyle(
            app.warningContainer, app.onWarningContainer, R.drawable.ic_cloud_off, stringResource(R.string.banner_offline_pending, banner.count),
        )
        Banner.Offline -> BannerStyle(
            colors.surfaceContainerHigh, colors.onSurface, R.drawable.ic_cloud_off, stringResource(R.string.banner_offline),
        )
        is Banner.Sending -> BannerStyle(
            colors.tertiaryContainer, colors.onTertiaryContainer, R.drawable.ic_sync, stringResource(R.string.banner_sending, banner.count),
        )
    }
    Surface(
        color = style.container,
        contentColor = style.content,
        shape = MaterialTheme.shapes.large,
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Column {
            Row(
                modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IconBadge(style.icon, container = style.content.copy(alpha = 0.12f), content = style.content)
                Text(style.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                val action = when (banner) {
                    is Banner.Failed -> R.string.action_retry to onRetry
                    Banner.SessionExpired -> R.string.action_sign_in to onSignIn
                    else -> null
                }
                action?.let { (label, onClick) ->
                    TextButton(onClick = onClick, colors = ButtonDefaults.textButtonColors(contentColor = style.content)) {
                        Text(stringResource(label), fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (banner is Banner.Sending) {
                LinearProgressIndicator(
                    strokeCap = StrokeCap.Round,
                    color = style.content,
                    trackColor = style.content.copy(alpha = 0.2f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
                )
            }
        }
    }
}
