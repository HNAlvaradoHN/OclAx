package io.github.hnalvaradohn.oclax.share

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import io.github.hnalvaradohn.oclax.data.ItemStore
import io.github.hnalvaradohn.oclax.model.StoredItem
import java.util.LinkedHashSet

class ShareIngestor(private val context: Context) {
    data class Result(val storedCount: Int, val clipboardKind: ClipboardKind)

    enum class ClipboardKind { NONE, TEXT, IMAGE }

    private val store = ItemStore(context)

    fun ingest(intent: Intent): Result {
        if (intent.action != Intent.ACTION_SEND && intent.action != Intent.ACTION_SEND_MULTIPLE) {
            return Result(0, ClipboardKind.NONE)
        }

        val importedFiles = collectUris(intent).map { store.importUri(it, intent.type) }
        val sharedText = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        val textItem = sharedText.takeIf { it.isNotEmpty() }?.let {
            store.importText(it, intent.getStringExtra(Intent.EXTRA_SUBJECT))
        }

        val imageItems = importedFiles.filter { it.mimeType.startsWith("image/") }
        val clipboardKind = when {
            imageItems.isNotEmpty() -> {
                publishImages(imageItems)
                ClipboardKind.IMAGE
            }
            sharedText.isNotEmpty() -> {
                publishText(sharedText)
                ClipboardKind.TEXT
            }
            else -> ClipboardKind.NONE
        }

        return Result(importedFiles.size + if (textItem != null) 1 else 0, clipboardKind)
    }

    private fun publishText(text: String) {
        context.getSystemService(ClipboardManager::class.java)
            .setPrimaryClip(ClipData.newPlainText("OclAx", text))
    }

    private fun publishImages(items: List<StoredItem>) {
        val uris = items.map { item ->
            FileProvider.getUriForFile(
                context,
                context.packageName + ".fileprovider",
                store.payloadFile(item),
            ) to item.mimeType
        }

        val description = ClipDescription("OclAx", uris.map { it.second }.distinct().toTypedArray())
        val clipData = ClipData(description, ClipData.Item(uris.first().first))
        uris.drop(1).forEach { (uri, _) -> clipData.addItem(ClipData.Item(uri)) }

        context.getSystemService(ClipboardManager::class.java).setPrimaryClip(clipData)
    }

    private fun collectUris(intent: Intent): List<Uri> {
        val result = LinkedHashSet<Uri>()
        if (intent.action == Intent.ACTION_SEND) singleStream(intent)?.let(result::add)
        if (intent.action == Intent.ACTION_SEND_MULTIPLE) multipleStreams(intent).forEach(result::add)

        intent.clipData?.let { clip ->
            for (index in 0 until clip.itemCount) {
                clip.getItemAt(index).uri?.let(result::add)
            }
        }
        return result.toList()
    }

    @Suppress("DEPRECATION")
    private fun singleStream(intent: Intent): Uri? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }

    @Suppress("DEPRECATION")
    private fun multipleStreams(intent: Intent): List<Uri> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
        } else {
            intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).orEmpty()
        }
}
