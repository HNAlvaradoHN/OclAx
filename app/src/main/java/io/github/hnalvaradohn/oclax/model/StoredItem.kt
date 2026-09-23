package io.github.hnalvaradohn.oclax.model

data class StoredItem(
    val id: String,
    val displayName: String,
    val mimeType: String,
    val payloadName: String,
    val byteSize: Long,
    val createdAt: Long,
    val pinned: Boolean,
)
