package com.zedit.ui.editor.timeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zedit.ui.theme.*


@Composable
fun TimelineCanvas(
    state: TimelineState,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onZoomToFit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val scrollState = rememberScrollState()

    Column(modifier = modifier.background(TimelineBackground)) {
        if (state.projectDurationMs == 0L || state.tracks.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Tap + to add media",
                    color = RulerText,
                    fontSize = 14.sp
                )
            }
        } else {
            TimelineContent(
                state = state,
                textMeasurer = textMeasurer,
                scrollState = scrollState,
                modifier = Modifier.weight(1f)
            )
        }

        ZoomControls(
            zoomLevel = state.zoomLevel,
            onZoomIn = onZoomIn,
            onZoomOut = onZoomOut,
            onZoomToFit = onZoomToFit
        )
    }
}


@Composable
private fun TimelineContent(
    state: TimelineState,
    textMeasurer: TextMeasurer,
    scrollState: ScrollState,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    val contentWidthDp = with(density) {
        val px = state.projectDurationMs * state.zoomLevel / 1000f + 100.dp.toPx()
        px.toDp()
    }

    Row(modifier = modifier) {
        Column {
            Box(modifier = Modifier.size(80.dp, 32.dp))

            state.tracks.forEach { track ->
                TrackHeader(
                    track = track,
                    modifier = Modifier
                        .width(80.dp)
                        .height(64.dp)
                )
            }
        }

        Column(modifier = Modifier.horizontalScroll(scrollState)) {
            TimeRulerCanvas(
                state = state,
                width = contentWidthDp,
                textMeasurer = textMeasurer
            )

            state.tracks.forEach { track ->
                TrackLaneCanvas(
                    track = track,
                    state = state,
                    width = contentWidthDp,
                    textMeasurer = textMeasurer
                )
            }
        }
    }
}


