package io.github.hnalvaradohn.oclax.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import io.github.hnalvaradohn.oclax.model.StoredItem
import java.io.File

class ContentOpener(private val context: Context) {
    fun open(item: StoredItem, file: File): Boolean {
        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file,
        )
        return open(uri, item.mimeType)
    }

    fun open(uri: Uri, mimeType: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }
}
