package com.inisialkey.fieldmonitoring.report.ui.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.inisialkey.fieldmonitoring.R
import com.inisialkey.fieldmonitoring.core.ui.components.EvaluationBadge
import com.inisialkey.fieldmonitoring.core.ui.components.SyncStatusChip
import com.inisialkey.fieldmonitoring.core.ui.coordinates
import com.inisialkey.fieldmonitoring.core.ui.formatDateTime
import com.inisialkey.fieldmonitoring.core.ui.labelRes
import com.inisialkey.fieldmonitoring.report.domain.Evaluation
import com.inisialkey.fieldmonitoring.report.domain.EvaluationResult
import com.inisialkey.fieldmonitoring.report.domain.Photo
import com.inisialkey.fieldmonitoring.report.domain.Report
import com.inisialkey.fieldmonitoring.report.domain.SyncState
import com.inisialkey.fieldmonitoring.core.ui.components.PhotoGrid
import kotlin.math.roundToInt

@Composable
fun ColumnScope.ReportDetailSections(report: Report, forOwner: Boolean, onOpenPhoto: ((Photo) -> Unit)? = null) {
    PhotoGrid(photos = report.photos, onOpen = onOpenPhoto)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(report.title, style = MaterialTheme.typography.titleLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (forOwner) SyncStatusChip(report)
            if (report.syncState == SyncState.SYNCED) EvaluationBadge(report.evaluation)
        }
    }
    if (forOwner && report.lastError != null && report.syncState != SyncState.SYNCED) {
        Notice(report.lastError)
    }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        report.reporterName?.takeIf { !forOwner }?.let { DetailItem(stringResource(R.string.detail_reporter), it) }
        report.category?.let { DetailItem(stringResource(R.string.detail_category), stringResource(it.labelRes())) }
        DetailItem(stringResource(R.string.detail_incident_time), formatDateTime(report.incidentAt, report.incidentOffsetMinutes))
        DetailItem(
            stringResource(R.string.detail_location),
            report.location?.let { stringResource(R.string.detail_coordinates, it.coordinates(), it.accuracyM.roundToInt()) }
                ?: stringResource(R.string.no_gps),
        )
        if (report.locationNote.isNotBlank()) DetailItem(stringResource(R.string.detail_location_note), report.locationNote)
        DetailItem(stringResource(R.string.detail_description), report.description)
        DetailItem(stringResource(R.string.detail_created), formatDateTime(report.createdAt))
        report.receivedAt?.let { DetailItem(stringResource(R.string.detail_received), formatDateTime(it)) }
    }
    report.evaluation?.let { EvaluationSection(it, showInvalidHint = forOwner) }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun Notice(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_error), contentDescription = null)
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun EvaluationSection(evaluation: Evaluation, showInvalidHint: Boolean) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.detail_evaluation), style = MaterialTheme.typography.titleMedium)
            EvaluationBadge(evaluation)
            evaluation.notes?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
            Text(
                stringResource(R.string.detail_evaluated_by, evaluation.evaluatorName, formatDateTime(evaluation.evaluatedAt)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (showInvalidHint && evaluation.result == EvaluationResult.INVALID) {
                Text(stringResource(R.string.detail_invalid_hint), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
