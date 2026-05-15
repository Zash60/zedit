package com.zedit.ui.editor

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zedit.data.media.MediaUriManager
import com.zedit.ui.editor.timeline.*
import com.zedit.permissions.getVideoPermission
import com.zedit.permissions.isVideoPermissionGranted
import com.zedit.permissions.rememberVideoPermissionLauncher
import com.zedit.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    projectId: Long,
    onNavigateBack: () -> Unit,
    viewModel: TimelineViewModel = hiltViewModel(),
    exportViewModel: ExportViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val exportState by exportViewModel.uiState.collectAsState()
    val context = LocalContext.current

    val mediaUriManager = remember { MediaUriManager(context.applicationContext) }

    var showSpeedDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var selectedSpeedClipId by remember { mutableStateOf<Long?>(null) }
    var timelineWidth by remember { mutableFloatStateOf(1000f) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val hasClips = state.tracks.any { it.clips.isNotEmpty() }
    var showBackConfirmDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberVideoPermissionLauncher(
        onGranted = {
            val intent = mediaUriManager.createVideoPickerIntent(allowMultiple = true)
            videoPickerLauncher.launch(intent)
        },
        onDenied = {
            scope.launch {
                snackbarHostState.showSnackbar("Video access permission is needed to add media files.")
            }
        }
    )

    LaunchedEffect(projectId) {
        viewModel.loadProject(projectId)
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uris: List<Uri> = mediaUriManager.processPickerResult(result.data)
        if (uris.isNotEmpty()) {
            val firstVideoTrack = state.tracks.firstOrNull { it.type == TrackType.VIDEO }
            if (firstVideoTrack != null) {
                uris.forEach { uri ->
                    viewModel.addClipToTrack(
                        trackId = firstVideoTrack.id,
                        sourceUri = uri.toString(),
                        startPositionMs = state.projectDurationMs,
                        trimInMs = 0L,
                        trimOutMs = 30000L
                    )
                }
            }
        }
    }

    val canMerge = state.selectedClipId != null && state.tracks.any { track ->
        track.clips.any { it.id == state.selectedClipId } && track.clips.size >= 2
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.projectName.ifEmpty { "Zedit" },
                        fontWeight = FontWeight.Medium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text(
                            text = "\u2190",
                            fontSize = 20.sp,
                            color = OnDarkSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = OnDarkSurface
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxWidth()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (hasClips) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "\u25B6",
                            fontSize = 48.sp,
                            color = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Video Preview",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "+",
                            fontSize = 48.sp,
                            color = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Add media to get started",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            EditorToolbar(
                isPlaying = state.isPlaying,
                selectedClipId = state.selectedClipId,
                canMerge = canMerge,
                zoomLevel = state.zoomLevel,
                hasClips = hasClips,
                onPlayPause = {
                    if (state.isPlaying) viewModel.pause()
                    else viewModel.play()
                },
                onSplit = { viewModel.splitClipAtPlayhead() },
                onMerge = { viewModel.mergeClipsOnSelectedTrack() },
                onSpeed = {
                    state.selectedClipId?.let {
                        selectedSpeedClipId = it
                        showSpeedDialog = true
                    }
                },
                onZoomIn = { viewModel.zoomIn() },
                onZoomOut = { viewModel.zoomOut() },
                onZoomToFit = {
                    viewModel.zoomToFit(state.projectDurationMs, timelineWidth)
                },
                onUndo = { viewModel.undo() },
                onRedo = { viewModel.redo() },
                onExport = { showExportDialog = true },
                onAddMedia = {
                    if (isVideoPermissionGranted(context)) {
                        val intent = mediaUriManager.createVideoPickerIntent(allowMultiple = true)
                        videoPickerLauncher.launch(intent)
                    } else {
                        permissionLauncher.launch(getVideoPermission())
                    }
                }
            )

            Box(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxWidth()
                    .onSizeChanged { size ->
                        timelineWidth = size.width.toFloat()
                    }
            ) {
                TimelineCanvas(
                    state = state,
                    onZoomIn = { viewModel.zoomIn() },
                    onZoomOut = { viewModel.zoomOut() },
                    onZoomToFit = {
                        viewModel.zoomToFit(state.projectDurationMs, timelineWidth)
                    },
                    onSelectClip = { viewModel.selectClip(it) },
                    onPlayheadDrag = { viewModel.setPlayheadFromDrag(it) },
                    onTrimCommit = { clipId, trimIn, trimOut ->
                        viewModel.commitTrim(clipId, trimIn, trimOut)
                    },
                    onSplit = { viewModel.splitClipAtPlayhead() },
                    onMerge = { viewModel.mergeClipsOnSelectedTrack() },
                    onSpeedChange = { clipId, speed ->
                        viewModel.setClipSpeed(clipId, speed)
                    }
                )
            }
        }
    }

    BackHandler(enabled = showExportDialog && (exportState is ExportUiState.Exporting || exportState is ExportUiState.Saving)) {
        showBackConfirmDialog = true
    }

    if (showBackConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showBackConfirmDialog = false },
            title = { Text("Export in Progress") },
            text = { Text("An export is in progress. Leaving now will cancel it.") },
            confirmButton = {
                TextButton(onClick = {
                    showBackConfirmDialog = false
                    showExportDialog = false
                    exportViewModel.cancelExport()
                    onNavigateBack()
                }) {
                    Text("Leave")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackConfirmDialog = false }) {
                    Text("Stay")
                }
            }
        )
    }

    if (showSpeedDialog && selectedSpeedClipId != null) {
        val clip = state.tracks
            .flatMap { it.clips }
            .find { it.id == selectedSpeedClipId }
        if (clip != null) {
            SpeedControlDialog(
                currentSpeed = clip.speed,
                onSpeedChange = { newSpeed ->
                    viewModel.setClipSpeed(selectedSpeedClipId!!, newSpeed)
                },
                onDismiss = { showSpeedDialog = false }
            )
        } else {
            LaunchedEffect(Unit) { showSpeedDialog = false }
        }
    }

    if (showExportDialog) {
        ExportDialog(
            state = exportState,
            onExport = { exportViewModel.startExport(projectId, state.tracks) },
            onCancel = { exportViewModel.cancelExport(); showExportDialog = false },
            onDismiss = { exportViewModel.resetState(); showExportDialog = false },
            onRetry = {
                exportViewModel.resetState()
                exportViewModel.startExport(projectId, state.tracks)
            },
            onOpenGallery = { /* future: open gallery app */ }
        )
    }
}

