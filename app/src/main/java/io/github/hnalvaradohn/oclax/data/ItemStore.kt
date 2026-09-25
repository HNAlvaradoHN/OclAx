package io.github.hnalvaradohn.oclax.data

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.StatFs
import android.provider.OpenableColumns
import io.github.hnalvaradohn.oclax.model.StoredItem
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.FileInputStream
import java.io.IOException
import java.util.UUID

class ItemStore(private val context: Context) {
    companion object {
        private const val META_FILE = "meta.json"
        private const val PREFS_FILE = "oclax_retention"
        private const val PREF_RETENTION_HOURS = "retention_hours"
        private const val MAX_TEXT_CHARS = 2_000_000
        const val MAX_ITEM_BYTES: Long = 4L * 1024L * 1024L * 1024L
        private const val RESERVED_FREE_BYTES: Long = 64L * 1024L * 1024L
        private const val STORAGE_RECHECK_BYTES: Long = 16L * 1024L * 1024L
    }

    private val itemsDir = File(context.filesDir, "oclax/items").apply { mkdirs() }
    private val preferences = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    @Synchronized
    fun listItems(): List<StoredItem> {
        cleanupExpired()
        return rawItems()
            .sortedWith(compareByDescending<StoredItem> { it.pinned }.thenByDescending { it.createdAt })
    }

    @Synchronized
    fun findItem(id: String): StoredItem? {
        if (!isSafeId(id)) return null
        return readItem(File(itemsDir, id))
    }

