package com.inisialkey.fieldmonitoring.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.inisialkey.fieldmonitoring.BuildConfig
import com.inisialkey.fieldmonitoring.R
import com.inisialkey.fieldmonitoring.core.ui.coordinates
import com.inisialkey.fieldmonitoring.core.ui.formatDateTime
import com.inisialkey.fieldmonitoring.core.ui.iconRes
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
        location.placeName != null -> location.placeName.split(", ").take(2).joinToString(", ")
        note.isNotEmpty() -> note
        else -> location.coordinates()
    }
}

@Composable
fun ReportCard(report: Report, onClick: () -> Unit, modifier: Modifier = Modifier, showReporter: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    val category = report.category
    AppCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box {
                PhotoThumbnail(report.photos.firstOrNull(), Modifier.size(88.dp))
                if (category != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                            .size(28.dp)
                            .background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painterResource(category.iconRes()),
                            contentDescription = null,
                            tint = Color(0xFF016F33),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (category != null) {
                    Text(
                        stringResource(category.labelRes()),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.primary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = report.title.ifBlank { stringResource(R.string.report_untitled) },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (report.syncState == SyncState.DRAFT && report.lastError != null) {
                    Text(report.lastError, style = MaterialTheme.typography.bodySmall, color = colors.error)
                }
                if (showReporter && report.reporterName != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Avatar(report.reporterName, size = 24.dp)
                        Text(report.reporterName, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                IconText(R.drawable.ic_location_on, locationSummary(report), maxLines = 2)
            }
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), color = colors.outlineVariant)
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (!showReporter) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SyncStatusChip(report)
                    if (report.syncState == SyncState.SYNCED) EvaluationBadge(report.evaluation)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconText(R.drawable.ic_event, formatDateTime(report.incidentAt, report.incidentOffsetMinutes), Modifier.weight(1f))
                IconText(R.drawable.ic_image, stringResource(R.string.photo_count, report.photos.size))
            }
        }
        if (report.syncState == SyncState.SYNCING && report.photos.isNotEmpty()) {
            LinearProgressIndicator(
                progress = { report.photos.count { it.uploaded }.toFloat() / report.photos.size },
                strokeCap = StrokeCap.Round,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
            )
        }
    }
}
