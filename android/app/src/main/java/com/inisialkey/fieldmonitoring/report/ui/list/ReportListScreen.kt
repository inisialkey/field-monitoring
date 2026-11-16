package com.inisialkey.fieldmonitoring.report.ui.list

import androidx.annotation.DrawableRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.inisialkey.fieldmonitoring.R
import com.inisialkey.fieldmonitoring.auth.ui.AccountMenu
import com.inisialkey.fieldmonitoring.core.ui.components.EmptyState
import com.inisialkey.fieldmonitoring.core.ui.components.ReportCard
import com.inisialkey.fieldmonitoring.core.ui.components.UserMessageEffect
import com.inisialkey.fieldmonitoring.core.ui.components.contentWidth
import com.inisialkey.fieldmonitoring.core.ui.theme.LocalStatusColors
import com.inisialkey.fieldmonitoring.report.domain.Report

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
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    UserMessageEffect(viewModel.messages, snackbarHostState)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.reports_title)) },
                actions = { AccountMenu(onSignedOut = onSignedOut) },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onCreateReport) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = null)
                Text(stringResource(R.string.create_report), modifier = Modifier.padding(start = 12.dp))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            state.banner?.let { ConnectivityBanner(it, onRetry = viewModel::retryFailed, onSignIn = onSignIn) }
            if (state.hasReports) FilterRow(state.filter, viewModel::onFilterChange)
            PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .contentWidth(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    when {
                        state.loading -> Unit
                        !state.hasReports -> item {
                            EmptyState(
                                icon = R.drawable.ic_edit_note,
                                title = stringResource(R.string.reports_empty_title),
                                message = stringResource(R.string.reports_empty_message),
                                actionLabel = stringResource(R.string.create_report),
                                onAction = onCreateReport,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        state.reports.isEmpty() -> item {
                            Text(
                                stringResource(R.string.reports_filter_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 24.dp),
                            )
                        }
                        else -> items(state.reports, key = { it.id }) { report ->
                            ReportCard(report, onClick = { onOpenReport(report) }, modifier = Modifier.animateItem())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterRow(selected: ReportFilter, onSelect: (ReportFilter) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .selectableGroup()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ReportFilter.entries.forEach { filter ->
            FilterChip(
                selected = filter == selected,
                onClick = { onSelect(filter) },
                label = { Text(stringResource(filter.label)) },
                leadingIcon = if (filter == selected) {
                    { Icon(painterResource(R.drawable.ic_check), contentDescription = null) }
                } else {
                    null
                },
            )
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
private fun ConnectivityBanner(banner: Banner, onRetry: () -> Unit, onSignIn: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val status = LocalStatusColors.current
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
            status.warningContainer,
            status.onWarningContainer,
            R.drawable.ic_cloud_off,
            stringResource(R.string.banner_offline_pending, banner.count),
        )
        Banner.Offline -> BannerStyle(
            colors.surfaceContainerHigh, colors.onSurface, R.drawable.ic_cloud_off, stringResource(R.string.banner_offline),
        )
        is Banner.Sending -> BannerStyle(
            colors.primaryContainer, colors.onPrimaryContainer, R.drawable.ic_sync, stringResource(R.string.banner_sending, banner.count),
        )
    }
    Surface(
        color = style.container,
        contentColor = style.content,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Column {
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(painterResource(style.icon), contentDescription = null)
                Text(
                    style.text,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 12.dp),
                )
                val action = when (banner) {
                    is Banner.Failed -> R.string.action_retry to onRetry
                    Banner.SessionExpired -> R.string.action_sign_in to onSignIn
                    else -> null
                }
                action?.let { (label, onClick) ->
                    TextButton(onClick = onClick, colors = ButtonDefaults.textButtonColors(contentColor = style.content)) {
                        Text(stringResource(label))
                    }
                }
            }
            if (banner is Banner.Sending) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}