    fun search(query: String): List<StoredItem> {
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) return listItems()
        return listItems().filter {
            it.displayName.lowercase().contains(needle) ||
                it.mimeType.lowercase().contains(needle)
        }
    }

    fun retentionHours(): Int = RetentionPolicy.normalizeHours(
        preferences.getInt(PREF_RETENTION_HOURS, RetentionPolicy.DEFAULT_HOURS),
    )

    @Synchronized
    fun setRetentionHours(hours: Int) {
        val normalized = RetentionPolicy.normalizeHours(hours)
        preferences.edit().putInt(PREF_RETENTION_HOURS, normalized).apply()
        cleanupExpired()
    }

    @Synchronized
    fun setPinned(id: String, pinned: Boolean): StoredItem? {
        if (!isSafeId(id)) return null
        val directory = File(itemsDir, id)
        val current = readItem(directory) ?: return null
        val updated = current.copy(pinned = pinned)
        writeMeta(directory, updated)
        return updated
    }

    @Synchronized
    fun deleteItem(id: String): Boolean {
        if (!isSafeId(id)) return false
        val directory = File(itemsDir, id)
        val item = readItem(directory) ?: return false
        if (item.id != id) return false

        val canonicalRoot = itemsDir.canonicalFile
        val canonicalDirectory = directory.canonicalFile
        if (canonicalDirectory.parentFile != canonicalRoot) return false

        return canonicalDirectory.deleteRecursively()
    }

    @Synchronized
    fun cleanupExpired(nowMillis: Long = System.currentTimeMillis()): Int {
        val retention = retentionHours()
        var deleted = 0
        rawItems().forEach { item ->
            if (RetentionPolicy.shouldExpire(item, nowMillis, retention)) {
                val directory = File(itemsDir, item.id)
                if (directory.parentFile == itemsDir && directory.deleteRecursively()) deleted++
            }
        }
        return deleted
    }

    @Synchronized
    fun importText(text: String, suggestedName: String? = null): StoredItem {
        if (text.isBlank()) throw IOException("El texto está vacío.")
        if (text.length > MAX_TEXT_CHARS) throw IOException("El texto supera el límite seguro de OclAx.")

        val displayName = SafeNames.sanitize(
            suggestedName?.takeIf { it.isNotBlank() }?.let {
                if (it.lowercase().endsWith(".txt")) it else "$it.txt"
            },
            fallback = "texto-${System.currentTimeMillis()}.txt",
        )
        val bytes = text.toByteArray(Charsets.UTF_8)
        ensureFreeSpace(bytes.size.toLong())

        return writeNewItem(displayName, "text/plain") { destination ->
            destination.outputStream().use { it.write(bytes) }
            bytes.size.toLong()
        }
    }

    @Synchronized
    fun importUri(uri: Uri, fallbackMimeType: String?): StoredItem {
        if (uri.scheme != ContentResolver.SCHEME_CONTENT) {
            throw IOException("OclAx solo acepta ubicaciones de contenido seguras.")
        }

        val resolver = context.contentResolver
        val metadata = readSourceMetadata(resolver, uri)
        val mimeType = normalizeMimeType(resolver.getType(uri) ?: fallbackMimeType)
        val displayName = SafeNames.sanitize(metadata.displayName, fallbackNameFor(mimeType))

        metadata.byteSize?.let {
            if (it < 0L || it > MAX_ITEM_BYTES) throw IOException("El archivo supera el límite seguro de OclAx.")
            ensureFreeSpace(it)
        }

        return writeNewItem(displayName, mimeType) { destination ->
            resolver.openInputStream(uri).use { input ->
                if (input == null) throw IOException("No se pudo abrir el archivo compartido.")
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var total = 0L
                    var sinceSpaceCheck = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        sinceSpaceCheck += read
                        if (total > MAX_ITEM_BYTES) throw IOException("El archivo supera el límite seguro de OclAx.")
                        if (sinceSpaceCheck >= STORAGE_RECHECK_BYTES) {
                            ensureFreeSpace(1L)
                            sinceSpaceCheck = 0L
                        }
                        output.write(buffer, 0, read)
                    }
                    output.flush()
                    total
                }
            }
        }
    }

    @Synchronized
    fun importTransferFile(
        source: File,
        displayName: String,
        mimeType: String,
        expectedBytes: Long,
    ): StoredItem {
        if (expectedBytes !in 1..MAX_ITEM_BYTES) {
            throw IOException("El archivo recibido supera el límite seguro de OclAx.")
        }

        val incomingRoot = File(context.filesDir, "oclax/transfers/incoming").canonicalFile
        val canonicalSource = source.canonicalFile
        if (!canonicalSource.path.startsWith(incomingRoot.path + File.separator)) {
            throw IOException("La fuente recibida no pertenece al almacenamiento temporal de OclAx.")
        }
        if (!canonicalSource.isFile || canonicalSource.length() != expectedBytes) {
            throw IOException("El archivo recibido está incompleto.")
        }

        val safeMimeType = normalizeMimeType(mimeType)
        val safeDisplayName = SafeNames.sanitize(
            displayName,
            fallbackNameFor(safeMimeType),
        )
        ensureFreeSpace(expectedBytes)

        return writeNewItem(safeDisplayName, safeMimeType) { destination ->
            FileInputStream(canonicalSource).use { input ->
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var total = 0L
                    var sinceSpaceCheck = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        sinceSpaceCheck += read
                        if (total > MAX_ITEM_BYTES || total > expectedBytes) {
                            throw IOException("El archivo recibido cambió durante la importación.")
                        }
                        if (sinceSpaceCheck >= STORAGE_RECHECK_BYTES) {
                            ensureFreeSpace(1L)
                            sinceSpaceCheck = 0L
                        }
                        output.write(buffer, 0, read)
                    }
                    output.flush()
                    if (total != expectedBytes) {
                        throw IOException("El archivo recibido cambió durante la importación.")
                    }
                    total
                }
            }
        }
    }

    fun payloadFile(item: StoredItem): File {
        if (!isSafeId(item.id)) throw IOException("Identificador inválido.")
        val directory = File(itemsDir, item.id).canonicalFile
        val payload = File(directory, item.payloadName).canonicalFile
        if (payload.parentFile != directory) throw IOException("Ruta de archivo inválida.")
        return payload
    }

    fun payloadFile(id: String): File? {
        val item = findItem(id) ?: return null
        return payloadFile(item).takeIf { it.isFile }
    }

    private fun rawItems(): List<StoredItem> =
        itemsDir.listFiles()
            .orEmpty()
            .asSequence()
            .filter { it.isDirectory && !it.name.startsWith(".tmp-") }
            .mapNotNull(::readItem)
            .toList()

    private fun writeNewItem(
        displayName: String,
        mimeType: String,
        writer: (File) -> Long,
    ): StoredItem {
        val id = UUID.randomUUID().toString()
        val tempDir = File(itemsDir, ".tmp-$id")
        val finalDir = File(itemsDir, id)
        if (!tempDir.mkdirs()) throw IOException("No se pudo preparar el almacenamiento local.")

        try {
            val payloadName = SafeNames.storageFileName(displayName, mimeType)
            val payloadFile = File(tempDir, payloadName)
            val actualBytes = writer(payloadFile)
            if (actualBytes <= 0L || !payloadFile.isFile) throw IOException("El archivo compartido está vacío.")

            val item = StoredItem(
                id = id,
                displayName = displayName,
                mimeType = mimeType,
                payloadName = payloadName,
                byteSize = actualBytes,
                createdAt = System.currentTimeMillis(),
                pinned = false,
            )
            writeMeta(tempDir, item)
            if (!tempDir.renameTo(finalDir)) throw IOException("No se pudo confirmar el archivo en OclAx.")
            return item
        } catch (error: Exception) {
            tempDir.deleteRecursively()
            throw error
        }
    }

    private fun writeMeta(directory: File, item: StoredItem) {
        val json = JSONObject()
            .put("id", item.id)
            .put("displayName", item.displayName)
            .put("mimeType", item.mimeType)
            .put("payloadName", item.payloadName)
            .put("byteSize", item.byteSize)
            .put("createdAt", item.createdAt)
            .put("pinned", item.pinned)

        val temp = File(directory, "$META_FILE.tmp")
        temp.writeText(json.toString(), Charsets.UTF_8)
        val target = File(directory, META_FILE)
        if (!temp.renameTo(target)) throw IOException("No se pudo guardar la metadata del archivo.")
    }

    private fun readItem(directory: File): StoredItem? = try {
        if (!directory.isDirectory) return null
        val json = JSONObject(File(directory, META_FILE).readText(Charsets.UTF_8))
        val item = StoredItem(
            id = json.getString("id"),
            displayName = json.getString("displayName"),
            mimeType = json.getString("mimeType"),
            payloadName = json.getString("payloadName"),
            byteSize = json.getLong("byteSize"),
            createdAt = json.getLong("createdAt"),
            pinned = json.optBoolean("pinned", false),
        )
        if (item.id != directory.name || !isSafeId(item.id)) return null
        val payload = File(directory.canonicalFile, item.payloadName).canonicalFile
        item.takeIf { payload.parentFile == directory.canonicalFile && payload.isFile }
    } catch (_: Exception) {
        null
    }

    private fun ensureFreeSpace(bytesNeeded: Long) {
        val available = StatFs(context.filesDir.absolutePath).availableBytes
        if (available - RESERVED_FREE_BYTES < bytesNeeded) {
            throw IOException("No hay suficiente espacio libre para guardar este archivo.")
        }
    }

    private fun readSourceMetadata(resolver: ContentResolver, uri: Uri): SourceMetadata {
        var name: String? = null
        var size: Long? = null
        try {
            resolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    name = cursor.stringOrNull(OpenableColumns.DISPLAY_NAME)
                    size = cursor.longOrNull(OpenableColumns.SIZE)
                }
            }
        } catch (_: Exception) {
            // Optional metadata only.
        }
        return SourceMetadata(name, size)
    }

    private fun Cursor.stringOrNull(columnName: String): String? {
        val index = getColumnIndex(columnName)
        return if (index >= 0 && !isNull(index)) getString(index) else null
    }

    private fun Cursor.longOrNull(columnName: String): Long? {
        val index = getColumnIndex(columnName)
        return if (index >= 0 && !isNull(index)) getLong(index) else null
    }

    private fun fallbackNameFor(mimeType: String): String {
        val suffix = when (mimeType) {
            "application/pdf" -> ".pdf"
            "application/vnd.android.package-archive" -> ".apk"
            "image/jpeg" -> ".jpg"
            "image/png" -> ".png"
            "image/webp" -> ".webp"
            "video/mp4" -> ".mp4"
            "text/plain" -> ".txt"
            else -> ""
        }
        return "archivo-${System.currentTimeMillis()}$suffix"
    }

    private fun normalizeMimeType(value: String?): String {
        val candidate = value?.trim()?.lowercase().orEmpty()
        return candidate.takeIf {
            it.length in 3..120 && it.contains('/') && !it.contains('\n') && !it.contains('\r')
        } ?: "application/octet-stream"
    }

    private fun isSafeId(value: String): Boolean =
        value.length == 36 && value.all { it.isLetterOrDigit() || it == '-' }

    private data class SourceMetadata(val displayName: String?, val byteSize: Long?)
}
