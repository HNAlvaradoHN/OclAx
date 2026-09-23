package io.github.hnalvaradohn.oclax.provider

import android.database.Cursor
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.os.StatFs
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import io.github.hnalvaradohn.oclax.data.ItemStore
import io.github.hnalvaradohn.oclax.model.StoredItem
import java.io.FileNotFoundException

class OclAxDocumentsProvider : DocumentsProvider() {
    companion object {
        private const val ROOT_ID = "oclax"
        private const val ROOT_DOCUMENT_ID = "root"

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

    private lateinit var store: ItemStore

    override fun onCreate(): Boolean {
        val providerContext = context ?: return false
        store = ItemStore(providerContext)
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
        if (documentId == ROOT_DOCUMENT_ID) {
            addRootDocument(cursor, columns)
        } else {
            addItem(
                cursor,
                columns,
                store.findItem(documentId) ?: throw FileNotFoundException("Documento no encontrado."),
            )
        }
        return cursor
    }

    override fun queryChildDocuments(
        parentDocumentId: String,
        projection: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        if (parentDocumentId != ROOT_DOCUMENT_ID) throw FileNotFoundException("Carpeta no encontrada.")
        return itemCursor(projection, store.listItems())
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
        val file = store.payloadFile(documentId) ?: throw FileNotFoundException("Documento no encontrado.")
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun getDocumentType(documentId: String): String =
        if (documentId == ROOT_DOCUMENT_ID) {
            DocumentsContract.Document.MIME_TYPE_DIR
        } else {
            store.findItem(documentId)?.mimeType ?: throw FileNotFoundException("Documento no encontrado.")
        }

    override fun isChildDocument(parentDocumentId: String, documentId: String): Boolean =
        parentDocumentId == ROOT_DOCUMENT_ID && store.findItem(documentId) != null

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
        put(row, columns, DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.MIME_TYPE_DIR)
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
        put(row, columns, DocumentsContract.Document.COLUMN_FLAGS, 0)
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
