package com.inisialkey.fieldmonitoring.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.inisialkey.fieldmonitoring.R
import com.inisialkey.fieldmonitoring.report.domain.Photo

@Composable
fun PhotoGrid(
    photos: List<Photo>,
    modifier: Modifier = Modifier,
    canAdd: Boolean = false,
    processing: Boolean = false,
    onAdd: () -> Unit = {},
    onRemove: ((Photo) -> Unit)? = null,
    onOpen: ((Photo) -> Unit)? = null,
) {
    val tiles: List<Photo?> = if (canAdd) photos + null else photos
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tiles.chunked(COLUMNS).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { photo ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                    ) {
                        if (photo == null) {
                            AddPhotoTile(processing, onAdd)
                        } else {
                            PhotoTile(photo, photos.indexOf(photo) + 1, onRemove, onOpen)
                        }
                    }
                }
                repeat(COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun PhotoTile(photo: Photo, number: Int, onRemove: ((Photo) -> Unit)?, onOpen: ((Photo) -> Unit)?) {
    val description = stringResource(R.string.photo_description, number)
    Box(modifier = Modifier.fillMaxSize()) {
        if (onOpen != null) {
            Surface(onClick = { onOpen(photo) }, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxSize()) {
                PhotoThumbnail(photo, Modifier.fillMaxSize(), contentDescription = description)
            }
        } else {
            PhotoThumbnail(photo, Modifier.fillMaxSize(), contentDescription = description)
        }
        if (onRemove != null) {
            IconButton(onClick = { onRemove(photo) }, modifier = Modifier.align(Alignment.TopEnd)) {
                Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.6f), contentColor = Color.White) {
                    Icon(
                        painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.remove_photo, number),
                        modifier = Modifier
                            .padding(4.dp)
                            .size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AddPhotoTile(processing: Boolean, onAdd: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val dash = colors.primary.copy(alpha = 0.6f)
    Surface(
        onClick = onAdd,
        enabled = !processing,
        shape = MaterialTheme.shapes.medium,
        color = colors.primaryContainer.copy(alpha = 0.45f),
        contentColor = colors.primary,
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawRoundRect(
                    color = dash,
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))),
                )
            },
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (processing) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Icon(painterResource(R.drawable.ic_add_a_photo), contentDescription = null)
                Text(
                    stringResource(R.string.add_photo),
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

private const val COLUMNS = 3
