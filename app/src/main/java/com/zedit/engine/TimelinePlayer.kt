package com.zedit.engine

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.Effect
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.effect.SpeedChangeEffect
import androidx.media3.transformer.Composition
import androidx.media3.transformer.CompositionPlayer
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import com.zedit.ui.editor.timeline.ClipState
import com.zedit.ui.editor.timeline.TrackState
import com.zedit.ui.editor.timeline.TrackType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimelinePlayer @Inject constructor(
    @ApplicationContext private val context: Context
) {

    val exoPlayer: CompositionPlayer = CompositionPlayer.Builder(context).build()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private var positionUpdateJob: Job? = null
    private var playerScope: CoroutineScope? = null

    fun play() {
        exoPlayer.play()
        _isPlaying.value = true
        startPositionUpdates()
    }

    fun pause() {
        exoPlayer.pause()
        _isPlaying.value = false
        stopPositionUpdates()
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
        _currentPositionMs.value = positionMs
    }

    fun getCurrentPosition(): Long = exoPlayer.currentPosition

    fun isPlaying(): Boolean = exoPlayer.isPlaying

    fun release() {
        stopPositionUpdates()
        exoPlayer.release()
    }

    @Suppress("UnstableApiUsage")
    fun rebuildComposition(tracks: List<TrackState>) {
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        _currentPositionMs.value = 0L

        val composition = buildComposition(tracks)
        if (composition != null) {
            exoPlayer.setComposition(composition)
            exoPlayer.prepare()
            exoPlayer.seekTo(0)
        }
    }

    @Suppress("UnstableApiUsage")
    private fun buildComposition(tracks: List<TrackState>): Composition? {
        val videoTracks = tracks
            .filter { it.type == TrackType.VIDEO && !it.isMuted }
            .sortedBy { it.sortOrder }

        val videoClips = videoTracks
            .flatMap { track -> track.clips }
            .sortedBy { it.startPositionMs }

        val audioTracks = tracks
            .filter { it.type == TrackType.AUDIO && !it.isMuted }

        val audioClips = audioTracks
            .flatMap { it.clips }
            .sortedBy { it.startPositionMs }

        val videoItems = videoClips.map { clip -> buildEditedMediaItem(clip) }
        val audioItems = audioClips.map { clip -> buildEditedMediaItem(clip) }

        val sequences = mutableListOf<EditedMediaItemSequence>()

        if (videoItems.isNotEmpty()) {
            val videoSeqBuilder = EditedMediaItemSequence.Builder(videoItems.first())
            videoItems.drop(1).forEach { videoSeqBuilder.addItem(it) }
            sequences.add(videoSeqBuilder.build())
        }

        if (audioItems.isNotEmpty()) {
            val audioSeqBuilder = EditedMediaItemSequence.Builder(audioItems.first())
            audioItems.drop(1).forEach { audioSeqBuilder.addItem(it) }
            sequences.add(audioSeqBuilder.build())
        }

        if (sequences.isEmpty()) {
            return null
        }

        return Composition.Builder(sequences).build()
    }

    @Suppress("UnstableApiUsage")
    private fun buildEditedMediaItem(clip: ClipState): EditedMediaItem {
        val mediaItem = MediaItem.Builder()
            .setUri(Uri.parse(clip.sourceUri))
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(clip.trimInMs)
                    .setEndPositionMs(clip.trimOutMs)
                    .build()
            )
            .build()
        val builder = EditedMediaItem.Builder(mediaItem)
        if (Math.abs(clip.speed - 1.0f) > 0.01f) {
            builder.setEffects(Effects(mutableListOf<AudioProcessor>(), mutableListOf<Effect>(SpeedChangeEffect(clip.speed))))
        }
        return builder.build()
    }

    private fun startPositionUpdates() {
        stopPositionUpdates()
        val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        playerScope = scope
        positionUpdateJob = scope.launch {
            while (isActive) {
                _currentPositionMs.value = exoPlayer.currentPosition
                delay(100L)
            }
        }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        playerScope?.cancel()
        playerScope = null
        positionUpdateJob = null
    }
}
