package com.inisialkey.fieldmonitoring.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.inisialkey.fieldmonitoring.BuildConfig
import com.inisialkey.fieldmonitoring.R
import com.inisialkey.fieldmonitoring.core.ui.coordinates
import com.inisialkey.fieldmonitoring.core.ui.formatDateTime
import com.inisialkey.fieldmonitoring.core.ui.labelRes
import com.inisialkey.fieldmonitoring.report.domain.Photo
import com.inisialkey.fieldmonitoring.report.domain.Report
import com.inisialkey.fieldmonitoring.report.domain.SyncState
import java.io.File

fun Photo.imageModel(): Any = localPath?.let(::File) ?: "${BuildConfig.API_BASE_URL}api/v1/attachments/$id/content"

@Composable
fun PhotoThumbnail(photo: Photo?, modifier: Modifier = Modifier, contentDescription: String? = null) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(R.drawable.ic_image), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        if (photo != null) {
            AsyncImage(
                model = photo.imageModel(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

@Composable
fun locationSummary(report: Report): String {
    val location = report.location
    val note = report.locationNote.trim()
    return when {
        location == null -> listOf(stringResource(R.string.no_gps), note).filter { it.isNotEmpty() }.joinToString(" · ")
        note.isNotEmpty() -> note
        else -> location.coordinates()
    }
}

@Composable
fun ReportCard(report: Report, onClick: () -> Unit, modifier: Modifier = Modifier, showReporter: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    OutlinedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = colors.surfaceContainerLowest),
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PhotoThumbnail(report.photos.firstOrNull(), Modifier.size(64.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = report.title.ifBlank { stringResource(R.string.report_untitled) },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (report.syncState == SyncState.DRAFT && report.lastError != null) {
                    Text(report.lastError, style = MaterialTheme.typography.bodyMedium, color = colors.error)
                }
                if (showReporter && report.reporterName != null) {
                    Text(report.reporterName, style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    text = listOfNotNull(
                        report.category?.let { stringResource(it.labelRes()) },
                        formatDateTime(report.incidentAt, report.incidentOffsetMinutes),
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                Text(
                    text = locationSummary(report),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.photo_count, report.photos.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                if (!showReporter) {
                    FlowRow(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SyncStatusChip(report)
                        if (report.syncState == SyncState.SYNCED) EvaluationBadge(report.evaluation)
                    }
                }
                if (report.syncState == SyncState.SYNCING && report.photos.isNotEmpty()) {
                    LinearProgressIndicator(
                        progress = { report.photos.count { it.uploaded }.toFloat() / report.photos.size },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                    )
                }
            }
        }
    }
}
