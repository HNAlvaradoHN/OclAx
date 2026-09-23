package io.github.hnalvaradohn.oclax.platform

import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.app.RecoverableSecurityException
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import android.webkit.MimeTypeMap
import io.github.hnalvaradohn.oclax.model.ContentType
import io.github.hnalvaradohn.oclax.model.contentTypeFor

sealed interface DeviceDeleteResult {
    data object Deleted : DeviceDeleteResult

    data class NeedsUserConfirmation(
        val intentSender: IntentSender,
    ) : DeviceDeleteResult

    data class Failed(
        val reason: String?,
    ) : DeviceDeleteResult
}

data class DeviceFileInfo(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
    val byteSize: Long,
    val modifiedAt: Long,
    val relativePath: String?,
    val mediaStoreType: Int = 0,
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
            add(MediaStore.Files.FileColumns.MEDIA_TYPE)
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
            val mediaTypeIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE)
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
                val mediaStoreType = if (mediaTypeIndex >= 0 && !cursor.isNull(mediaTypeIndex)) {
                    cursor.getInt(mediaTypeIndex)
                } else {
                    0
                }

                result += DeviceFileInfo(
                    id = id,
                    uri = ContentUris.withAppendedId(collection, id),
                    displayName = displayName,
                    mimeType = mimeType,
                    byteSize = size,
                    modifiedAt = modifiedAt,
                    relativePath = relativePath,
                    mediaStoreType = mediaStoreType,
                )
            }
        }

        return result
    }

    fun requestDelete(file: DeviceFileInfo): DeviceDeleteResult {
        if (!hasBroadFileAccess()) {
            return DeviceDeleteResult.Failed("OclAx no tiene acceso a los archivos del dispositivo.")
        }

        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                requestDeleteAndroid11Plus(file)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                requestDeleteAndroid10(file)
            else ->
                deleteLegacy(file)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun requestDeleteAndroid11Plus(file: DeviceFileInfo): DeviceDeleteResult {
        var directDeleteError: Exception? = null

        try {
            val deleted = resolver.delete(file.uri, null, null)
            if (deleted > 0) {
                return DeviceDeleteResult.Deleted
            }
        } catch (error: Exception) {
            directDeleteError = error
        }

        val confirmationUri = mediaDeleteConfirmationUri(file)
            ?: return DeviceDeleteResult.Failed(
                directDeleteError?.message ?: "Android no permitió eliminar el archivo original.",
            )

        return runCatching {
            val pendingIntent = MediaStore.createDeleteRequest(
                resolver,
                listOf(confirmationUri),
            )
            DeviceDeleteResult.NeedsUserConfirmation(pendingIntent.intentSender)
        }.getOrElse { error ->
            DeviceDeleteResult.Failed(error.message ?: directDeleteError?.message)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun mediaDeleteConfirmationUri(file: DeviceFileInfo): Uri? {
        val volume = runCatching { MediaStore.getVolumeName(file.uri) }
            .getOrDefault(MediaStore.VOLUME_EXTERNAL)

        val collection = when (file.mediaStoreType) {
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE ->
                MediaStore.Images.Media.getContentUri(volume)
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO ->
                MediaStore.Video.Media.getContentUri(volume)
            MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO ->
                MediaStore.Audio.Media.getContentUri(volume)
            else -> return null
        }
        return ContentUris.withAppendedId(collection, file.id)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestDeleteAndroid10(file: DeviceFileInfo): DeviceDeleteResult =
        try {
            val deleted = resolver.delete(file.uri, null, null)
            if (deleted > 0) {
                DeviceDeleteResult.Deleted
            } else {
                DeviceDeleteResult.Failed("Android no eliminó el archivo.")
            }
        } catch (error: RecoverableSecurityException) {
            DeviceDeleteResult.NeedsUserConfirmation(
                error.userAction.actionIntent.intentSender,
            )
        } catch (error: SecurityException) {
            DeviceDeleteResult.Failed(error.message)
        } catch (error: Exception) {
            DeviceDeleteResult.Failed(error.message)
        }

    private fun deleteLegacy(file: DeviceFileInfo): DeviceDeleteResult =
        try {
            val deleted = resolver.delete(file.uri, null, null)
            if (deleted > 0) {
                DeviceDeleteResult.Deleted
            } else {
                DeviceDeleteResult.Failed("Android no eliminó el archivo.")
            }
        } catch (error: SecurityException) {
            DeviceDeleteResult.Failed(error.message)
        } catch (error: Exception) {
            DeviceDeleteResult.Failed(error.message)
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
