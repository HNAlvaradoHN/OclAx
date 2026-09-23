package io.github.hnalvaradohn.oclax.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.LruCache
import android.util.Size
import io.github.hnalvaradohn.oclax.model.ContentType
import java.io.File
import java.io.InputStream
import kotlin.math.max

class ThumbnailLoader(context: Context) {
    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver

    private val cache = object : LruCache<String, Bitmap>(12 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int =
            value.byteCount / 1024
    }

    fun loadDevice(file: DeviceFileInfo, targetPx: Int = 320): Bitmap? {
        if (
            file.type != ContentType.IMAGE &&
            file.type != ContentType.VIDEO &&
            file.type != ContentType.PDF
        ) {
            return null
        }

        val safeTarget = targetPx.coerceIn(96, 1024)
        val key = "uri:${file.uri}:${file.modifiedAt}:$safeTarget"
        cache.get(key)?.let { return it }

        val bitmap = runCatching {
            when (file.type) {
                ContentType.PDF -> pdfFirstPage(safeTarget) {
                    resolver.openFileDescriptor(file.uri, "r")
                }
                ContentType.IMAGE,
                ContentType.VIDEO,
                -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    resolver.loadThumbnail(file.uri, Size(safeTarget, safeTarget), null)
                } else {
                    when (file.type) {
                        ContentType.IMAGE -> decodeSampledImage(safeTarget) {
                            resolver.openInputStream(file.uri)
                        }
                        ContentType.VIDEO -> videoFrame(safeTarget) {
                            setDataSource(appContext, file.uri)
                        }
                        else -> null
                    }
                }
                else -> null
            }
        }.getOrNull()

        bitmap?.let { cache.put(key, it) }
        return bitmap
    }

    fun loadFile(
        file: File,
        type: ContentType,
        targetPx: Int = 320,
    ): Bitmap? {
        if (
            type != ContentType.IMAGE &&
            type != ContentType.VIDEO &&
            type != ContentType.PDF
        ) {
            return null
        }

        val safeTarget = targetPx.coerceIn(96, 1024)
        val key = "file:${file.absolutePath}:${file.lastModified()}:$safeTarget"
        cache.get(key)?.let { return it }

        val bitmap = runCatching {
            when (type) {
                ContentType.IMAGE -> decodeSampledImage(safeTarget) {
                    file.inputStream()
                }
                ContentType.VIDEO -> videoFrame(safeTarget) {
                    setDataSource(file.absolutePath)
                }
                ContentType.PDF -> pdfFirstPage(safeTarget) {
                    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                }
                else -> null
            }
        }.getOrNull()

        bitmap?.let { cache.put(key, it) }
        return bitmap
    }

    private fun decodeSampledImage(
        targetPx: Int,
        streamFactory: () -> InputStream?,
    ): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        streamFactory()?.use { BitmapFactory.decodeStream(it, null, bounds) }

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (
            max(bounds.outWidth / sample, bounds.outHeight / sample) > targetPx * 2 &&
            sample < 128
        ) {
            sample *= 2
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return streamFactory()?.use { BitmapFactory.decodeStream(it, null, options) }
    }

    private fun pdfFirstPage(
        targetPx: Int,
        descriptorFactory: () -> ParcelFileDescriptor?,
    ): Bitmap? {
        val descriptor = descriptorFactory() ?: return null
        return descriptor.use { parcelFileDescriptor ->
            PdfRenderer(parcelFileDescriptor).use { renderer ->
                if (renderer.pageCount <= 0) return@use null

                renderer.openPage(0).use { page ->
                    val largest = max(page.width, page.height).coerceAtLeast(1)
                    val scale = targetPx.toFloat() / largest.toFloat()
                    val width = (page.width * scale).toInt().coerceAtLeast(1)
                    val height = (page.height * scale).toInt().coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    page.render(
                        bitmap,
                        null,
                        null,
                        PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY,
                    )
                    bitmap
                }
            }
        }
    }

    private fun videoFrame(
        targetPx: Int,
        configure: MediaMetadataRetriever.() -> Unit,
    ): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.configure()
            val frame = retriever.getFrameAtTime(
                0L,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
            ) ?: return null

            val largest = max(frame.width, frame.height)
            if (largest <= targetPx) {
                frame
            } else {
                val scale = targetPx.toFloat() / largest.toFloat()
                val scaled = Bitmap.createScaledBitmap(
                    frame,
                    (frame.width * scale).toInt().coerceAtLeast(1),
                    (frame.height * scale).toInt().coerceAtLeast(1),
                    true,
                )
                if (scaled !== frame) frame.recycle()
                scaled
            }
        } finally {
            retriever.release()
        }
    }
}