@Composable
private fun EditorToolbar(
    isPlaying: Boolean,
    selectedClipId: Long?,
    canMerge: Boolean,
    zoomLevel: Float,
    hasClips: Boolean,
    onPlayPause: () -> Unit,
    onSplit: () -> Unit,
    onMerge: () -> Unit,
    onSpeed: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onZoomToFit: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onExport: () -> Unit,
    onAddMedia: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolbarIconButton(
                text = if (isPlaying) "\u23F8" else "\u25B6",
                label = if (isPlaying) "Pause" else "Play",
                enabled = true,
                onClick = onPlayPause
            )
            ToolbarIconButton(
                text = "\u2702",
                label = "Split",
                enabled = selectedClipId != null,
                onClick = onSplit
            )
            ToolbarIconButton(
                text = "\u23F9",
                label = "Merge",
                enabled = canMerge,
                onClick = onMerge
            )
            ToolbarIconButton(
                text = "\u23F1",
                label = "Speed",
                enabled = selectedClipId != null,
                onClick = onSpeed
            )
            ToolbarIconButton(
                text = "\u21A9",
                label = "Undo",
                enabled = true,
                onClick = onUndo
            )
            ToolbarIconButton(
                text = "\u21AA",
                label = "Redo",
                enabled = true,
                onClick = onRedo
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolbarIconButton(
                text = "\u2212",
                label = "Zoom Out",
                enabled = zoomLevel > 2f,
                onClick = onZoomOut
            )
            Text(
                text = "%.1f".format(zoomLevel),
                color = OnDarkSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            ToolbarIconButton(
                text = "\u25A3",
                label = "Fit",
                enabled = true,
                onClick = onZoomToFit
            )
            ToolbarIconButton(
                text = "+",
                label = "Zoom In",
                enabled = zoomLevel < 500f,
                onClick = onZoomIn
            )
            ToolbarIconButton(
                text = "+",
                label = "Add Media",
                enabled = true,
                onClick = onAddMedia
            )
            ToolbarIconButton(
                text = "\u2B06",
                label = "Export",
                enabled = hasClips,
                onClick = onExport
            )
        }
    }
}

@Composable
private fun ToolbarIconButton(
    text: String,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val contentAlpha = if (enabled) 1f else 0.38f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled
        ) {
            Text(
                text = text,
                fontSize = 18.sp,
                color = OnDarkSurface.copy(alpha = contentAlpha)
            )
        }
        Text(
            text = label,
            fontSize = 10.sp,
            color = OnDarkSurface.copy(alpha = contentAlpha),
            maxLines = 1
        )
    }
}