@Composable
private fun TrackHeader(
    track: TrackState,
    modifier: Modifier = Modifier
) {
    val icon = when (track.type) {
        TrackType.VIDEO -> "\u25B6"
        TrackType.AUDIO -> "\u266A"
    }
    val iconColor = when (track.type) {
        TrackType.VIDEO -> VideoTrackBlue
        TrackType.AUDIO -> AudioTrackGreen
    }

    Box(
        modifier = modifier
            .padding(4.dp)
            .background(DarkSurfaceVariant, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = "$icon ${track.name}",
            color = iconColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}


@Composable
private fun TimeRulerCanvas(
    state: TimelineState,
    width: Dp,
    textMeasurer: TextMeasurer
) {
    Canvas(
        modifier = Modifier
            .width(width)
            .height(32.dp)
    ) {
        drawRect(color = RulerColor, size = size)

        val zoom = state.zoomLevel
        val totalSeconds = (state.projectDurationMs / 1000L).toInt()
        val rulerHeight = size.height

        for (second in 0..totalSeconds) {
            val x = second * zoom
            val isMajor = second % 5 == 0
            val tickHeight = if (isMajor) 4.dp.toPx() else 2.dp.toPx()
            val tickColor = if (isMajor) Color.White else RulerText

            drawRect(
                color = tickColor,
                topLeft = Offset(x, rulerHeight - tickHeight),
                size = Size(1.dp.toPx(), tickHeight)
            )

            if (isMajor) {
                val label = formatSeconds(second)
                val labelResult = textMeasurer.measure(
                    text = label,
                    style = TextStyle(
                        fontSize = 10.sp,
                        color = RulerText
                    )
                )
                drawText(
                    textLayoutResult = labelResult,
                    topLeft = Offset(
                        x - labelResult.size.width / 2f,
                        rulerHeight - tickHeight - labelResult.size.height - 3.dp.toPx()
                    )
                )
            }
        }

        drawPlayhead(
            state = state,
            height = size.height,
            drawCircle = true
        )
    }
}


@Composable
private fun TrackLaneCanvas(
    track: TrackState,
    state: TimelineState,
    width: Dp,
    textMeasurer: TextMeasurer
) {
    Canvas(
        modifier = Modifier
            .width(width)
            .height(64.dp)
    ) {
        drawRect(color = TrackBackground, size = size)

        val clipColor = when (track.type) {
            TrackType.VIDEO -> ClipVideoColor
            TrackType.AUDIO -> ClipAudioColor
        }

        for (clip in track.clips) {
            drawClip(
                clip = clip,
                zoomLevel = state.zoomLevel,
                isSelected = clip.id == state.selectedClipId,
                laneHeight = size.height,
                clipColor = clipColor,
                textMeasurer = textMeasurer
            )
        }

        drawPlayhead(
            state = state,
            height = size.height,
            drawCircle = false
        )
    }
}


private fun DrawScope.drawClip(
    clip: ClipState,
    zoomLevel: Float,
    isSelected: Boolean,
    laneHeight: Float,
    clipColor: Color,
    textMeasurer: TextMeasurer
) {
    val clipX = clip.startPositionMs * zoomLevel / 1000f
    val clipWidth = clip.durationMs * zoomLevel / 1000f
    val padding = 4.dp.toPx()
    val clipHeight = laneHeight - 2 * padding
    val clipY = padding
    val cornerRadius = CornerRadius(4.dp.toPx())

    if (clipWidth <= 0f) return

    drawRoundRect(
        color = clipColor,
        topLeft = Offset(clipX, clipY),
        size = Size(clipWidth, clipHeight),
        cornerRadius = cornerRadius
    )

    if (isSelected) {
        drawRoundRect(
            color = SelectionGold,
            topLeft = Offset(clipX, clipY),
            size = Size(clipWidth, clipHeight),
            cornerRadius = cornerRadius,
            style = Stroke(width = 2.dp.toPx())
        )
    }

    val minTextWidth = 30.dp.toPx()
    if (clipWidth <= minTextWidth) return

    val label = "Clip ${clip.id}"
    val labelStyle = TextStyle(fontSize = 11.sp, color = Color.White)
    val labelResult = textMeasurer.measure(text = label, style = labelStyle)

    val durationSec = clip.durationMs / 1000f
    val durationStr = "%.1fs".format(durationSec)
    val durationStyle = TextStyle(fontSize = 9.sp, color = Color.White.copy(alpha = 0.7f))
    val durationResult = textMeasurer.measure(text = durationStr, style = durationStyle)

    val textBlockHeight = labelResult.size.height + 2.dp.toPx() + durationResult.size.height
    val textBlockY = clipY + (clipHeight - textBlockHeight) / 2f

    val labelX = clipX + (clipWidth - labelResult.size.width) / 2f
    drawText(
        textLayoutResult = labelResult,
        topLeft = Offset(labelX, textBlockY)
    )

    val durationX = clipX + (clipWidth - durationResult.size.width) / 2f
    drawText(
        textLayoutResult = durationResult,
        topLeft = Offset(durationX, textBlockY + labelResult.size.height + 2.dp.toPx())
    )
}


private fun DrawScope.drawPlayhead(
    state: TimelineState,
    height: Float,
    drawCircle: Boolean
) {
    val playheadX = state.playheadPositionMs * state.zoomLevel / 1000f

    if (playheadX < 0f || playheadX > size.width) return

    drawLine(
        color = PlayheadRed,
        start = Offset(playheadX, 0f),
        end = Offset(playheadX, height),
        strokeWidth = 2.dp.toPx()
    )

    if (drawCircle) {
        drawCircle(
            color = PlayheadRed,
            radius = 3.dp.toPx(),
            center = Offset(playheadX, 3.dp.toPx())
        )
    }
}


@Composable
private fun ZoomControls(
    zoomLevel: Float,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onZoomToFit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onZoomOut,
            enabled = zoomLevel > 2f
        ) {
            Text(
                text = "\u2212",
                fontSize = 18.sp,
                color = if (zoomLevel > 2f) OnDarkSurface
                else OnDarkSurface.copy(alpha = 0.38f)
            )
        }

        Text(
            text = "%.1f px/s".format(zoomLevel),
            color = OnDarkSurface,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        IconButton(onClick = onZoomToFit) {
            Text(
                text = "\u25A3",
                fontSize = 16.sp,
                color = OnDarkSurface
            )
        }

        IconButton(
            onClick = onZoomIn,
            enabled = zoomLevel < 500f
        ) {
            Text(
                text = "+",
                fontSize = 18.sp,
                color = if (zoomLevel < 500f) OnDarkSurface
                else OnDarkSurface.copy(alpha = 0.38f)
            )
        }
    }
}


private fun formatSeconds(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
