package com.zedit.ui.editor.timeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlin.math.abs


@Composable
fun TimelineCanvas(
    state: TimelineState,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onZoomToFit: () -> Unit,
    onSelectClip: (Long?) -> Unit,
    onPlayheadDrag: (Long) -> Unit,
    onTrimCommit: (Long, Long, Long) -> Unit,
    onSplit: () -> Unit,
    onMerge: () -> Unit,
    onSpeedChange: (Long, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val scrollState = rememberScrollState()

    val canMerge = state.selectedClipId != null && state.tracks.any { track ->
        track.clips.any { it.id == state.selectedClipId } && track.clips.size >= 2
    }

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
                onSelectClip = onSelectClip,
                onPlayheadDrag = onPlayheadDrag,
                onTrimCommit = onTrimCommit,
                modifier = Modifier.weight(1f)
            )
        }

        BottomControls(
            zoomLevel = state.zoomLevel,
            selectedClipId = state.selectedClipId,
            canMerge = canMerge,
            onZoomIn = onZoomIn,
            onZoomOut = onZoomOut,
            onZoomToFit = onZoomToFit,
            onSplit = onSplit,
            onMerge = onMerge,
            onSpeed = {}
        )
    }

}


@Composable
private fun TimelineContent(
    state: TimelineState,
    textMeasurer: TextMeasurer,
    scrollState: ScrollState,
    onSelectClip: (Long?) -> Unit,
    onPlayheadDrag: (Long) -> Unit,
    onTrimCommit: (Long, Long, Long) -> Unit,
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
                textMeasurer = textMeasurer,
                onPlayheadDrag = onPlayheadDrag
            )

            state.tracks.forEach { track ->
                TrackLaneCanvas(
                    track = track,
                    state = state,
                    width = contentWidthDp,
                    textMeasurer = textMeasurer,
                    onSelectClip = onSelectClip,
                    onTrimCommit = onTrimCommit
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
    textMeasurer: TextMeasurer,
    onPlayheadDrag: (Long) -> Unit
) {
    Canvas(
        modifier = Modifier
            .width(width)
            .height(32.dp)
            .pointerInput(state.zoomLevel) {
                detectTapGestures { offset ->
                    val positionMs = (offset.x * 1000f / state.zoomLevel).toLong()
                        .coerceIn(0, state.projectDurationMs)
                    onPlayheadDrag(positionMs)
                }
            }
            .pointerInput(state.zoomLevel) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        val positionMs = (offset.x * 1000f / state.zoomLevel).toLong()
                            .coerceIn(0, state.projectDurationMs)
                        onPlayheadDrag(positionMs)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val positionMs = (change.position.x * 1000f / state.zoomLevel).toLong()
                            .coerceIn(0, state.projectDurationMs)
                        onPlayheadDrag(positionMs)
                    }
                )
            }
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


private class TrimDragState(
    val clipId: Long,
    val edge: TrimEdge,
    val originalTrimInMs: Long,
    val originalTrimOutMs: Long,
    val startX: Float,
    val clipSpeed: Float
)

private enum class TrimEdge { LEFT, RIGHT }

