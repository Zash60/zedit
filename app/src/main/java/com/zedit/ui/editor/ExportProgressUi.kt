package com.zedit.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * A Material3 dialog that reacts to [ExportUiState] and displays export progress,
 * success, or error feedback to the user.
 *
 * @param state  The current export UI state.
 * @param onCancel  Called when the user presses Cancel during export.
 * @param onDismiss Called when the user dismisses the dialog after success/error.
 * @param onRetry   Called when the user presses Try Again after an error.
 */
@Composable
fun ExportProgressUi(
    state: ExportUiState,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    when (state) {
        is ExportUiState.Idle -> {
        }

        is ExportUiState.Exporting -> {
            val progressPercent = (state.progress * 100).toInt()
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Exporting Video") },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = "Exporting... $progressPercent%",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = onCancel) {
                        Text("Cancel")
                    }
                }
            )
        }

        is ExportUiState.Saving -> {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Saving to Gallery") },
                text = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Saving to gallery...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                confirmButton = {}
            )
        }

        is ExportUiState.Done -> {
            LaunchedEffect(state) {
                delay(3000)
                onDismiss()
            }

            val fileName = state.uri.lastPathSegment ?: "Zedit_Export.mp4"
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Export Complete") },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "\u2713",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Export saved to Movies/$fileName",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = onDismiss) {
                        Text("Done")
                    }
                }
            )
        }

        is ExportUiState.Error -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Export Failed") },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "\u2717",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = onRetry) {
                        Text("Try Again")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
