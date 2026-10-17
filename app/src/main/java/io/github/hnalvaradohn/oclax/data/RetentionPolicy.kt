package io.github.hnalvaradohn.oclax.data

import io.github.hnalvaradohn.oclax.model.StoredItem

object RetentionPolicy {
    const val DEFAULT_HOURS = 24
    val allowedHours = setOf(1, 24, 72, 168, 0)

    fun normalizeHours(value: Int): Int = value.takeIf { it in allowedHours } ?: DEFAULT_HOURS

    fun expirationAt(item: StoredItem, retentionHours: Int): Long? {
        if (item.pinned) return null
        val hours = normalizeHours(retentionHours)
        if (hours == 0) return null
        return item.createdAt + hours * 60L * 60L * 1000L
    }

    fun shouldExpire(item: StoredItem, nowMillis: Long, retentionHours: Int): Boolean =
        expirationAt(item, retentionHours)?.let { it <= nowMillis } ?: false
}
