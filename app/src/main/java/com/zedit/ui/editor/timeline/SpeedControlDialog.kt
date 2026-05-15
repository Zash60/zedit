package com.zedit.ui.editor.timeline

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.zedit.ui.theme.AccentBlue
import com.zedit.ui.theme.DarkSurface
import com.zedit.ui.theme.DarkSurfaceVariant
import com.zedit.ui.theme.OnDarkSurface
import kotlin.math.abs

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpeedControlDialog(
    currentSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var sliderValue by remember { mutableFloatStateOf(currentSpeed) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Speed",
                color = OnDarkSurface
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "%.2fx".format(sliderValue),
                    color = OnDarkSurface,
                    style = androidx.compose.material3.MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 0.25f..4.0f,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presets = listOf(0.25f, 0.5f, 1.0f, 1.5f, 2.0f, 4.0f)
                    presets.forEach { preset ->
                        val isActive = abs(sliderValue - preset) < 0.01f
                        SuggestionChip(
                            onClick = {
                                sliderValue = preset
                                onSpeedChange(preset)
                            },
                            label = {
                                Text(
                                    text = "%.2fx".format(preset),
                                    color = if (isActive) Color.White else OnDarkSurface
                                )
                            },
                            colors = if (isActive) {
                                SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = AccentBlue,
                                    labelColor = Color.White
                                )
                            } else {
                                SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = DarkSurfaceVariant,
                                    labelColor = OnDarkSurface
                                )
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSpeedChange(sliderValue)
                    onDismiss()
                }
            ) {
                Text("Apply", color = AccentBlue)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = OnDarkSurface)
            }
        },
        containerColor = DarkSurface,
        titleContentColor = OnDarkSurface,
        textContentColor = OnDarkSurface
    )
}
