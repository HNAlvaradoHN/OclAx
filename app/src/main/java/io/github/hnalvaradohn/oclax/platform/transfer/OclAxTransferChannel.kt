package io.github.hnalvaradohn.oclax.platform.transfer

import android.content.Context
import android.os.StatFs
import io.github.hnalvaradohn.oclax.data.ItemStore
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.util.concurrent.TimeUnit

internal class OclAxTransferChannel(
    context: Context,
    private val client: SyncthingRestClient,
) {
    private val root = File(context.applicationContext.filesDir, "oclax/transfers").apply { mkdirs() }

    fun send(
        deviceId: String,
        ownDeviceId: String,
        source: File,
        displayName: String,
        mimeType: String,
        byteSize: Long,
        onProgress: (TransferProgress) -> Unit,
    ) {
        require(byteSize in 1..ItemStore.MAX_ITEM_BYTES) {
            "El archivo supera el límite seguro de OclAx."
        }
        check(source.isFile && source.length() == byteSize) {
            "El archivo local cambió antes del envío."
        }
        ensureStagingSpace(byteSize)

        val folderId = newOclAxTransferFolderId()
        val directory = transferDirectory("outgoing", folderId)
        if (!directory.mkdirs()) {
            throw IOException("No se pudo preparar el envío.")
        }

        try {
            copyBounded(source, File(directory, OCLAX_TRANSFER_PAYLOAD), byteSize)
            writeManifest(
                File(directory, OCLAX_TRANSFER_MANIFEST),
                TransferManifest(
                    transferId = folderId,
                    senderDeviceId = ownDeviceId,
                    displayName = displayName,
                    mimeType = mimeType,
                    byteSize = byteSize,
                ),
            )

            client.configureTransferFolder(
                folderId = folderId,
                label = transferLabelFor(displayName),
                path = directory.absolutePath,
                deviceId = deviceId,
            )
            client.scanFolder(folderId)
            waitForAck(folderId, directory, deviceId, byteSize, onProgress)
            cleanupAfterAck(folderId, directory, deviceId)
            onProgress(TransferProgress(100, "Recibido por el otro dispositivo."))
        } catch (error: Exception) {
            runCatching { client.removeTransferFolder(folderId) }
            directory.deleteRecursively()
            throw error
        }
    }

    fun pending(deviceId: String): List<IncomingTransferOffer> =
        client.pendingTransferFolders(deviceId)
            .mapNotNull { pending ->
                if (!isOclAxTransferFolderId(pending.folderId)) return@mapNotNull null
                val hint = displayNameHintFromTransferLabel(pending.label) ?: return@mapNotNull null
                IncomingTransferOffer(
                    folderId = pending.folderId,
                    senderDeviceId = deviceId,
                    displayNameHint = hint,
                )
            }

    fun reject(offer: IncomingTransferOffer) {
        validateOffer(offer)
        client.dismissPendingTransferFolder(
            folderId = offer.folderId,
            deviceId = offer.senderDeviceId,
        )
        transferDirectory("incoming", offer.folderId).deleteRecursively()
    }

    fun receive(
        offer: IncomingTransferOffer,
        onProgress: (TransferProgress) -> Unit,
    ): ReceivedTransferPayload {
        validateOffer(offer)
        val directory = transferDirectory("incoming", offer.folderId)
        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("No se pudo preparar la recepción.")
        }

        try {
            client.configureTransferFolder(
                folderId = offer.folderId,
                label = transferLabelFor(offer.displayNameHint),
                path = directory.absolutePath,
                deviceId = offer.senderDeviceId,
            )
            client.scanFolder(offer.folderId)

            val deadline = System.currentTimeMillis() + RECEIVE_TIMEOUT_MILLIS
            var manifest: TransferManifest? = null
            while (System.currentTimeMillis() < deadline) {
                val status = client.transferFolderStatus(offer.folderId)
                if (
                    status.globalBytes > MAX_TRANSFER_FOLDER_BYTES ||
                    status.globalFiles > MAX_TRANSFER_FOLDER_FILES ||
                    status.globalDirectories > 0 ||
                    status.globalSymlinks > 0
                ) {
                    throw IOException("La solicitud contiene más datos de los permitidos por OclAx.")
                }

                val completion = client.transferFolderCompletion(
                    folderId = offer.folderId,
                    deviceId = null,
                )
                val percent = transferPercent(completion.completion)
                onProgress(TransferProgress(percent, "Recibiendo… $percent%"))

                val manifestFile = File(directory, OCLAX_TRANSFER_MANIFEST)
                val payloadFile = File(directory, OCLAX_TRANSFER_PAYLOAD)
                if (
                    completion.needBytes == 0L &&
                    completion.needItems == 0 &&
                    manifestFile.isFile &&
                    payloadFile.isFile &&
                    !Files.isSymbolicLink(manifestFile.toPath()) &&
                    manifestFile.length() <= MAX_MANIFEST_BYTES
                ) {
                    val parsed = readManifest(manifestFile)
                    validateManifest(parsed, offer, payloadFile)
                    manifest = parsed
                    break
                }
                Thread.sleep(POLL_MILLIS)
            }

            val ready = manifest ?: throw IOException("La recepción no terminó a tiempo.")
            return ReceivedTransferPayload(
                offer = offer,
                manifest = ready,
                payloadPath = File(directory, OCLAX_TRANSFER_PAYLOAD).absolutePath,
            )
        } catch (error: Exception) {
            runCatching { client.removeTransferFolder(offer.folderId) }
            directory.deleteRecursively()
            throw error
        }
    }

    fun acknowledgeImported(payload: ReceivedTransferPayload) {
        validateOffer(payload.offer)
        check(payload.manifest.transferId == payload.offer.folderId)
        val directory = transferDirectory("incoming", payload.offer.folderId)
        val ack = File(directory, OCLAX_TRANSFER_ACK)
        ack.writeText(
            JSONObject()
                .put("version", 1)
                .put("transferId", payload.offer.folderId)
                .put("status", "imported")
                .toString(),
            Charsets.UTF_8,
        )
        client.scanFolder(payload.offer.folderId)

        val deadline = System.currentTimeMillis() + CLEANUP_WAIT_MILLIS
        val manifest = File(directory, OCLAX_TRANSFER_MANIFEST)
        val data = File(directory, OCLAX_TRANSFER_PAYLOAD)
        while (System.currentTimeMillis() < deadline) {
            if (!manifest.exists() && !data.exists() && !ack.exists()) break
            Thread.sleep(POLL_MILLIS)
        }

        runCatching { client.removeTransferFolder(payload.offer.folderId) }
        directory.deleteRecursively()
    }

    private fun waitForAck(
        folderId: String,
        directory: File,
        deviceId: String,
        byteSize: Long,
        onProgress: (TransferProgress) -> Unit,
    ) {
        val deadline = System.currentTimeMillis() + sendTimeoutMillis(byteSize)
        val ack = File(directory, OCLAX_TRANSFER_ACK)

        while (System.currentTimeMillis() < deadline) {
            if (ack.isFile && validAck(ack, folderId)) return

            val completion = client.transferFolderCompletion(
                folderId = folderId,
                deviceId = deviceId,
            )
            val percent = transferPercent(completion.completion)
            val message = when (completion.remoteState) {
                "valid" -> "Enviando… $percent%"
                "paused" -> "El otro dispositivo pausó la recepción."
                else -> "Esperando aceptación en el otro dispositivo…"
            }
            onProgress(TransferProgress(percent, message))
            Thread.sleep(POLL_MILLIS)
        }

        throw IOException("El otro dispositivo no confirmó la recepción a tiempo.")
    }

    private fun cleanupAfterAck(
        folderId: String,
        directory: File,
        deviceId: String,
    ) {
        listOf(
            OCLAX_TRANSFER_MANIFEST,
            OCLAX_TRANSFER_PAYLOAD,
            OCLAX_TRANSFER_ACK,
        ).forEach { name ->
            File(directory, name).delete()
        }
        client.scanFolder(folderId)

        val deadline = System.currentTimeMillis() + CLEANUP_WAIT_MILLIS
        while (System.currentTimeMillis() < deadline) {
            val completion = runCatching {
                client.transferFolderCompletion(folderId, deviceId)
            }.getOrNull() ?: break
            if (completion.needBytes == 0L && completion.needItems == 0) break
            Thread.sleep(POLL_MILLIS)
        }

        client.removeTransferFolder(folderId)
        directory.deleteRecursively()
    }

    private fun validateManifest(
        manifest: TransferManifest,
        offer: IncomingTransferOffer,
        payloadFile: File,
    ) {
        check(manifest.transferId == offer.folderId) {
            "La solicitud recibida no coincide con el contenido."
        }
        check(manifest.senderDeviceId == offer.senderDeviceId) {
            "El remitente no coincide con el dispositivo emparejado."
        }
        check(manifest.byteSize in 1..ItemStore.MAX_ITEM_BYTES) {
            "El archivo recibido supera el límite seguro."
        }
        check(payloadFile.isFile && !Files.isSymbolicLink(payloadFile.toPath())) {
            "El archivo recibido no es un archivo regular seguro."
        }
        check(payloadFile.length() == manifest.byteSize) {
            "El archivo recibido está incompleto."
        }
        check(manifest.displayName.isNotBlank() && manifest.displayName.length <= 255) {
            "El nombre recibido no es válido."
        }
        check(manifest.mimeType.isNotBlank() && manifest.mimeType.length <= 200) {
            "El tipo recibido no es válido."
        }
    }

    private fun validateOffer(offer: IncomingTransferOffer) {
        check(isOclAxTransferFolderId(offer.folderId)) {
            "La solicitud no pertenece al protocolo OclAx."
        }
        check(offer.senderDeviceId.isNotBlank()) {
            "La solicitud no tiene un remitente válido."
        }
    }

    private fun transferDirectory(direction: String, folderId: String): File {
        check(isOclAxTransferFolderId(folderId))
        val parent = File(root, direction).apply { mkdirs() }.canonicalFile
        val child = File(parent, folderId).canonicalFile
        check(child.parentFile == parent) {
            "Ruta de transferencia inválida."
        }
        return child
    }

    private fun writeManifest(file: File, manifest: TransferManifest) {
        file.writeText(
            JSONObject()
                .put("version", 1)
                .put("transferId", manifest.transferId)
                .put("senderDeviceId", manifest.senderDeviceId)
                .put("displayName", manifest.displayName)
                .put("mimeType", manifest.mimeType)
                .put("byteSize", manifest.byteSize)
                .toString(),
            Charsets.UTF_8,
        )
    }

    private fun readManifest(file: File): TransferManifest {
        val json = JSONObject(file.readText(Charsets.UTF_8))
        check(json.optInt("version", 0) == 1) {
            "La versión de transferencia recibida no es compatible."
        }
        return TransferManifest(
            transferId = json.getString("transferId"),
            senderDeviceId = json.getString("senderDeviceId"),
            displayName = json.getString("displayName"),
            mimeType = json.getString("mimeType"),
            byteSize = json.getLong("byteSize"),
        )
    }

    private fun validAck(file: File, folderId: String): Boolean = runCatching {
        if (
            !file.isFile ||
            Files.isSymbolicLink(file.toPath()) ||
            file.length() > MAX_ACK_BYTES
        ) {
            return@runCatching false
        }
        val json = JSONObject(file.readText(Charsets.UTF_8))
        json.optInt("version", 0) == 1 &&
            json.optString("transferId") == folderId &&
            json.optString("status") == "imported"
    }.getOrDefault(false)

    private fun copyBounded(source: File, destination: File, expectedBytes: Long) {
        FileInputStream(source).use { input ->
            FileOutputStream(destination).use { output ->
                val buffer = ByteArray(COPY_BUFFER_BYTES)
                var total = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > ItemStore.MAX_ITEM_BYTES) {
                        throw IOException("El archivo supera el límite seguro de OclAx.")
                    }
                    output.write(buffer, 0, read)
                }
                output.flush()
                check(total == expectedBytes) {
                    "El archivo local cambió durante la preparación."
                }
            }
        }
    }

    private fun ensureStagingSpace(bytesNeeded: Long) {
        val available = StatFs(root.absolutePath).availableBytes
        if (available - RESERVED_FREE_BYTES < bytesNeeded) {
            throw IOException("No hay suficiente espacio libre para preparar el envío.")
        }
    }

    companion object {
        private const val COPY_BUFFER_BYTES = 256 * 1024
        private const val RESERVED_FREE_BYTES = 64L * 1024L * 1024L
        private const val MAX_TRANSFER_FOLDER_FILES = 3
        private const val MAX_MANIFEST_BYTES = 16L * 1024L
        private const val MAX_ACK_BYTES = 4L * 1024L
        private const val MAX_PROTOCOL_OVERHEAD_BYTES = 64L * 1024L
        private const val MAX_TRANSFER_FOLDER_BYTES =
            ItemStore.MAX_ITEM_BYTES + MAX_PROTOCOL_OVERHEAD_BYTES
        private const val POLL_MILLIS = 750L
        private const val MIN_TRANSFER_BYTES_PER_SECOND = 512L * 1024L
        private val MIN_SEND_TIMEOUT_MILLIS = TimeUnit.MINUTES.toMillis(5)
        private val MAX_SEND_TIMEOUT_MILLIS = TimeUnit.HOURS.toMillis(2)
        private val RECEIVE_TIMEOUT_MILLIS = TimeUnit.HOURS.toMillis(2)
        private val CLEANUP_WAIT_MILLIS = TimeUnit.SECONDS.toMillis(30)

        internal fun sendTimeoutMillis(byteSize: Long): Long {
            val estimatedTransferMillis =
                (byteSize.coerceAtLeast(1L) * 1_000L) / MIN_TRANSFER_BYTES_PER_SECOND
            return (MIN_SEND_TIMEOUT_MILLIS + estimatedTransferMillis)
                .coerceAtMost(MAX_SEND_TIMEOUT_MILLIS)
        }
    }
}
