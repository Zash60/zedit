package com.zedit.ui.editor

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zedit.data.media.MediaStoreSaver
import com.zedit.engine.ExportEngine
import com.zedit.ui.editor.timeline.TrackState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI-friendly sealed class representing the state of an export operation.
 */
sealed class ExportUiState {
    data object Idle : ExportUiState()
    data class Exporting(val progress: Float) : ExportUiState()
    data object Saving : ExportUiState()
    data class Done(val uri: Uri) : ExportUiState()
    data class Error(val message: String) : ExportUiState()
}

/**
 * ViewModel for the export workflow.
 *
 * Observes [ExportEngine.exportState], displays progress via [uiState],
 * and delegates the MediaStore copy step to [MediaStoreSaver] once the
 * engine has finished writing the temp file.
 */
@HiltViewModel
class ExportViewModel @Inject constructor(
    private val exportEngine: ExportEngine,
    private val mediaStoreSaver: MediaStoreSaver
) : ViewModel() {

    private val _uiState = MutableStateFlow<ExportUiState>(ExportUiState.Idle)
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    private var currentOutputUri: Uri? = null

    /**
     * Starts an export for [projectId] with the given timeline [tracks].
     * Emits [ExportUiState.Exporting] / [ExportUiState.Saving] / [ExportUiState.Done]
     * / [ExportUiState.Error] through [uiState].
     */
    fun startExport(projectId: Long, tracks: List<TrackState>) {
        _uiState.value = ExportUiState.Exporting(0f)

        viewModelScope.launch {
            exportEngine.exportState.collect { state ->
                when (state) {
                    is ExportEngine.ExportState.Idle -> {
                    }
                    is ExportEngine.ExportState.Progress -> {
                        _uiState.value = ExportUiState.Exporting(state.progress)
                    }
                    is ExportEngine.ExportState.Completed -> {
                        _uiState.value = ExportUiState.Saving
                        currentOutputUri = state.outputUri
                        try {
                            val file = java.io.File(state.outputUri.path!!)
                            val mediaStoreUri = mediaStoreSaver.saveToMediaStore(file)
                            _uiState.value = ExportUiState.Done(mediaStoreUri)
                        } catch (e: Exception) {
                            _uiState.value = ExportUiState.Error(
                                e.message ?: "Failed to save to gallery"
                            )
                        }
                        return@collect
                    }
                    is ExportEngine.ExportState.Error -> {
                        _uiState.value = ExportUiState.Error(state.message)
                    }
                }
            }
        }

        viewModelScope.launch {
            exportEngine.exportProject(projectId, tracks)
        }
    }

    /**
     * Cancels the current export and returns to [ExportUiState.Idle].
     */
    fun cancelExport() {
        exportEngine.cancelExport()
        _uiState.value = ExportUiState.Idle
        currentOutputUri = null
    }

    /**
     * Resets the UI state back to [ExportUiState.Idle].
     */
    fun resetState() {
        _uiState.value = ExportUiState.Idle
        currentOutputUri = null
    }
}
