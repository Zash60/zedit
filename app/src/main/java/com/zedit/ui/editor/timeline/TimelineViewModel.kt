package com.zedit.ui.editor.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zedit.data.model.ClipEntity
import com.zedit.data.repository.ProjectRepository
import com.zedit.engine.TimelinePlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val timelinePlayer: TimelinePlayer
) : ViewModel() {

    val player: TimelinePlayer get() = timelinePlayer

    private val _state = MutableStateFlow(TimelineState())
    val state: StateFlow<TimelineState> = _state.asStateFlow()

    private val undoStack = mutableListOf<TimelineState>()
    private val redoStack = mutableListOf<TimelineState>()
    private val maxHistory = 50

    private var currentProjectId: Long = 0L
    private var isRestoringState = false  // prevent saveState during undo/redo

    @OptIn(ExperimentalCoroutinesApi::class)
    fun loadProject(projectId: Long) {
        currentProjectId = projectId

        viewModelScope.launch {
            projectRepository.getProjectById(projectId)?.let { project ->
                _state.update { it.copy(projectName = project.name, projectDurationMs = project.durationMs) }
            }
        }

        // Observe tracks and clips, combine into TimelineState
        projectRepository.getTracksByProject(projectId)
            .flatMapLatest { tracks ->
                if (tracks.isEmpty()) {
                    flowOf(emptyList<Pair<TrackEntity, List<ClipEntity>>>())
                } else {
                    combine(tracks.map { track ->
                        projectRepository.getClipsByTrack(track.id).map { clips -> track to clips }
                    }) { it.toList() }
                }
            }
            .map { trackClipPairs ->
                val trackStates = trackClipPairs.map { (track, clips) ->
                    TrackState(
                        id = track.id,
                        name = track.name,
                        type = if (track.type == "video") TrackType.VIDEO else TrackType.AUDIO,
                        clips = clips.map { clip ->
                            ClipState(
                                id = clip.id,
                                trackId = clip.trackId,
                                sourceUri = clip.sourceUri,
                                startPositionMs = clip.startPositionMs,
                                trimInMs = clip.trimInMs,
                                trimOutMs = clip.trimOutMs,
                                speed = clip.speed
                            )
                        },
                        sortOrder = track.sortOrder,
                        isMuted = track.isMuted
                    )
                }
                val maxDuration = trackStates.maxOfOrNull { track ->
                    track.clips.maxOfOrNull { it.endPositionMs } ?: 0L
                } ?: 0L

                _state.update { current ->
                    current.copy(
                        tracks = trackStates,
                        projectDurationMs = maxDuration,
                        projectId = projectId
                    )
                }
                timelinePlayer.rebuildComposition(trackStates)
            }
            .launchIn(viewModelScope)
    }

    fun setPlayhead(positionMs: Long) {
        _state.update { it.copy(playheadPositionMs = positionMs.coerceAtLeast(0)) }
    }

    fun setZoom(zoom: Float) {
        _state.update { it.copy(zoomLevel = zoom.coerceIn(2f, 500f)) }
    }

    fun zoomIn() {
        _state.update { it.copy(zoomLevel = (it.zoomLevel * 1.25f).coerceAtMost(500f)) }
    }

    fun zoomOut() {
        _state.update { it.copy(zoomLevel = (it.zoomLevel / 1.25f).coerceAtLeast(2f)) }
    }

    fun zoomToFit(durationMs: Long, canvasWidthPx: Float) {
        if (durationMs > 0 && canvasWidthPx > 0) {
            val zoom = (canvasWidthPx * 1000f / durationMs).coerceIn(2f, 500f)
            _state.update { it.copy(zoomLevel = zoom) }
        }
    }

    fun selectClip(clipId: Long?) {
        _state.update { it.copy(selectedClipId = clipId) }
    }

    fun play() {
        timelinePlayer.play()
        _state.update { it.copy(isPlaying = true) }
    }

    fun pause() {
        timelinePlayer.pause()
        _state.update { it.copy(isPlaying = false) }
    }

    fun seekTo(positionMs: Long) {
        timelinePlayer.seekTo(positionMs)
        _state.update { it.copy(playheadPositionMs = positionMs) }
    }

    fun saveUndoState() {
        if (isRestoringState) return
        undoStack.add(_state.value)
        redoStack.clear()
        if (undoStack.size > maxHistory) {
            undoStack.removeAt(0)
        }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        redoStack.add(_state.value)
        isRestoringState = true
        val previousState = undoStack.removeLast()
        _state.value = previousState
        timelinePlayer.rebuildComposition(previousState.tracks)
        isRestoringState = false
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack.add(_state.value)
        isRestoringState = true
        val nextState = redoStack.removeLast()
        _state.value = nextState
        timelinePlayer.rebuildComposition(nextState.tracks)
        isRestoringState = false
    }

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun updateClipInDb(clip: ClipState) {
        viewModelScope.launch {
            projectRepository.updateClip(
                ClipEntity(
                    id = clip.id,
                    trackId = clip.trackId,
                    sourceUri = clip.sourceUri,
                    startPositionMs = clip.startPositionMs,
                    trimInMs = clip.trimInMs,
                    trimOutMs = clip.trimOutMs,
                    speed = clip.speed
                )
            )
        }
    }

    fun deleteClipFromDb(clipId: Long) {
        viewModelScope.launch {
            projectRepository.deleteClipById(clipId)
        }
    }

    fun addClipToTrack(
        trackId: Long,
        sourceUri: String,
        startPositionMs: Long,
        trimInMs: Long,
        trimOutMs: Long,
        speed: Float = 1.0f
    ) {
        viewModelScope.launch {
            saveUndoState()
            projectRepository.addClipToTrack(
                trackId = trackId,
                sourceUri = sourceUri,
                startPositionMs = startPositionMs,
                trimInMs = trimInMs,
                trimOutMs = trimOutMs,
                speed = speed
            )
        }
    }

    fun setPlayheadFromDrag(positionMs: Long) {
        _state.update { it.copy(playheadPositionMs = positionMs.coerceIn(0, it.projectDurationMs)) }
    }

    fun commitTrim(clipId: Long, newTrimIn: Long, newTrimOut: Long) {
        val state = _state.value
        val track = state.tracks.firstOrNull { t -> t.clips.any { it.id == clipId } } ?: return
        val clip = track.clips.first { it.id == clipId }

        val clampedTrimIn = newTrimIn.coerceAtLeast(0)
        val clampedTrimOut = newTrimOut.coerceAtLeast(clampedTrimIn + 100)

        saveUndoState()
        updateClipInDb(clip.copy(trimInMs = clampedTrimIn, trimOutMs = clampedTrimOut))
        selectClip(clipId)
    }

    fun splitClipAtPlayhead() {
        val state = _state.value
        val clipId = state.selectedClipId ?: return
        val playhead = state.playheadPositionMs

        val track = state.tracks.firstOrNull { t -> t.clips.any { it.id == clipId } } ?: return
        val clip = track.clips.first { it.id == clipId }

        if (playhead <= clip.startPositionMs || playhead >= clip.endPositionMs) return

        val splitOffsetMs = playhead - clip.startPositionMs
        val splitTrimOut = clip.trimInMs + (splitOffsetMs * clip.speed).toLong()
        val splitTrimIn = splitTrimOut

        saveUndoState()

        viewModelScope.launch {
            projectRepository.updateClip(
                ClipEntity(
                    id = clip.id,
                    trackId = clip.trackId,
                    sourceUri = clip.sourceUri,
                    startPositionMs = clip.startPositionMs,
                    trimInMs = clip.trimInMs,
                    trimOutMs = splitTrimOut,
                    speed = clip.speed
                )
            )

            projectRepository.addClipToTrack(
                trackId = clip.trackId,
                sourceUri = clip.sourceUri,
                startPositionMs = playhead,
                trimInMs = splitTrimIn,
                trimOutMs = clip.trimOutMs,
                speed = clip.speed
            )
        }
    }

    fun mergeClipsOnSelectedTrack() {
        val state = _state.value
        val clipId = state.selectedClipId ?: return

        val trackIndex = state.tracks.indexOfFirst { t -> t.clips.any { it.id == clipId } }
        if (trackIndex < 0) return
        val track = state.tracks[trackIndex]
        if (track.clips.size < 2) return

        saveUndoState()
        viewModelScope.launch {
            val sortedClips = track.clips.sortedBy { it.startPositionMs }
            var currentStart = sortedClips.first().startPositionMs
            for (clip in sortedClips) {
                projectRepository.updateClip(
                    ClipEntity(
                        id = clip.id,
                        trackId = clip.trackId,
                        sourceUri = clip.sourceUri,
                        startPositionMs = currentStart,
                        trimInMs = clip.trimInMs,
                        trimOutMs = clip.trimOutMs,
                        speed = clip.speed
                    )
                )
                currentStart += clip.durationMs
            }
        }
    }

    fun setClipSpeed(clipId: Long, newSpeed: Float) {
        val state = _state.value
        val track = state.tracks.firstOrNull { t -> t.clips.any { it.id == clipId } } ?: return
        val clip = track.clips.first { it.id == clipId }
        val clampedSpeed = newSpeed.coerceIn(0.25f, 4.0f)
        saveUndoState()
        updateClipInDb(clip.copy(speed = clampedSpeed))
    }

    override fun onCleared() {
        super.onCleared()
        timelinePlayer.release()
    }
}
