package com.inisialkey.fieldmonitoring.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.inisialkey.fieldmonitoring.R
import com.inisialkey.fieldmonitoring.report.domain.Photo

private val Scrim = Color.Black.copy(alpha = 0.55f)

@Composable
fun PhotoPager(photos: List<Photo>, onOpen: (Photo) -> Unit, modifier: Modifier = Modifier) {
    val pagerState = rememberPagerState { photos.size }
    val openLabel = stringResource(R.string.open_photo)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        if (photos.isEmpty()) {
            Icon(
                painterResource(R.drawable.ic_image),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val photo = photos[page]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClickLabel = openLabel) { onOpen(photo) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_image), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                AsyncImage(
                    model = photo.imageModel(),
                    contentDescription = stringResource(R.string.photo_page, page + 1, photos.size),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        if (photos.size > 1) {
            Text(
                "${pagerState.currentPage + 1}/${photos.size}",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .background(Scrim, CircleShape)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .clearAndSetSemantics {},
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(12.dp)
                    .background(Scrim, CircleShape)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .clearAndSetSemantics {},
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(photos.size) { index ->
                    val current = index == pagerState.currentPage
                    Box(
                        Modifier
                            .size(if (current) 8.dp else 6.dp)
                            .background(Color.White.copy(alpha = if (current) 1f else 0.6f), CircleShape),
                    )
                }
            }
        }
    }
}

@Composable
fun FullScreenPhoto(photo: Photo, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            AsyncImage(
                model = photo.imageModel(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .safeDrawingPadding()
                    .align(Alignment.TopEnd),
            ) {
                Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.action_close), tint = Color.White)
            }
        }
    }
}
