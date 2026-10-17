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
import io.github.hnalvaradohn.oclax.platform.DeviceContentRepository
import io.github.hnalvaradohn.oclax.platform.DeviceFileInfo
import io.github.hnalvaradohn.oclax.platform.InstalledAppDocumentInfo
import io.github.hnalvaradohn.oclax.platform.InstalledAppsRepository
import io.github.hnalvaradohn.oclax.platform.ThumbnailLoader
import java.io.File
import java.io.FileNotFoundException

class OclAxDocumentsProvider : DocumentsProvider() {
    companion object {
        private const val ROOT_ID = "oclax"
        private const val ROOT_DOCUMENT_ID = "root"
        private const val DEVICE_ROOT_DOCUMENT_ID = "device:root"
        private const val OCLAX_CATEGORY_PREFIX = "oclax-category:"
        private const val DEVICE_CATEGORY_PREFIX = "device-category:"
        private const val DEVICE_FILE_PREFIX = "device-file:"

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
        APPLICATIONS("applications", "Aplicaciones", prefersGrid = true),
        IMAGES("images", "Imágenes", prefersGrid = true),
        DOCUMENTS("documents", "Documentos"),
        PDF("pdf", "PDF"),
        APK("apk", "APK"),
        TEXT("text", "Texto/Código"),
        VIDEO("video", "Video", prefersGrid = true),
        AUDIO("audio", "Audio"),
        OTHER("other", "Otros"),
        ;

        fun matches(mimeType: String, pinned: Boolean = false): Boolean {
            if (this == PINNED) return pinned
            if (this == APPLICATIONS) return false
            return when (this) {
                IMAGES -> contentTypeFor(mimeType) == ContentType.IMAGE
                DOCUMENTS -> contentTypeFor(mimeType) == ContentType.DOCUMENT
                PDF -> contentTypeFor(mimeType) == ContentType.PDF
                APK -> contentTypeFor(mimeType) == ContentType.APP
                TEXT -> contentTypeFor(mimeType) == ContentType.TEXT
                VIDEO -> contentTypeFor(mimeType) == ContentType.VIDEO
                AUDIO -> contentTypeFor(mimeType) == ContentType.AUDIO
                OTHER -> contentTypeFor(mimeType) == ContentType.OTHER
                PINNED -> pinned
                APPLICATIONS -> false
            }
        }

        val oclaxDocumentId: String
            get() = OCLAX_CATEGORY_PREFIX + id

        val deviceDocumentId: String
            get() = DEVICE_CATEGORY_PREFIX + id

        companion object {
            fun fromOclAxDocumentId(documentId: String): Category? =
                entries.firstOrNull {
                    it != APPLICATIONS && it.oclaxDocumentId == documentId
                }

            fun fromDeviceDocumentId(documentId: String): Category? =
                entries.firstOrNull {
                    it != PINNED && it.deviceDocumentId == documentId
                }
        }
    }

    private lateinit var store: ItemStore
    private lateinit var deviceRepository: DeviceContentRepository
    private lateinit var installedAppsRepository: InstalledAppsRepository
    private lateinit var thumbnailLoader: ThumbnailLoader

