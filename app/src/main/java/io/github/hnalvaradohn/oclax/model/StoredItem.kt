package io.github.hnalvaradohn.oclax.model

data class StoredItem(
    val id: String,
    val displayName: String,
    val mimeType: String,
    val payloadName: String,
    val byteSize: Long,
    val createdAt: Long,
    val pinned: Boolean,
    val sourceUri: String? = null,
    val sourceDisplayName: String? = null,
    val sourceByteSize: Long? = null,
    val sourceAddedAt: Long? = null,
    val deleteSourceOnExpiry: Boolean = false,
)
