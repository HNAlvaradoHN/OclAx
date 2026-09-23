package io.github.hnalvaradohn.oclax.provider

import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Bitmap
import android.graphics.Point
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.os.StatFs
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import io.github.hnalvaradohn.oclax.data.ItemStore
import io.github.hnalvaradohn.oclax.model.ContentType
import io.github.hnalvaradohn.oclax.model.StoredItem
import io.github.hnalvaradohn.oclax.model.contentTypeFor
import io.github.hnalvaradohn.oclax.platform.ThumbnailLoader
import java.io.File
import java.io.FileNotFoundException

class OclAxDocumentsProvider : DocumentsProvider() {
    companion object {
        private const val ROOT_ID = "oclax"
        private const val ROOT_DOCUMENT_ID = "root"
        private const val CATEGORY_PREFIX = "category:"

        private val ROOT_PROJECTION = arrayOf(
            DocumentsContract.Root.COLUMN_ROOT_ID,
            DocumentsContract.Root.COLUMN_FLAGS,
            DocumentsContract.Root.COLUMN_TITLE,
            DocumentsContract.Root.COLUMN_DOCUMENT_ID,
            DocumentsContract.Root.COLUMN_MIME_TYPES,
            DocumentsContract.Root.COLUMN_AVAILABLE_BYTES,
        )

        private val DOCUMENT_PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_FLAGS,
            DocumentsContract.Document.COLUMN_SIZE,
        )
    }

    private enum class Category(
        val id: String,
        val label: String,
        val prefersGrid: Boolean = false,
    ) {
        PINNED("pinned", "Fijados"),
        IMAGES("images", "Imágenes", prefersGrid = true),
        DOCUMENTS("documents", "Documentos"),
        PDF("pdf", "PDF"),
        APK("apk", "APK"),
        TEXT("text", "Texto/Código"),
        VIDEO("video", "Video", prefersGrid = true),
        AUDIO("audio", "Audio"),
        OTHER("other", "Otros"),
        ;

        val documentId: String
            get() = CATEGORY_PREFIX + id

        fun matches(item: StoredItem): Boolean {
            if (this == PINNED) return item.pinned

            return when (this) {
                IMAGES -> contentTypeFor(item.mimeType) == ContentType.IMAGE
                DOCUMENTS -> contentTypeFor(item.mimeType) == ContentType.DOCUMENT
                PDF -> contentTypeFor(item.mimeType) == ContentType.PDF
                APK -> contentTypeFor(item.mimeType) == ContentType.APP
                TEXT -> contentTypeFor(item.mimeType) == ContentType.TEXT
                VIDEO -> contentTypeFor(item.mimeType) == ContentType.VIDEO
                AUDIO -> contentTypeFor(item.mimeType) == ContentType.AUDIO
                OTHER -> contentTypeFor(item.mimeType) == ContentType.OTHER
                PINNED -> item.pinned
            }
        }

        companion object {
            fun fromDocumentId(documentId: String): Category? =
                entries.firstOrNull { it.documentId == documentId }
        }
    }

    private lateinit var store: ItemStore
    private lateinit var thumbnailLoader: ThumbnailLoader

    override fun onCreate(): Boolean {
        val providerContext = context ?: return false
        store = ItemStore(providerContext)
        thumbnailLoader = ThumbnailLoader(providerContext)
        return true
    }

    override fun queryRoots(projection: Array<out String>?): Cursor {
        val columns = projection ?: ROOT_PROJECTION
        val cursor = MatrixCursor(columns)
        val row = cursor.newRow()
        put(row, columns, DocumentsContract.Root.COLUMN_ROOT_ID, ROOT_ID)
        put(
            row,
            columns,
            DocumentsContract.Root.COLUMN_FLAGS,
            DocumentsContract.Root.FLAG_LOCAL_ONLY or
                DocumentsContract.Root.FLAG_SUPPORTS_RECENTS or
                DocumentsContract.Root.FLAG_SUPPORTS_SEARCH,
        )
        put(row, columns, DocumentsContract.Root.COLUMN_TITLE, "OclAx")
        put(row, columns, DocumentsContract.Root.COLUMN_DOCUMENT_ID, ROOT_DOCUMENT_ID)
        put(row, columns, DocumentsContract.Root.COLUMN_MIME_TYPES, "*/*")
        put(
            row,
            columns,
            DocumentsContract.Root.COLUMN_AVAILABLE_BYTES,
            StatFs(requireNotNull(context).filesDir.absolutePath).availableBytes,
        )
        return cursor
    }

    override fun queryDocument(documentId: String, projection: Array<out String>?): Cursor {
        val columns = projection ?: DOCUMENT_PROJECTION
        val cursor = MatrixCursor(columns)

        when {
            documentId == ROOT_DOCUMENT_ID -> addRootDocument(cursor, columns)
            Category.fromDocumentId(documentId) != null ->
                addCategory(cursor, columns, requireNotNull(Category.fromDocumentId(documentId)))
            else ->
                addItem(
                    cursor,
                    columns,
                    store.findItem(documentId)
                        ?: throw FileNotFoundException("Documento no encontrado."),
                )
        }

        return cursor
    }

    override fun queryChildDocuments(
        parentDocumentId: String,
        projection: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        val columns = projection ?: DOCUMENT_PROJECTION
        val cursor = MatrixCursor(columns)

        if (parentDocumentId == ROOT_DOCUMENT_ID) {
            Category.entries.forEach { addCategory(cursor, columns, it) }
            store.listItems().forEach { addItem(cursor, columns, it) }
            return cursor
        }

        val category = Category.fromDocumentId(parentDocumentId)
            ?: throw FileNotFoundException("Carpeta no encontrada.")

        store.listItems()
            .filter(category::matches)
            .forEach { addItem(cursor, columns, it) }

        return cursor
    }

    override fun queryRecentDocuments(rootId: String, projection: Array<out String>?): Cursor {
        if (rootId != ROOT_ID) throw FileNotFoundException("Raíz no encontrada.")
        return itemCursor(projection, store.listItems())
    }

    override fun querySearchDocuments(
        rootId: String,
        query: String,
        projection: Array<out String>?,
    ): Cursor {
        if (rootId != ROOT_ID) throw FileNotFoundException("Raíz no encontrada.")
        return itemCursor(projection, store.search(query))
    }

    override fun openDocument(
        documentId: String,
        mode: String,
        signal: CancellationSignal?,
    ): ParcelFileDescriptor {
        if (mode != "r") throw FileNotFoundException("OclAx expone documentos en modo lectura.")
        if (documentId == ROOT_DOCUMENT_ID || Category.fromDocumentId(documentId) != null) {
            throw FileNotFoundException("No se puede abrir una carpeta como archivo.")
        }

        val file = store.payloadFile(documentId)
            ?: throw FileNotFoundException("Documento no encontrado.")
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun openDocumentThumbnail(
        documentId: String,
        sizeHint: Point,
        signal: CancellationSignal?,
    ): AssetFileDescriptor {
        if (documentId == ROOT_DOCUMENT_ID || Category.fromDocumentId(documentId) != null) {
            throw FileNotFoundException("Una carpeta no tiene miniatura.")
        }

        signal?.throwIfCanceled()

        val item = store.findItem(documentId)
            ?: throw FileNotFoundException("Documento no encontrado.")
        val type = contentTypeFor(item.mimeType)
        if (type != ContentType.IMAGE && type != ContentType.VIDEO) {
            throw FileNotFoundException("Este tipo de archivo no ofrece miniatura.")
        }

        val source = store.payloadFile(documentId)
            ?: throw FileNotFoundException("Documento no encontrado.")
        val targetPx = maxOf(sizeHint.x, sizeHint.y).coerceIn(96, 1024)
        val bitmap = thumbnailLoader.loadFile(source, type, targetPx)
            ?: throw FileNotFoundException("No se pudo generar la miniatura.")

        signal?.throwIfCanceled()

        val thumbnailDir = File(
            requireNotNull(context).cacheDir,
            "oclax/provider_thumbnails",
        ).apply { mkdirs() }
        val thumbnailFile = File(
            thumbnailDir,
            "${item.id}-${item.createdAt}-$targetPx.png",
        )
        if (!thumbnailFile.exists()) {
            thumbnailFile.outputStream().buffered().use { output ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 90, output)) {
                    throw FileNotFoundException("No se pudo guardar la miniatura.")
                }
            }
        }

        val descriptor = ParcelFileDescriptor.open(
            thumbnailFile,
            ParcelFileDescriptor.MODE_READ_ONLY,
        )
        return AssetFileDescriptor(descriptor, 0, thumbnailFile.length())
    }

    override fun getDocumentType(documentId: String): String =
        when {
            documentId == ROOT_DOCUMENT_ID -> DocumentsContract.Document.MIME_TYPE_DIR
            Category.fromDocumentId(documentId) != null -> DocumentsContract.Document.MIME_TYPE_DIR
            else ->
                store.findItem(documentId)?.mimeType
                    ?: throw FileNotFoundException("Documento no encontrado.")
        }

    override fun isChildDocument(parentDocumentId: String, documentId: String): Boolean {
        if (parentDocumentId == ROOT_DOCUMENT_ID) {
            return Category.fromDocumentId(documentId) != null || store.findItem(documentId) != null
        }

        val category = Category.fromDocumentId(parentDocumentId) ?: return false
        val item = store.findItem(documentId) ?: return false
        return category.matches(item)
    }

    private fun itemCursor(projection: Array<out String>?, items: List<StoredItem>): Cursor {
        val columns = projection ?: DOCUMENT_PROJECTION
        val cursor = MatrixCursor(columns)
        items.forEach { addItem(cursor, columns, it) }
        return cursor
    }

    private fun addRootDocument(cursor: MatrixCursor, columns: Array<out String>) {
        val row = cursor.newRow()
        put(row, columns, DocumentsContract.Document.COLUMN_DOCUMENT_ID, ROOT_DOCUMENT_ID)
        put(row, columns, DocumentsContract.Document.COLUMN_DISPLAY_NAME, "Recientes")
        put(
            row,
            columns,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.MIME_TYPE_DIR,
        )
        val directoryFlags = DocumentsContract.Document.FLAG_DIR_PREFERS_LAST_MODIFIED or
            if (category.prefersGrid) {
                DocumentsContract.Document.FLAG_DIR_PREFERS_GRID
            } else {
                0
            }
        put(
            row,
            columns,
            DocumentsContract.Document.COLUMN_FLAGS,
            directoryFlags,
        )
        put(row, columns, DocumentsContract.Document.COLUMN_LAST_MODIFIED, System.currentTimeMillis())
        put(row, columns, DocumentsContract.Document.COLUMN_SIZE, 0L)
    }

    private fun addCategory(
        cursor: MatrixCursor,
        columns: Array<out String>,
        category: Category,
    ) {
        val row = cursor.newRow()
        put(row, columns, DocumentsContract.Document.COLUMN_DOCUMENT_ID, category.documentId)
        put(row, columns, DocumentsContract.Document.COLUMN_DISPLAY_NAME, category.label)
        put(
            row,
            columns,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.MIME_TYPE_DIR,
        )
        put(
            row,
            columns,
            DocumentsContract.Document.COLUMN_FLAGS,
            DocumentsContract.Document.FLAG_DIR_PREFERS_GRID or
                DocumentsContract.Document.FLAG_DIR_PREFERS_LAST_MODIFIED,
        )
        put(row, columns, DocumentsContract.Document.COLUMN_LAST_MODIFIED, System.currentTimeMillis())
        put(row, columns, DocumentsContract.Document.COLUMN_SIZE, 0L)
    }

    private fun addItem(cursor: MatrixCursor, columns: Array<out String>, item: StoredItem) {
        val row = cursor.newRow()
        put(row, columns, DocumentsContract.Document.COLUMN_DOCUMENT_ID, item.id)
        put(row, columns, DocumentsContract.Document.COLUMN_DISPLAY_NAME, item.displayName)
        put(row, columns, DocumentsContract.Document.COLUMN_MIME_TYPE, item.mimeType)
        put(row, columns, DocumentsContract.Document.COLUMN_LAST_MODIFIED, item.createdAt)
        val type = contentTypeFor(item.mimeType)
        val itemFlags = if (type == ContentType.IMAGE || type == ContentType.VIDEO) {
            DocumentsContract.Document.FLAG_SUPPORTS_THUMBNAIL
        } else {
            0
        }
        put(row, columns, DocumentsContract.Document.COLUMN_FLAGS, itemFlags)
        put(row, columns, DocumentsContract.Document.COLUMN_SIZE, item.byteSize)
    }

    private fun put(
        row: MatrixCursor.RowBuilder,
        columns: Array<out String>,
        column: String,
        value: Any?,
    ) {
        if (columns.contains(column)) row.add(column, value)
    }
}
