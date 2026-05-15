package com.zedit.ui.editor.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zedit.data.model.ClipEntity
import com.zedit.data.model.TrackEntity
import com.zedit.data.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val projectRepository: ProjectRepository
) : ViewModel() {

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
            }
            .launchIn(viewModelScope)
    }

    fun setPlayhead(positionMs: Long) {
        _state.update { it.copy(playheadPositionMs = positionMs.coerceAtLeast(0)) }
    }

    fun setZoom(zoom: Float) {
        _state.update { it.copy(zoomLevel = zoom.coerceIn(2f, 500f)) }
    }

    fun selectClip(clipId: Long?) {
        _state.update { it.copy(selectedClipId = clipId) }
    }

    fun togglePlay() {
        _state.update { it.copy(isPlaying = !it.isPlaying) }
    }

    fun setPlaying(isPlaying: Boolean) {
        _state.update { it.copy(isPlaying = isPlaying) }
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
        isRestoringState = false
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack.add(_state.value)
        isRestoringState = true
        val nextState = redoStack.removeLast()
        _state.value = nextState
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
}
