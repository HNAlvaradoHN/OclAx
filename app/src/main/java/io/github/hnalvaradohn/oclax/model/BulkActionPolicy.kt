package io.github.hnalvaradohn.oclax.model

internal fun bulkShareMimeType(mimeTypes: List<String>): String {
    val normalized = mimeTypes
        .map { it.trim().lowercase() }
        .filter { it.contains('/') }
        .distinct()
    return normalized.singleOrNull() ?: "*/*"
}

internal fun bulkPinTarget(pinnedStates: List<Boolean>): Boolean =
    pinnedStates.any { pinned -> !pinned }
