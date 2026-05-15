package com.zedit.data.media

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.core.net.toFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Saves exported MP4 files to the public MediaStore Movies collection.
 *
 * This is the final step after [com.zedit.engine.ExportEngine] produces a temp file
 * in the app-internal cache directory.
 */
@Singleton
class MediaStoreSaver @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Copies [sourceFile] (a temp export from ExportEngine) into the public
     * MediaStore Movies directory and returns the content [Uri] for the saved entry.
     *
     * The source file is deleted on successful copy.
     *
     * @throws IOException if storage is full or the copy fails.
     */
    suspend fun saveToMediaStore(sourceFile: File): Uri = withContext(Dispatchers.IO) {
        val displayName = "Zedit_Export_${System.currentTimeMillis()}.mp4"
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Zedit")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val collectionUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        val insertedUri: Uri = resolver.insert(collectionUri, contentValues)
            ?: throw IOException("Failed to create MediaStore entry — insert returned null")

        try {
            resolver.openOutputStream(insertedUri)?.use { outputStream ->
                sourceFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: throw IOException("Failed to open output stream for $insertedUri")

            val updateValues = ContentValues().apply {
                put(MediaStore.Video.Media.IS_PENDING, 0)
            }
            resolver.update(insertedUri, updateValues, null, null)

            sourceFile.delete()
        } catch (e: Exception) {
            try {
                resolver.delete(insertedUri, null, null)
            } catch (_: Exception) {
            }
            throw e
        }

        insertedUri
    }
}