    override fun onCreate(): Boolean {
        val providerContext = context ?: return false
        store = ItemStore(providerContext)
        deviceRepository = DeviceContentRepository(providerContext)
        installedAppsRepository = InstalledAppsRepository(providerContext)
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
            documentId == ROOT_DOCUMENT_ID ->
                addDirectory(cursor, columns, ROOT_DOCUMENT_ID, "OclAx", prefersGrid = true)

            documentId == DEVICE_ROOT_DOCUMENT_ID ->
                addDirectory(
                    cursor,
                    columns,
                    DEVICE_ROOT_DOCUMENT_ID,
                    "Mi dispositivo",
                    prefersGrid = true,
                )

            Category.fromOclAxDocumentId(documentId) != null -> {
                val category = requireNotNull(Category.fromOclAxDocumentId(documentId))
                addDirectory(cursor, columns, documentId, category.label, category.prefersGrid)
            }

            Category.fromDeviceDocumentId(documentId) != null -> {
                val category = requireNotNull(Category.fromDeviceDocumentId(documentId))
                addDirectory(cursor, columns, documentId, category.label, category.prefersGrid)
            }

            InstalledAppDocumentIds.parse(documentId) != null ->
                addInstalledAppDocument(
                    cursor,
                    columns,
                    requireNotNull(InstalledAppDocumentIds.parse(documentId)),
                )

            documentId.startsWith(DEVICE_FILE_PREFIX) ->
                addDeviceFile(
                    cursor,
                    columns,
                    resolveDeviceFile(documentId)
                        ?: throw FileNotFoundException("Archivo del dispositivo no encontrado."),
                )

            else ->
                addOclAxItem(
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

        when {
            parentDocumentId == ROOT_DOCUMENT_ID -> {
                addDirectory(
                    cursor,
                    columns,
                    DEVICE_ROOT_DOCUMENT_ID,
                    "Mi dispositivo",
                    prefersGrid = true,
                )
                Category.entries
                    .filterNot { it == Category.APPLICATIONS }
                    .forEach { category ->
                        addDirectory(
                            cursor,
                            columns,
                            category.oclaxDocumentId,
                            category.label,
                            category.prefersGrid,
                        )
                    }
                store.listItems().forEach { addOclAxItem(cursor, columns, it) }
                if (deviceRepository.hasBroadFileAccess()) {
                    deviceRepository.listFiles()
                        .take(100)
                        .forEach { addDeviceFile(cursor, columns, it) }
                }
            }

            parentDocumentId == DEVICE_ROOT_DOCUMENT_ID -> {
                addDirectory(
                    cursor,
                    columns,
                    Category.APPLICATIONS.deviceDocumentId,
                    Category.APPLICATIONS.label,
                    Category.APPLICATIONS.prefersGrid,
                )

                if (deviceRepository.hasBroadFileAccess()) {
                    Category.entries
                        .filterNot {
                            it == Category.PINNED || it == Category.APPLICATIONS
                        }
                        .forEach { category ->
                            addDirectory(
                                cursor,
                                columns,
                                category.deviceDocumentId,
                                category.label,
                                category.prefersGrid,
                            )
                        }
                    deviceRepository.listFiles().forEach { addDeviceFile(cursor, columns, it) }
                }
            }

            Category.fromOclAxDocumentId(parentDocumentId) != null -> {
                val category = requireNotNull(Category.fromOclAxDocumentId(parentDocumentId))
                store.listItems()
                    .filter { category.matches(it.mimeType, it.pinned) }
                    .forEach { addOclAxItem(cursor, columns, it) }
            }

            Category.fromDeviceDocumentId(parentDocumentId) == Category.APPLICATIONS -> {
                installedAppsRepository.listInstalledAppDocuments()
                    .forEach { app -> addInstalledAppEntry(cursor, columns, app) }
            }

            Category.fromDeviceDocumentId(parentDocumentId) != null -> {
                ensureDeviceAccess()
                val category = requireNotNull(Category.fromDeviceDocumentId(parentDocumentId))
                deviceRepository.listFiles()
                    .filter { category.matches(it.mimeType) }
                    .forEach { addDeviceFile(cursor, columns, it) }
            }

            InstalledAppDocumentIds.parse(parentDocumentId) is InstalledAppDocumentRef.Directory -> {
                val ref = requireNotNull(
                    InstalledAppDocumentIds.parse(parentDocumentId)
                        as? InstalledAppDocumentRef.Directory,
                )
                val app = resolveInstalledAppDocument(ref.packageName)
                    ?: throw FileNotFoundException("Aplicación instalada no encontrada.")
                installedAppSourceFiles(app)
                    .forEachIndexed { index, file ->
                        addInstalledAppPart(cursor, columns, app, file, index)
                    }
            }

            else -> throw FileNotFoundException("Carpeta no encontrada.")
        }

        return cursor
    }

    override fun queryRecentDocuments(rootId: String, projection: Array<out String>?): Cursor {
        if (rootId != ROOT_ID) throw FileNotFoundException("Raíz no encontrada.")
        val columns = projection ?: DOCUMENT_PROJECTION
        val cursor = MatrixCursor(columns)
        store.listItems().forEach { addOclAxItem(cursor, columns, it) }
        if (deviceRepository.hasBroadFileAccess()) {
            deviceRepository.listFiles().take(200).forEach { addDeviceFile(cursor, columns, it) }
        }
        return cursor
    }

    override fun querySearchDocuments(
        rootId: String,
        query: String,
        projection: Array<out String>?,
    ): Cursor {
        if (rootId != ROOT_ID) throw FileNotFoundException("Raíz no encontrada.")
        val columns = projection ?: DOCUMENT_PROJECTION
        val cursor = MatrixCursor(columns)
        val needle = query.trim().lowercase()

        store.search(query).forEach { addOclAxItem(cursor, columns, it) }

        installedAppsRepository.listInstalledAppDocuments()
            .asSequence()
            .filter { app ->
                needle.isEmpty() ||
                    app.label.lowercase().contains(needle) ||
                    app.packageName.lowercase().contains(needle)
            }
            .take(200)
            .forEach { addInstalledAppEntry(cursor, columns, it) }

        if (deviceRepository.hasBroadFileAccess()) {
            deviceRepository.listFiles()
                .asSequence()
                .filter { file ->
                    needle.isEmpty() ||
                        file.displayName.lowercase().contains(needle) ||
                        file.mimeType.lowercase().contains(needle) ||
                        file.relativePath.orEmpty().lowercase().contains(needle)
                }
                .take(500)
                .forEach { addDeviceFile(cursor, columns, it) }
        }
        return cursor
    }

    override fun openDocument(
        documentId: String,
        mode: String,
        signal: CancellationSignal?,
    ): ParcelFileDescriptor {
        if (mode != "r") throw FileNotFoundException("OclAx expone documentos en modo lectura.")
        if (isDirectoryDocument(documentId)) {
            throw FileNotFoundException("No se puede abrir una carpeta como archivo.")
        }

        InstalledAppDocumentIds.parse(documentId)?.let { ref ->
            val source = resolveInstalledAppSource(ref)
                ?: throw FileNotFoundException("APK instalado no encontrado.")
            signal?.throwIfCanceled()
            return ParcelFileDescriptor.open(source, ParcelFileDescriptor.MODE_READ_ONLY)
        }

        if (documentId.startsWith(DEVICE_FILE_PREFIX)) {
            val file = resolveDeviceFile(documentId)
                ?: throw FileNotFoundException("Archivo del dispositivo no encontrado.")
            return requireNotNull(context).contentResolver.openFileDescriptor(file.uri, "r", signal)
                ?: throw FileNotFoundException("Android no pudo abrir el archivo.")
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
        if (isDirectoryDocument(documentId)) {
            throw FileNotFoundException("Una carpeta no tiene miniatura.")
        }

        signal?.throwIfCanceled()
        val targetPx = maxOf(sizeHint.x, sizeHint.y).coerceIn(96, 1024)

        val appRef = InstalledAppDocumentIds.parse(documentId)
        if (appRef != null) {
            throw FileNotFoundException("Los APK instalados usan el icono genérico del sistema.")
        }

        val bitmap = when {
            documentId.startsWith(DEVICE_FILE_PREFIX) -> {
                val file = resolveDeviceFile(documentId)
                    ?: throw FileNotFoundException("Archivo del dispositivo no encontrado.")
                if (!supportsThumbnail(file.mimeType)) {
                    throw FileNotFoundException("Este tipo de archivo no ofrece miniatura.")
                }
                thumbnailLoader.loadDevice(file, targetPx)
            }

            else -> {
                val item = store.findItem(documentId)
                    ?: throw FileNotFoundException("Documento no encontrado.")
                if (!supportsThumbnail(item.mimeType)) {
                    throw FileNotFoundException("Este tipo de archivo no ofrece miniatura.")
                }
                val source = store.payloadFile(documentId)
                    ?: throw FileNotFoundException("Documento no encontrado.")
                thumbnailLoader.loadFile(source, contentTypeFor(item.mimeType), targetPx)
            }
        } ?: throw FileNotFoundException("No se pudo generar la miniatura.")

        signal?.throwIfCanceled()

        val thumbnailDir = File(
            requireNotNull(context).cacheDir,
            "oclax/provider_thumbnails",
        ).apply { mkdirs() }
        val safeId = documentId.replace(Regex("[^A-Za-z0-9._-]"), "_").take(120)
        val thumbnailFile = File(thumbnailDir, "$safeId-$targetPx.png")
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
            isDirectoryDocument(documentId) -> DocumentsContract.Document.MIME_TYPE_DIR

            InstalledAppDocumentIds.parse(documentId) != null ->
                INSTALLED_APK_MIME_TYPE

            documentId.startsWith(DEVICE_FILE_PREFIX) ->
                resolveDeviceFile(documentId)?.mimeType
                    ?: throw FileNotFoundException("Archivo del dispositivo no encontrado.")

            else ->
                store.findItem(documentId)?.mimeType
                    ?: throw FileNotFoundException("Documento no encontrado.")
        }

    override fun isChildDocument(parentDocumentId: String, documentId: String): Boolean {
        if (parentDocumentId == ROOT_DOCUMENT_ID) {
            return documentId == DEVICE_ROOT_DOCUMENT_ID ||
                Category.fromOclAxDocumentId(documentId) != null ||
                store.findItem(documentId) != null ||
                (documentId.startsWith(DEVICE_FILE_PREFIX) && resolveDeviceFile(documentId) != null)
        }

        if (parentDocumentId == DEVICE_ROOT_DOCUMENT_ID) {
            return Category.fromDeviceDocumentId(documentId) != null ||
                (documentId.startsWith(DEVICE_FILE_PREFIX) && resolveDeviceFile(documentId) != null)
        }

        Category.fromOclAxDocumentId(parentDocumentId)?.let { category ->
            val item = store.findItem(documentId) ?: return false
            return category.matches(item.mimeType, item.pinned)
        }

        Category.fromDeviceDocumentId(parentDocumentId)?.let { category ->
            if (category == Category.APPLICATIONS) {
                val ref = InstalledAppDocumentIds.parse(documentId) ?: return false
                if (ref is InstalledAppDocumentRef.Part) return false
                val app = resolveInstalledAppDocument(ref.packageName) ?: return false
                return installedAppEntryRef(app) == ref
            }

            val file = resolveDeviceFile(documentId) ?: return false
            return category.matches(file.mimeType)
        }

        val parentAppRef = InstalledAppDocumentIds.parse(parentDocumentId)
            as? InstalledAppDocumentRef.Directory
        if (parentAppRef != null) {
            val childRef = InstalledAppDocumentIds.parse(documentId)
                as? InstalledAppDocumentRef.Part
                ?: return false
            return childRef.packageName == parentAppRef.packageName &&
                resolveInstalledAppSource(childRef) != null
        }

        return false
    }

    private fun resolveDeviceFile(documentId: String): DeviceFileInfo? {
        if (!deviceRepository.hasBroadFileAccess()) return null
        val id = documentId.removePrefix(DEVICE_FILE_PREFIX).toLongOrNull() ?: return null
        return deviceRepository.listFiles().firstOrNull { it.id == id }
    }

    private fun deviceDocumentId(file: DeviceFileInfo): String = DEVICE_FILE_PREFIX + file.id

    private fun resolveInstalledAppDocument(packageName: String): InstalledAppDocumentInfo? =
        installedAppsRepository.findInstalledAppDocument(packageName)

    private fun installedAppSourceFiles(app: InstalledAppDocumentInfo): List<File> =
        app.sourceApkPaths
            .asSequence()
            .map(::File)
            .filter { it.isFile }
            .toList()

    private fun installedAppEntryRef(app: InstalledAppDocumentInfo): InstalledAppDocumentRef? {
        val parts = installedAppSourceFiles(app)
        return when (parts.size) {
            0 -> null
            1 -> InstalledAppDocumentRef.Single(app.packageName)
            else -> InstalledAppDocumentRef.Directory(app.packageName)
        }
    }

    private fun resolveInstalledAppSource(ref: InstalledAppDocumentRef): File? {
        val app = resolveInstalledAppDocument(ref.packageName) ?: return null
        val files = installedAppSourceFiles(app)
        return when (ref) {
            is InstalledAppDocumentRef.Single ->
                files.singleOrNull()

            is InstalledAppDocumentRef.Part ->
                files.getOrNull(ref.index)

            is InstalledAppDocumentRef.Directory ->
                null
        }
    }

    private fun ensureDeviceAccess() {
        if (!deviceRepository.hasBroadFileAccess()) {
            throw FileNotFoundException("OclAx no tiene acceso a los archivos del dispositivo.")
        }
    }

    private fun isDirectoryDocument(documentId: String): Boolean =
        documentId == ROOT_DOCUMENT_ID ||
            documentId == DEVICE_ROOT_DOCUMENT_ID ||
            Category.fromOclAxDocumentId(documentId) != null ||
            Category.fromDeviceDocumentId(documentId) != null ||
            InstalledAppDocumentIds.parse(documentId) is InstalledAppDocumentRef.Directory

    private fun supportsThumbnail(mimeType: String): Boolean =
        when (contentTypeFor(mimeType)) {
            ContentType.IMAGE, ContentType.VIDEO, ContentType.PDF -> true
            else -> false
        }

    private fun addInstalledAppDocument(
        cursor: MatrixCursor,
        columns: Array<out String>,
        ref: InstalledAppDocumentRef,
    ) {
        val app = resolveInstalledAppDocument(ref.packageName)
            ?: throw FileNotFoundException("Aplicación instalada no encontrada.")

        when (ref) {
            is InstalledAppDocumentRef.Single -> {
                val source = resolveInstalledAppSource(ref)
                    ?: throw FileNotFoundException("APK instalado no encontrado.")
                addInstalledAppFile(
                    cursor = cursor,
                    columns = columns,
                    documentId = InstalledAppDocumentIds.single(app.packageName),
                    displayName = installedAppDisplayName(app),
                    byteSize = source.length(),
                )
            }

            is InstalledAppDocumentRef.Directory -> {
                if (installedAppSourceFiles(app).size <= 1) {
                    throw FileNotFoundException("La aplicación ya no usa varios APK.")
                }
                addDirectory(
                    cursor,
                    columns,
                    InstalledAppDocumentIds.directory(app.packageName),
                    app.label,
                    prefersGrid = false,
                )
            }

            is InstalledAppDocumentRef.Part -> {
                val source = resolveInstalledAppSource(ref)
                    ?: throw FileNotFoundException("Parte APK no encontrada.")
                addInstalledAppFile(
                    cursor = cursor,
                    columns = columns,
                    documentId = InstalledAppDocumentIds.part(app.packageName, ref.index),
                    displayName = installedAppPartDisplayName(source, ref.index),
                    byteSize = source.length(),
                )
            }
        }
    }

    private fun addInstalledAppEntry(
        cursor: MatrixCursor,
        columns: Array<out String>,
        app: InstalledAppDocumentInfo,
    ) {
        when (val ref = installedAppEntryRef(app)) {
            is InstalledAppDocumentRef.Single -> {
                val source = resolveInstalledAppSource(ref) ?: return
                addInstalledAppFile(
                    cursor = cursor,
                    columns = columns,
                    documentId = InstalledAppDocumentIds.single(app.packageName),
                    displayName = installedAppDisplayName(app),
                    byteSize = source.length(),
                )
            }

            is InstalledAppDocumentRef.Directory -> {
                addDirectory(
                    cursor,
                    columns,
                    InstalledAppDocumentIds.directory(app.packageName),
                    app.label,
                    prefersGrid = false,
                )
            }

            else -> Unit
        }
    }

    private fun addInstalledAppPart(
        cursor: MatrixCursor,
        columns: Array<out String>,
        app: InstalledAppDocumentInfo,
        source: File,
        index: Int,
    ) {
        addInstalledAppFile(
            cursor = cursor,
            columns = columns,
            documentId = InstalledAppDocumentIds.part(app.packageName, index),
            displayName = installedAppPartDisplayName(source, index),
            byteSize = source.length(),
        )
    }

    private fun addInstalledAppFile(
        cursor: MatrixCursor,
        columns: Array<out String>,
        documentId: String,
        displayName: String,
        byteSize: Long,
    ) {
        val row = cursor.newRow()
        put(row, columns, DocumentsContract.Document.COLUMN_DOCUMENT_ID, documentId)
        put(row, columns, DocumentsContract.Document.COLUMN_DISPLAY_NAME, displayName)
        put(row, columns, DocumentsContract.Document.COLUMN_MIME_TYPE, INSTALLED_APK_MIME_TYPE)
        put(row, columns, DocumentsContract.Document.COLUMN_LAST_MODIFIED, 0L)
        put(row, columns, DocumentsContract.Document.COLUMN_FLAGS, 0)
        put(row, columns, DocumentsContract.Document.COLUMN_SIZE, byteSize)
    }

    private fun installedAppDisplayName(app: InstalledAppDocumentInfo): String {
        val clean = app.label.replace('/', '_').replace('\\', '_').trim()
        return (clean.ifBlank { app.packageName }) + ".apk"
    }

    private fun installedAppPartDisplayName(source: File, index: Int): String {
        val name = source.name
            .replace('/', '_')
            .replace('\\', '_')
            .takeIf { it.endsWith(".apk", ignoreCase = true) }
        return name ?: if (index == 0) "base.apk" else "split-$index.apk"
    }

    private fun addDirectory(
        cursor: MatrixCursor,
        columns: Array<out String>,
        documentId: String,
        displayName: String,
        prefersGrid: Boolean,
    ) {
        val row = cursor.newRow()
        put(row, columns, DocumentsContract.Document.COLUMN_DOCUMENT_ID, documentId)
        put(row, columns, DocumentsContract.Document.COLUMN_DISPLAY_NAME, displayName)
        put(
            row,
            columns,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.MIME_TYPE_DIR,
        )
        val flags = DocumentsContract.Document.FLAG_DIR_PREFERS_LAST_MODIFIED or
            if (prefersGrid) DocumentsContract.Document.FLAG_DIR_PREFERS_GRID else 0
        put(row, columns, DocumentsContract.Document.COLUMN_FLAGS, flags)
        put(row, columns, DocumentsContract.Document.COLUMN_LAST_MODIFIED, System.currentTimeMillis())
        put(row, columns, DocumentsContract.Document.COLUMN_SIZE, 0L)
    }

    private fun addOclAxItem(
        cursor: MatrixCursor,
        columns: Array<out String>,
        item: StoredItem,
    ) {
        val row = cursor.newRow()
        put(row, columns, DocumentsContract.Document.COLUMN_DOCUMENT_ID, item.id)
        put(row, columns, DocumentsContract.Document.COLUMN_DISPLAY_NAME, item.displayName)
        put(row, columns, DocumentsContract.Document.COLUMN_MIME_TYPE, item.mimeType)
        put(row, columns, DocumentsContract.Document.COLUMN_LAST_MODIFIED, item.createdAt)
        put(
            row,
            columns,
            DocumentsContract.Document.COLUMN_FLAGS,
            if (supportsThumbnail(item.mimeType)) {
                DocumentsContract.Document.FLAG_SUPPORTS_THUMBNAIL
            } else {
                0
            },
        )
        put(row, columns, DocumentsContract.Document.COLUMN_SIZE, item.byteSize)
    }

    private fun addDeviceFile(
        cursor: MatrixCursor,
        columns: Array<out String>,
        file: DeviceFileInfo,
    ) {
        val row = cursor.newRow()
        put(row, columns, DocumentsContract.Document.COLUMN_DOCUMENT_ID, deviceDocumentId(file))
        put(row, columns, DocumentsContract.Document.COLUMN_DISPLAY_NAME, file.displayName)
        put(row, columns, DocumentsContract.Document.COLUMN_MIME_TYPE, file.mimeType)
        put(row, columns, DocumentsContract.Document.COLUMN_LAST_MODIFIED, file.modifiedAt)
        put(
            row,
            columns,
            DocumentsContract.Document.COLUMN_FLAGS,
            if (supportsThumbnail(file.mimeType)) {
                DocumentsContract.Document.FLAG_SUPPORTS_THUMBNAIL
            } else {
                0
            },
        )
        put(row, columns, DocumentsContract.Document.COLUMN_SIZE, file.byteSize)
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
