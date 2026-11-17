package com.inisialkey.fieldmonitoring.report.ui.detail

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inisialkey.fieldmonitoring.R
import com.inisialkey.fieldmonitoring.core.ui.components.Avatar
import com.inisialkey.fieldmonitoring.core.ui.components.EvaluationBadge
import com.inisialkey.fieldmonitoring.core.ui.components.InfoRow
import com.inisialkey.fieldmonitoring.core.ui.components.PhotoPager
import com.inisialkey.fieldmonitoring.core.ui.components.SectionCard
import com.inisialkey.fieldmonitoring.core.ui.components.SyncStatusChip
import com.inisialkey.fieldmonitoring.core.ui.coordinates
import com.inisialkey.fieldmonitoring.core.ui.formatDateTime
import com.inisialkey.fieldmonitoring.core.ui.iconRes
import com.inisialkey.fieldmonitoring.core.ui.labelRes
import com.inisialkey.fieldmonitoring.core.ui.theme.LocalAppColors
import com.inisialkey.fieldmonitoring.report.domain.Evaluation
import com.inisialkey.fieldmonitoring.report.domain.EvaluationResult
import com.inisialkey.fieldmonitoring.report.domain.Photo
import com.inisialkey.fieldmonitoring.report.domain.Report
import com.inisialkey.fieldmonitoring.report.domain.SyncState
import kotlin.math.roundToInt

