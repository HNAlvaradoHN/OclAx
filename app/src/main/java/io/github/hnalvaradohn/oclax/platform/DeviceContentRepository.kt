package io.github.hnalvaradohn.oclax.platform

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import android.webkit.MimeTypeMap
import io.github.hnalvaradohn.oclax.model.ContentType
import io.github.hnalvaradohn.oclax.model.contentTypeFor

data class DeviceFileInfo(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
    val byteSize: Long,
    val modifiedAt: Long,
    val relativePath: String?,
) {
    val type: ContentType
        get() = contentTypeFor(mimeType)
}

class DeviceContentRepository(context: Context) {
    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver

    fun hasBroadFileAccess(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                appContext,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }

    fun listFiles(): List<DeviceFileInfo> {
        if (!hasBroadFileAccess()) return emptyList()

        val volume = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.VOLUME_EXTERNAL
        } else {
            "external"
        }
        val collection = MediaStore.Files.getContentUri(volume)
        val projection = buildList {
            add(MediaStore.Files.FileColumns._ID)
            add(MediaStore.Files.FileColumns.DISPLAY_NAME)
            add(MediaStore.Files.FileColumns.MIME_TYPE)
            add(MediaStore.Files.FileColumns.SIZE)
            add(MediaStore.Files.FileColumns.DATE_MODIFIED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Files.FileColumns.RELATIVE_PATH)
            }
        }.toTypedArray()

        val result = mutableListOf<DeviceFileInfo>()

        resolver.query(
            collection,
            projection,
            null,
            null,
            MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC",
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val mimeIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)
            val sizeIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
            val dateIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_MODIFIED)
            val pathIndex = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor.getColumnIndex(MediaStore.Files.FileColumns.RELATIVE_PATH)
            } else {
                -1
            }

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val displayName = cursor.getString(nameIndex)
                    ?.takeIf { it.isNotBlank() }
                    ?: continue
                val mime = if (mimeIndex >= 0 && !cursor.isNull(mimeIndex)) {
                    cursor.getString(mimeIndex)
                } else {
                    null
                }
                val mimeType = normalizeMimeType(mime, displayName)
                val size = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                    cursor.getLong(sizeIndex).coerceAtLeast(0L)
                } else {
                    0L
                }
                val modifiedAt = if (dateIndex >= 0 && !cursor.isNull(dateIndex)) {
                    cursor.getLong(dateIndex).coerceAtLeast(0L) * 1000L
                } else {
                    0L
                }
                val relativePath = if (pathIndex >= 0 && !cursor.isNull(pathIndex)) {
                    cursor.getString(pathIndex)
                } else {
                    null
                }

                result += DeviceFileInfo(
                    id = id,
                    uri = ContentUris.withAppendedId(collection, id),
                    displayName = displayName,
                    mimeType = mimeType,
                    byteSize = size,
                    modifiedAt = modifiedAt,
                    relativePath = relativePath,
                )
            }
        }

        return result
    }

    private fun normalizeMimeType(value: String?, displayName: String): String {
        val candidate = value?.trim()?.lowercase().orEmpty()
        if (candidate.contains('/')) return candidate

        val extension = displayName
            .substringAfterLast('.', missingDelimiterValue = "")
            .lowercase()
            .takeIf { it.isNotBlank() }

        return extension
            ?.let { MimeTypeMap.getSingleton().getMimeTypeFromExtension(it) }
            ?: when (extension) {
                "apk" -> "application/vnd.android.package-archive"
                "md", "json", "xml", "csv", "log" -> "text/plain"
                else -> "application/octet-stream"
            }
    }
}
