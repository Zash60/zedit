package com.zedit.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zedit.ui.theme.*
import kotlinx.coroutines.delay

/**
 * A modal bottom sheet that guides the user through the export workflow.
 *
 * Displays one of five states: [ExportUiState.Idle], [ExportUiState.Exporting],
 * [ExportUiState.Saving], [ExportUiState.Done], or [ExportUiState.Error].
 *
 * @param state        The current export UI state.
 * @param onExport     Called when the user initiates export from the Idle screen.
 * @param onCancel     Called when the user cancels an in-progress export.
 * @param onDismiss    Called when the user closes a terminal (Done/Error) or Idle sheet.
 * @param onRetry      Called when the user retries after an error.
 * @param onOpenGallery Called when the user wants to open the gallery app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportDialog(
    state: ExportUiState,
    onExport: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onOpenGallery: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = {
            when (state) {
                is ExportUiState.Exporting,
                is ExportUiState.Saving -> { }
                else -> onDismiss()
            }
        },
        sheetState = sheetState,
        containerColor = DarkSurface,
        contentColor = OnDarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (state) {
                is ExportUiState.Idle -> IdleContent(onExport = onExport, onDismiss = onDismiss)
                is ExportUiState.Exporting -> ExportingContent(state = state, onCancel = onCancel)
                is ExportUiState.Saving -> SavingContent()
                is ExportUiState.Done -> DoneContent(
                    state = state,
                    onOpenGallery = onOpenGallery,
                    onDismiss = onDismiss
                )
                is ExportUiState.Error -> ErrorContent(
                    state = state,
                    onRetry = onRetry,
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@Composable
private fun IdleContent(
    onExport: () -> Unit,
    onDismiss: () -> Unit
) {
    Text(
        text = "Export Project",
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = OnDarkSurface
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = "Your project is ready to export. This will save the final video to your device\u2019s gallery.",
        fontSize = 14.sp,
        color = OnDarkSurface.copy(alpha = 0.7f),
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(24.dp))
    Button(
        onClick = onExport,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
    ) {
        Text("Export")
    }
    Spacer(modifier = Modifier.height(8.dp))
    TextButton(onClick = onDismiss) {
        Text("Cancel", color = OnDarkSurface.copy(alpha = 0.7f))
    }
}

@Composable
private fun ExportingContent(
    state: ExportUiState.Exporting,
    onCancel: () -> Unit
) {
    val progressPercent = (state.progress * 100).toInt()

    Text(
        text = "Exporting Video",
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = OnDarkSurface
    )
    Spacer(modifier = Modifier.height(24.dp))
    LinearProgressIndicator(
        progress = { state.progress },
        modifier = Modifier.fillMaxWidth(),
        color = AccentBlue,
        trackColor = DarkSurfaceVariant
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = "Exporting... $progressPercent%",
        fontSize = 14.sp,
        color = OnDarkSurface.copy(alpha = 0.7f)
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Processing...",
        fontSize = 12.sp,
        color = OnDarkSurface.copy(alpha = 0.5f)
    )
    Spacer(modifier = Modifier.height(24.dp))
    TextButton(onClick = onCancel) {
        Text("Cancel", color = OnDarkSurface.copy(alpha = 0.7f))
    }
}

@Composable
private fun SavingContent() {
    Text(
        text = "Saving to Gallery",
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = OnDarkSurface
    )
    Spacer(modifier = Modifier.height(24.dp))
    CircularProgressIndicator(
        modifier = Modifier.size(48.dp),
        strokeWidth = 4.dp,
        color = AccentBlue
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "Saving to gallery...",
        fontSize = 14.sp,
        color = OnDarkSurface.copy(alpha = 0.7f)
    )
}

@Composable
private fun DoneContent(
    state: ExportUiState.Done,
    onOpenGallery: () -> Unit,
    onDismiss: () -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current

    LaunchedEffect(state) {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        delay(3000)
        onDismiss()
    }

    Text(
        text = "\u2713",
        fontSize = 48.sp,
        color = SuccessGreen
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "Export Complete",
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = OnDarkSurface
    )
    Spacer(modifier = Modifier.height(8.dp))

    val fileName = state.uri.lastPathSegment ?: "Zedit_Export.mp4"
    Text(
        text = "Export saved to Movies/$fileName",
        fontSize = 14.sp,
        color = OnDarkSurface.copy(alpha = 0.7f),
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(24.dp))
    Button(
        onClick = onOpenGallery,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
    ) {
        Text("Open Gallery")
    }
    Spacer(modifier = Modifier.height(8.dp))
    TextButton(onClick = onDismiss) {
        Text("Done", color = OnDarkSurface)
    }
}

@Composable
private fun ErrorContent(
    state: ExportUiState.Error,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    val userMessage = remember(state.message) {
        when {
            state.message.contains("Failed to open output stream") ||
            state.message.contains("insert returned null") ->
                "Unable to save file. Please check your device storage and try again."

            state.message.contains("codec", ignoreCase = true) ->
                "This device doesn\u2019t support the required video codec for export."

            state.message.contains("not found", ignoreCase = true) ||
            state.message.contains("NotFoundException") ->
                "One or more media files could not be found. They may have been moved or deleted."

            else -> "Export failed: ${state.message}. Please try again."
        }
    }

    Text(
        text = "\u2717",
        fontSize = 48.sp,
        color = PlayheadRed
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "Export Failed",
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = OnDarkSurface
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = userMessage,
        fontSize = 14.sp,
        color = OnDarkSurface.copy(alpha = 0.7f),
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(24.dp))
    Button(
        onClick = onRetry,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
    ) {
        Text("Try Again")
    }
    Spacer(modifier = Modifier.height(8.dp))
    TextButton(onClick = onDismiss) {
        Text("Cancel", color = OnDarkSurface.copy(alpha = 0.7f))
    }
}
