package com.inisialkey.fieldmonitoring.report.ui.form

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inisialkey.fieldmonitoring.R
import com.inisialkey.fieldmonitoring.core.ui.components.IconBadge
import com.inisialkey.fieldmonitoring.core.ui.coordinates
import com.inisialkey.fieldmonitoring.core.ui.formatDateTime
import com.inisialkey.fieldmonitoring.core.ui.theme.LocalAppColors
import com.inisialkey.fieldmonitoring.report.domain.GeoPoint
import kotlin.math.roundToInt

private const val LOW_ACCURACY_METERS = 50

@Composable
fun LocationField(
    status: LocationStatus,
    location: GeoPoint?,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        when (status) {
            LocationStatus.Idle -> {
                Text(
                    stringResource(R.string.location_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FilledTonalButton(onClick = onRequest) {
                    Icon(painterResource(R.drawable.ic_location_on), contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.location_get), modifier = Modifier.padding(start = 8.dp))
                }
            }
            LocationStatus.Searching -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Text(stringResource(R.string.location_searching), style = MaterialTheme.typography.bodyMedium)
            }
            LocationStatus.Acquired -> if (location != null) AcquiredLocation(location, onRequest)
            LocationStatus.Approximate -> {
                Text(stringResource(R.string.location_approximate), style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = onOpenSettings) { Text(stringResource(R.string.action_open_settings)) }
            }
            is LocationStatus.Unavailable -> {
                Text(stringResource(R.string.location_unavailable), style = MaterialTheme.typography.bodyMedium)
                if (status.needsSettings) {
                    OutlinedButton(onClick = onOpenSettings) { Text(stringResource(R.string.action_open_settings)) }
                } else {
                    OutlinedButton(onClick = onRequest) { Text(stringResource(R.string.action_retry)) }
                }
            }
        }
    }
}

@Composable
private fun AcquiredLocation(location: GeoPoint, onRefresh: () -> Unit) {
    val accuracy = location.accuracyM.roundToInt()
    val lowAccuracy = accuracy > LOW_ACCURACY_METERS
    val app = LocalAppColors.current
    val container = if (lowAccuracy) app.warningContainer else MaterialTheme.colorScheme.primaryContainer
    val content = if (lowAccuracy) app.onWarningContainer else MaterialTheme.colorScheme.onPrimaryContainer
    Surface(shape = MaterialTheme.shapes.medium, color = container, contentColor = content, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconBadge(R.drawable.ic_location_on, container = content.copy(alpha = 0.12f), content = content)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(if (lowAccuracy) R.string.location_low_accuracy else R.string.location_accuracy, accuracy),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(location.coordinates(), style = MaterialTheme.typography.bodyMedium)
                Text(
                    stringResource(R.string.location_captured_at, formatDateTime(location.capturedAt)),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            TextButton(onClick = onRefresh, colors = ButtonDefaults.textButtonColors(contentColor = content)) {
                Text(stringResource(R.string.location_refresh), fontWeight = FontWeight.Bold)
            }
        }
    }
}
