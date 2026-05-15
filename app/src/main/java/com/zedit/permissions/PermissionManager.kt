package com.zedit.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat

/**
 * Returns the required video read permission string based on API level:
 * - API 33+ (TIRAMISU): [Manifest.permission.READ_MEDIA_VIDEO]
 * - API 28-32: [Manifest.permission.READ_EXTERNAL_STORAGE]
 */
fun getVideoPermission(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
}

/**
 * Checks whether the appropriate video read permission is already granted.
 *
 * Uses [ContextCompat.checkSelfPermission] to avoid making a system call
 * when the permission has already been granted.
 */
fun isVideoPermissionGranted(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        getVideoPermission()
    ) == PackageManager.PERMISSION_GRANTED
}

/**
 * Creates and remembers an [ActivityResultLauncher] for requesting the
 * video read permission appropriate for the current API level.
 *
 * @param onGranted Called when the user grants the permission.
 * @param onDenied  Called when the user denies the permission.
 * @return An [ActivityResultLauncher] that must be launched with the
 *         permission string obtained from [getVideoPermission].
 */
@Composable
fun rememberVideoPermissionLauncher(
    onGranted: () -> Unit,
    onDenied: () -> Unit
): ActivityResultLauncher<String> {
    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) onGranted()
        else onDenied()
    }
}
