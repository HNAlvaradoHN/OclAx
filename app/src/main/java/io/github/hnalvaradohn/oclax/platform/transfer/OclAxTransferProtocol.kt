package io.github.hnalvaradohn.oclax.platform.transfer

import java.util.UUID

internal const val OCLAX_TRANSFER_FOLDER_PREFIX = "oclax-"
internal const val OCLAX_TRANSFER_LABEL_PREFIX = "OclAx · "
internal const val OCLAX_TRANSFER_MANIFEST = ".oclax-manifest.json"
internal const val OCLAX_TRANSFER_ACK = ".oclax-ack.json"
internal const val OCLAX_TRANSFER_PAYLOAD = "payload.bin"

internal data class IncomingTransferOffer(
    val folderId: String,
    val senderDeviceId: String,
    val displayNameHint: String,
)

internal data class TransferProgress(
    val percent: Int?,
    val message: String,
)

internal data class TransferManifest(
    val transferId: String,
    val senderDeviceId: String,
    val displayName: String,
    val mimeType: String,
    val byteSize: Long,
)

internal data class ReceivedTransferPayload(
    val offer: IncomingTransferOffer,
    val manifest: TransferManifest,
    val payloadPath: String,
)

internal fun newOclAxTransferFolderId(): String =
    OCLAX_TRANSFER_FOLDER_PREFIX + UUID.randomUUID().toString().replace("-", "")

internal fun isOclAxTransferFolderId(value: String): Boolean {
    if (!value.startsWith(OCLAX_TRANSFER_FOLDER_PREFIX)) return false
    val suffix = value.removePrefix(OCLAX_TRANSFER_FOLDER_PREFIX)
    return suffix.length == 32 && suffix.all { it in '0'..'9' || it in 'a'..'f' }
}

internal fun transferLabelFor(displayName: String): String {
    val compact = displayName
        .replace(Regex("[\\r\\n\\t]+"), " ")
        .trim()
        .ifBlank { "archivo" }
        .take(80)
    return OCLAX_TRANSFER_LABEL_PREFIX + compact
}

internal fun displayNameHintFromTransferLabel(label: String): String? =
    label
        .takeIf { it.startsWith(OCLAX_TRANSFER_LABEL_PREFIX) }
        ?.removePrefix(OCLAX_TRANSFER_LABEL_PREFIX)
        ?.replace(Regex("[\\r\\n\\t]+"), " ")
        ?.trim()
        ?.take(80)
        ?.takeIf { it.isNotBlank() }

internal fun transferPercent(value: Double): Int =
    value.coerceIn(0.0, 100.0).toInt()