@Composable
private fun TrackLaneCanvas(
    track: TrackState,
    state: TimelineState,
    width: Dp,
    textMeasurer: TextMeasurer,
    onSelectClip: (Long?) -> Unit,
    onTrimCommit: (Long, Long, Long) -> Unit
) {
    val density = LocalDensity.current
    val edgeThresholdPx = with(density) { 12.dp.toPx() }
    val zoomLevel = state.zoomLevel

    var trimPreview by remember { mutableStateOf<TrimDragState?>(null) }
    var previewTrimInMs by remember { mutableLongStateOf(0L) }
    var previewTrimOutMs by remember { mutableLongStateOf(0L) }

    Canvas(
        modifier = Modifier
            .width(width)
            .height(64.dp)
            .pointerInput(zoomLevel, state.selectedClipId, track) {
                detectTapGestures { offset ->
                    val clickedClip = track.clips.find { clip ->
                        val clipX = clip.startPositionMs * zoomLevel / 1000f
                        val clipW = clip.durationMs * zoomLevel / 1000f
                        offset.x in clipX..(clipX + clipW)
                    }
                    onSelectClip(clickedClip?.id)
                }
            }
            .pointerInput(zoomLevel, state.selectedClipId, edgeThresholdPx, track) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        val selectedClip = track.clips.firstOrNull { it.id == state.selectedClipId }
                            ?: return@detectDragGestures

                        val clipX = selectedClip.startPositionMs * zoomLevel / 1000f
                        val clipW = selectedClip.durationMs * zoomLevel / 1000f
                        val clipRight = clipX + clipW

                        when {
                            abs(startOffset.x - clipX) < edgeThresholdPx -> {
                                trimPreview = TrimDragState(
                                    clipId = selectedClip.id,
                                    edge = TrimEdge.LEFT,
                                    originalTrimInMs = selectedClip.trimInMs,
                                    originalTrimOutMs = selectedClip.trimOutMs,
                                    startX = startOffset.x,
                                    clipSpeed = selectedClip.speed
                                )
                                previewTrimInMs = selectedClip.trimInMs
                                previewTrimOutMs = selectedClip.trimOutMs
                            }
                            abs(startOffset.x - clipRight) < edgeThresholdPx -> {
                                trimPreview = TrimDragState(
                                    clipId = selectedClip.id,
                                    edge = TrimEdge.RIGHT,
                                    originalTrimInMs = selectedClip.trimInMs,
                                    originalTrimOutMs = selectedClip.trimOutMs,
                                    startX = startOffset.x,
                                    clipSpeed = selectedClip.speed
                                )
                                previewTrimInMs = selectedClip.trimInMs
                                previewTrimOutMs = selectedClip.trimOutMs
                            }
                        }
                    },
                    onDrag = { change, _ ->
                        val drag = trimPreview ?: return@detectDragGestures
                        change.consume()

                        val deltaPx = change.position.x - drag.startX
                        val deltaMs = (deltaPx * 1000f / zoomLevel * drag.clipSpeed).toLong()

                        when (drag.edge) {
                            TrimEdge.LEFT -> {
                                previewTrimInMs = (drag.originalTrimInMs + deltaMs)
                                    .coerceIn(0, drag.originalTrimOutMs - 100)
                                previewTrimOutMs = drag.originalTrimOutMs
                            }
                            TrimEdge.RIGHT -> {
                                previewTrimOutMs = (drag.originalTrimOutMs + deltaMs)
                                    .coerceAtLeast(drag.originalTrimInMs + 100)
                                previewTrimInMs = drag.originalTrimInMs
                            }
                        }
                    },
                    onDragEnd = {
                        val drag = trimPreview ?: return@detectDragGestures
                        onTrimCommit(drag.clipId, previewTrimInMs, previewTrimOutMs)
                        trimPreview = null
                    },
                    onDragCancel = {
                        trimPreview = null
                    }
                )
            }
    ) {
        drawRect(color = TrackBackground, size = size)

        val clipColor = when (track.type) {
            TrackType.VIDEO -> ClipVideoColor
            TrackType.AUDIO -> ClipAudioColor
        }

        for (clip in track.clips) {
            val isSelected = clip.id == state.selectedClipId
            val isTrimming = trimPreview?.clipId == clip.id

            if (isTrimming) {
                drawClip(
                    clip = clip,
                    zoomLevel = zoomLevel,
                    isSelected = true,
                    laneHeight = size.height,
                    clipColor = clipColor,
                    textMeasurer = textMeasurer,
                    overrideTrimInMs = previewTrimInMs,
                    overrideTrimOutMs = previewTrimOutMs
                )

                val trimX = if (trimPreview?.edge == TrimEdge.LEFT) {
                    clip.startPositionMs * zoomLevel / 1000f
                } else {
                    clip.startPositionMs * zoomLevel / 1000f +
                        ((previewTrimOutMs - previewTrimInMs) / clip.speed * zoomLevel / 1000f)
                }.coerceIn(0f, size.width)

                val path = Path().apply {
                    moveTo(trimX, 0f)
                    lineTo(trimX, size.height)
                }
                drawPath(
                    path = path,
                    color = PlayheadRed.copy(alpha = 0.5f),
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(6.dp.toPx(), 4.dp.toPx()), 0f
                        )
                    )
                )
            } else {
                drawClip(
                    clip = clip,
                    zoomLevel = zoomLevel,
                    isSelected = isSelected,
                    laneHeight = size.height,
                    clipColor = clipColor,
                    textMeasurer = textMeasurer
                )
            }
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
    textMeasurer: TextMeasurer,
    overrideTrimInMs: Long? = null,
    overrideTrimOutMs: Long? = null
) {
    val effectiveTrimInMs = overrideTrimInMs ?: clip.trimInMs
    val effectiveTrimOutMs = overrideTrimOutMs ?: clip.trimOutMs
    val effectiveDurationMs = if (clip.speed > 0f) {
        ((effectiveTrimOutMs - effectiveTrimInMs) / clip.speed).toLong()
    } else 0L

    val clipX = clip.startPositionMs * zoomLevel / 1000f
    val clipWidth = effectiveDurationMs * zoomLevel / 1000f
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

    val durationSec = effectiveDurationMs / 1000f
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
private fun BottomControls(
    zoomLevel: Float,
    selectedClipId: Long?,
    canMerge: Boolean,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onZoomToFit: () -> Unit,
    onSplit: () -> Unit,
    onMerge: () -> Unit,
    onSpeed: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onMerge,
                enabled = canMerge
            ) {
                Text(
                    text = "\u23F9",
                    fontSize = 16.sp,
                    color = if (canMerge) OnDarkSurface
                    else OnDarkSurface.copy(alpha = 0.38f)
                )
            }

            Text(
                text = "Merge",
                fontSize = 11.sp,
                color = if (canMerge) OnDarkSurface
                else OnDarkSurface.copy(alpha = 0.38f)
            )

            Spacer(modifier = Modifier.width(16.dp))

            IconButton(
                onClick = onSpeed,
                enabled = selectedClipId != null
            ) {
                Text(
                    text = "\u23F1",
                    fontSize = 16.sp,
                    color = if (selectedClipId != null) OnDarkSurface
                    else OnDarkSurface.copy(alpha = 0.38f)
                )
            }

            Text(
                text = "Speed",
                fontSize = 11.sp,
                color = if (selectedClipId != null) OnDarkSurface
                else OnDarkSurface.copy(alpha = 0.38f)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onSplit,
                enabled = selectedClipId != null
            ) {
                Text(
                    text = "\u2702",
                    fontSize = 16.sp,
                    color = if (selectedClipId != null) OnDarkSurface
                    else OnDarkSurface.copy(alpha = 0.38f)
                )
            }

            Text(
                text = "Split",
                fontSize = 11.sp,
                color = if (selectedClipId != null) OnDarkSurface
                else OnDarkSurface.copy(alpha = 0.38f)
            )
        }

        ZoomControls(
            zoomLevel = zoomLevel,
            onZoomIn = onZoomIn,
            onZoomOut = onZoomOut,
            onZoomToFit = onZoomToFit
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
