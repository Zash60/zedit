package com.zedit.ui.editor.timeline

data class TimelineState(
    val projectId: Long = 0,
    val projectName: String = "",
    val tracks: List<TrackState> = emptyList(),
    val playheadPositionMs: Long = 0L,
    val zoomLevel: Float = 10f,  // pixels per second
    val selectedClipId: Long? = null,
    val isPlaying: Boolean = false,
    val projectDurationMs: Long = 0L
)

data class TrackState(
    val id: Long,
    val name: String,
    val type: TrackType,
    val clips: List<ClipState> = emptyList(),
    val sortOrder: Int,
    val isMuted: Boolean = false
)

data class ClipState(
    val id: Long,
    val trackId: Long,
    val sourceUri: String,
    val startPositionMs: Long,
    val trimInMs: Long,
    val trimOutMs: Long,
    val speed: Float,
    val sourceDurationMs: Long = 0L  // full source duration (for clamping)
) {
    /**
     * Effective duration of this clip on the timeline, accounting for speed.
     */
    val durationMs: Long
        get() = if (speed > 0f) ((trimOutMs - trimInMs) / speed).toLong() else 0L

    val endPositionMs: Long
        get() = startPositionMs + durationMs
}

enum class TrackType {
    VIDEO, AUDIO
}
