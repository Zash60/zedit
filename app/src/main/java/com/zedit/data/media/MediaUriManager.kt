package com.zedit.data.media

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaUriManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val contentResolver: ContentResolver = context.contentResolver

    /**
     * Creates an ACTION_OPEN_DOCUMENT intent for picking video files.
     * Supports multi-select.
     */
    fun createVideoPickerIntent(allowMultiple: Boolean = true): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "video/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple)
        }
    }

    /**
     * Persists read permission for the given URI so it survives app restarts.
     */
    fun persistUriPermission(uri: Uri) {
        contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    }

    /**
     * Extracts the display file name from a content URI.
     */
    fun getFileName(uri: Uri): String {
        var name = "Unknown"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }

    /**
     * Extracts the file size in bytes from a content URI.
     */
    fun getFileSize(uri: Uri): Long {
        var size = 0L
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (sizeIndex >= 0 && cursor.moveToFirst()) {
                size = cursor.getLong(sizeIndex)
            }
        }
        return size
    }

    /**
     * Processes result from the SAF picker and returns list of selected URIs.
     * Persists permissions for each URI.
     */
    fun processPickerResult(
        data: Intent?,
        persistPermissions: Boolean = true
    ): List<Uri> {
        val uris = mutableListOf<Uri>()

        data?.data?.let { uri ->
            uris.add(uri)
        }

        data?.clipData?.let { clipData ->
            for (i in 0 until clipData.itemCount) {
                clipData.getItemAt(i)?.uri?.let { uri ->
                    uris.add(uri)
                }
            }
        }

        if (persistPermissions) {
            uris.forEach { persistUriPermission(it) }
        }

        return uris
    }
}