@Composable
fun ColumnScope.ReportDetailSections(report: Report, forOwner: Boolean, onOpenPhoto: (Photo) -> Unit) {
    PhotoPager(photos = report.photos, onOpen = onOpenPhoto)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(report.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (forOwner) SyncStatusChip(report)
            if (report.syncState == SyncState.SYNCED) EvaluationBadge(report.evaluation)
        }
    }
    report.evaluation?.let { EvaluationResultCard(it, showInvalidHint = forOwner) }
    if (forOwner) {
        SectionCard(title = stringResource(R.string.detail_timeline), icon = R.drawable.ic_schedule) {
            StatusTimeline(report)
        }
    }
    SectionCard(title = stringResource(R.string.detail_info), icon = R.drawable.ic_notes) {
        report.reporterName?.takeIf { !forOwner }?.let { InfoRow(R.drawable.ic_person, stringResource(R.string.detail_reporter), it) }
        report.category?.let { InfoRow(it.iconRes(), stringResource(R.string.detail_category), stringResource(it.labelRes())) }
        InfoRow(
            R.drawable.ic_event,
            stringResource(R.string.detail_incident_time),
            formatDateTime(report.incidentAt, report.incidentOffsetMinutes),
        )
        InfoRow(
            R.drawable.ic_location_on,
            stringResource(R.string.detail_location),
            report.location?.let { stringResource(R.string.detail_coordinates, it.coordinates(), it.accuracyM.roundToInt()) }
                ?: stringResource(R.string.no_gps),
        )
        if (report.locationNote.isNotBlank()) {
            InfoRow(R.drawable.ic_map, stringResource(R.string.detail_location_note), report.locationNote)
        }
    }
    SectionCard(title = stringResource(R.string.detail_description), icon = R.drawable.ic_edit_note) {
        Text(report.description, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun EvaluationResultCard(evaluation: Evaluation, showInvalidHint: Boolean) {
    val valid = evaluation.result == EvaluationResult.VALID
    val app = LocalAppColors.current
    val container = if (valid) app.successContainer else MaterialTheme.colorScheme.errorContainer
    val content = if (valid) app.onSuccessContainer else MaterialTheme.colorScheme.onErrorContainer
    val accent = if (valid) app.success else MaterialTheme.colorScheme.error
    Surface(
        shape = MaterialTheme.shapes.large,
        color = container,
        contentColor = content,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(
                    painterResource(if (valid) R.drawable.ic_check_circle else R.drawable.ic_cancel),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(32.dp),
                )
                Column {
                    Text(stringResource(R.string.detail_evaluation), style = MaterialTheme.typography.labelLarge)
                    Text(
                        stringResource(if (valid) R.string.evaluation_valid else R.string.evaluation_invalid),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            evaluation.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                Row(modifier = Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier
                            .width(3.dp)
                            .fillMaxHeight()
                            .background(accent, CircleShape),
                    )
                    Text(notes, style = MaterialTheme.typography.bodyLarge)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Avatar(evaluation.evaluatorName, size = 28.dp, container = accent.copy(alpha = 0.16f), content = content)
                Text(
                    stringResource(R.string.detail_evaluated_by, evaluation.evaluatorName, formatDateTime(evaluation.evaluatedAt)),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (showInvalidHint && !valid) {
                Text(stringResource(R.string.detail_invalid_hint), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private enum class StepState { DONE, CURRENT, ERROR, UPCOMING }

private data class Step(val title: String, val detail: String?, val state: StepState)

@Composable
private fun StatusTimeline(report: Report) {
    val delivery = when (report.syncState) {
        SyncState.SYNCED -> Step(
            stringResource(R.string.timeline_received),
            report.receivedAt?.let { formatDateTime(it) },
            StepState.DONE,
        )
        SyncState.SYNCING -> Step(
            stringResource(R.string.timeline_sending),
            stringResource(R.string.sync_syncing_progress, report.photos.count { it.uploaded }, report.photos.size),
            StepState.CURRENT,
        )
        SyncState.FAILED -> Step(stringResource(R.string.timeline_failed), report.lastError, StepState.ERROR)
        SyncState.NEEDS_ATTENTION -> Step(stringResource(R.string.timeline_attention), report.lastError, StepState.ERROR)
        SyncState.PENDING_SYNC, SyncState.DRAFT -> Step(stringResource(R.string.timeline_waiting), null, StepState.CURRENT)
    }
    val evaluation = report.evaluation
    val review = when {
        evaluation != null -> Step(
            stringResource(
                R.string.timeline_evaluated,
                stringResource(if (evaluation.result == EvaluationResult.VALID) R.string.evaluation_valid else R.string.evaluation_invalid),
            ),
            formatDateTime(evaluation.evaluatedAt),
            StepState.DONE,
        )
        report.syncState == SyncState.SYNCED -> Step(stringResource(R.string.timeline_waiting_evaluation), null, StepState.CURRENT)
        else -> Step(stringResource(R.string.timeline_evaluation), null, StepState.UPCOMING)
    }
    val steps = listOf(
        Step(stringResource(R.string.timeline_created), formatDateTime(report.createdAt), StepState.DONE),
        delivery,
        review,
    )
    Column {
        steps.forEachIndexed { index, step ->
            TimelineStep(step, isLast = index == steps.lastIndex)
        }
    }
}

@Composable
private fun TimelineStep(step: Step, isLast: Boolean) {
    val colors = MaterialTheme.colorScheme
    val stateLabel = stringResource(
        when (step.state) {
            StepState.DONE -> R.string.step_done
            StepState.CURRENT -> R.string.step_current
            StepState.ERROR -> R.string.step_error
            StepState.UPCOMING -> R.string.step_upcoming
        },
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .semantics(mergeDescendants = true) { stateDescription = stateLabel },
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            when (step.state) {
                StepState.DONE -> StepNode(R.drawable.ic_check, colors.primary, colors.onPrimary)
                StepState.ERROR -> StepNode(R.drawable.ic_error, colors.error, colors.onError)
                StepState.CURRENT -> Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(colors.primaryContainer, CircleShape)
                        .padding(7.dp)
                        .background(colors.primary, CircleShape),
                )
                StepState.UPCOMING -> Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(colors.surfaceContainerHigh, CircleShape),
                )
            }
            if (!isLast) {
                Box(
                    Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(if (step.state == StepState.DONE) colors.primary else colors.outlineVariant),
                )
            }
        }
        Column(modifier = Modifier.padding(bottom = if (isLast) 0.dp else 20.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                step.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = when (step.state) {
                    StepState.ERROR -> colors.error
                    StepState.UPCOMING -> colors.onSurfaceVariant
                    else -> colors.onSurface
                },
            )
            step.detail?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant) }
        }
    }
}

@Composable
private fun StepNode(@DrawableRes icon: Int, container: Color, content: Color) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(container, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = content, modifier = Modifier.size(14.dp))
    }
}
