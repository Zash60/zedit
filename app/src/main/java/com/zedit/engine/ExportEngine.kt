package com.zedit.engine

import android.content.Context
import android.net.Uri
import androidx.media3.common.ClippingConfiguration
import androidx.media3.common.Composition
import androidx.media3.common.EditedMediaItem
import androidx.media3.common.EditedMediaItemSequence
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.effect.SpeedChangeEffect
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.zedit.ui.editor.timeline.ClipState
import com.zedit.ui.editor.timeline.TrackState
import com.zedit.ui.editor.timeline.TrackType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Handles exporting a timeline project to an H.264 MP4 file using Media3 Transformer.
 *
 * Provides progress tracking via [exportState] (a [StateFlow] of [ExportState]),
 * cancellation support, and error handling. Hilt-injectable singleton.
 */
@Singleton
class ExportEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    private var currentTransformer: Transformer? = null
    private var outputFile: File? = null

    sealed class ExportState {
        data object Idle : ExportState()
        data class Progress(val progress: Float) : ExportState()
        data class Completed(val outputUri: Uri) : ExportState()
        data class Error(val message: String) : ExportState()
    }

    /**
     * Exports the given timeline [tracks] to an H.264 MP4 file stored in the
     * app-internal cache directory.
     *
     * @param projectId  Used to name the output file.
     * @param tracks     The flattened timeline tracks to export.
     * @param onComplete Callback invoked on the exporter thread when export finishes.
     */
    suspend fun exportProject(
        projectId: Long,
        tracks: List<TrackState>,
        onComplete: (Uri) -> Unit = {}
    ) {
        _exportState.value = ExportState.Progress(0f)

        try {
            // 1. Build video sequence from unmuted VIDEO tracks
            val videoClips = tracks
                .filter { it.type == TrackType.VIDEO && !it.isMuted }
                .flatMap { it.clips }
                .sortedBy { it.startPositionMs }

            val videoItems = videoClips.map { clip -> buildEditedMediaItem(clip) }
            val videoSequence = EditedMediaItemSequence(videoItems)

            // 2. Build audio sequence from all unmuted tracks
            val audioClips = tracks
                .filter { !it.isMuted }
                .flatMap { it.clips }
                .sortedBy { it.startPositionMs }

            val audioItems = audioClips.map { clip -> buildEditedMediaItem(clip) }
            val audioSequence = EditedMediaItemSequence(audioItems)

            // 3. Create Composition
            // First sequence = video (+ its audio), subsequent sequences = additional audio
            val composition = Composition.Builder(
                videoSequence,
                audioSequence
            ).build()

            // 4. Create output file in app-internal cache
            val outputDir = File(context.cacheDir, "exports")
            outputDir.mkdirs()
            val outputFile = File(
                outputDir,
                "zedit_export_${projectId}_${System.currentTimeMillis()}.mp4"
            )
            this.outputFile = outputFile

            // 5. Configure and create Transformer
            val transformer = Transformer.Builder(context)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .setAudioMimeType(MimeTypes.AUDIO_AAC)
                .build()
            this.currentTransformer = transformer

            // 6. Bridge the callback-based Transformer API into a suspend function
            suspendCancellableCoroutine<Unit> { continuation ->
                transformer.addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, result: ExportResult) {
                        val uri = Uri.fromFile(File(result.outputPath))
                        _exportState.value = ExportState.Completed(uri)
                        onComplete(uri)
                        if (continuation.isActive) {
                            continuation.resume(Unit)
                        }
                    }

                    override fun onError(
                        composition: Composition,
                        result: ExportResult,
                        exception: ExportException
                    ) {
                        _exportState.value = ExportState.Error(
                            exception.message ?: "Unknown export error"
                        )
                        if (continuation.isActive) {
                            continuation.resume(Unit)
                        }
                    }
                })

                transformer.start(composition, outputFile.absolutePath)

                continuation.invokeOnCancellation {
                    transformer.cancel()
                }
            }
        } catch (e: Exception) {
            _exportState.value = ExportState.Error(
                e.message ?: "Export failed"
            )
        }
    }

    /**
     * Cancels the current export (if any) and resets the state to [ExportState.Idle].
     * The partial output file is deleted.
     */
    fun cancelExport() {
        currentTransformer?.cancel()
        currentTransformer = null
        outputFile?.delete()
        outputFile = null
        _exportState.value = ExportState.Idle
    }

    private fun buildEditedMediaItem(clip: ClipState): EditedMediaItem {
        val mediaItem = MediaItem.fromUri(Uri.parse(clip.sourceUri))

        val clippingConfiguration = ClippingConfiguration.Builder()
            .setStartPositionMs(clip.trimInMs)
            .setEndPositionMs(clip.trimOutMs)
            .build()

        val itemBuilder = EditedMediaItem.Builder(mediaItem)
            .setClippingConfiguration(clippingConfiguration)

        if (Math.abs(clip.speed - 1.0f) > 0.01f) {
            itemBuilder.setEffects(listOf(SpeedChangeEffect(clip.speed)))
        }

        return itemBuilder.build()
    }
}
