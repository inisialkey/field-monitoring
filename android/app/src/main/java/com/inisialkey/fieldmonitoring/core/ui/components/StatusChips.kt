package com.inisialkey.fieldmonitoring.core.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.inisialkey.fieldmonitoring.R
import com.inisialkey.fieldmonitoring.core.ui.theme.LocalStatusColors
import com.inisialkey.fieldmonitoring.report.domain.Evaluation
import com.inisialkey.fieldmonitoring.report.domain.EvaluationResult
import com.inisialkey.fieldmonitoring.report.domain.Report
import com.inisialkey.fieldmonitoring.report.domain.SyncState

@Composable
private fun StatusPill(
    label: String,
    @DrawableRes icon: Int,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.status_description, label)
    Surface(
        color = container,
        contentColor = content,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(16.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun SyncStatusChip(report: Report, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val status = LocalStatusColors.current
    when (report.syncState) {
        SyncState.DRAFT -> StatusPill(
            stringResource(R.string.sync_draft), R.drawable.ic_edit_note, colors.surfaceContainerHigh, colors.onSurfaceVariant, modifier,
        )
        SyncState.PENDING_SYNC -> StatusPill(
            stringResource(R.string.sync_pending), R.drawable.ic_schedule, status.warningContainer, status.onWarningContainer, modifier,
        )
        SyncState.SYNCING -> StatusPill(
            stringResource(R.string.sync_syncing_progress, report.photos.count { it.uploaded }, report.photos.size),
            R.drawable.ic_sync,
            colors.primaryContainer,
            colors.onPrimaryContainer,
            modifier,
        )
        SyncState.SYNCED -> StatusPill(
            stringResource(R.string.sync_synced), R.drawable.ic_cloud_done, status.successContainer, status.onSuccessContainer, modifier,
        )
        SyncState.FAILED -> StatusPill(
            stringResource(R.string.sync_failed), R.drawable.ic_error, colors.errorContainer, colors.onErrorContainer, modifier,
        )
        SyncState.NEEDS_ATTENTION -> StatusPill(
            stringResource(R.string.sync_attention), R.drawable.ic_block, colors.errorContainer, colors.onErrorContainer, modifier,
        )
    }
}

@Composable
fun EvaluationBadge(evaluation: Evaluation?, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val status = LocalStatusColors.current
    when (evaluation?.result) {
        null -> StatusPill(
            stringResource(R.string.evaluation_waiting), R.drawable.ic_hourglass_empty, colors.surfaceContainerHigh, colors.onSurfaceVariant, modifier,
        )
        EvaluationResult.VALID -> StatusPill(
            stringResource(R.string.evaluation_valid), R.drawable.ic_check_circle, status.successContainer, status.onSuccessContainer, modifier,
        )
        EvaluationResult.INVALID -> StatusPill(
            stringResource(R.string.evaluation_invalid), R.drawable.ic_cancel, colors.errorContainer, colors.onErrorContainer, modifier,
        )
    }
}
