package io.github.hnalvaradohn.oclax.transfer

import io.github.hnalvaradohn.oclax.data.ItemStore
import io.github.hnalvaradohn.oclax.model.StoredItem
import io.github.hnalvaradohn.oclax.platform.transfer.IncomingTransferOffer
import io.github.hnalvaradohn.oclax.platform.transfer.TransferProgress
import io.github.hnalvaradohn.oclax.platform.transfer.TransferRuntimeController
import java.io.File

internal class FileTransferCoordinator(
    private val store: ItemStore,
    private val runtime: TransferRuntimeController,
) {
    fun send(
        item: StoredItem,
        deviceId: String,
        onProgress: (TransferProgress) -> Unit,
    ) {
        runtime.sendFile(
            deviceId = deviceId,
            source = store.payloadFile(item),
            displayName = item.displayName,
            mimeType = item.mimeType,
            byteSize = item.byteSize,
            onProgress = onProgress,
        )
    }

    fun pending(deviceId: String): List<IncomingTransferOffer> =
        runtime.pendingTransfers(deviceId)

    fun receive(
        offer: IncomingTransferOffer,
        onProgress: (TransferProgress) -> Unit,
    ): StoredItem {
        val payload = runtime.receiveTransfer(offer, onProgress)
        val item = store.importTransferFile(
            source = File(payload.payloadPath),
            displayName = payload.manifest.displayName,
            mimeType = payload.manifest.mimeType,
            expectedBytes = payload.manifest.byteSize,
        )
        runtime.acknowledgeReceived(payload)
        return item
    }

    fun reject(offer: IncomingTransferOffer) {
        runtime.rejectTransfer(offer)
    }
}
