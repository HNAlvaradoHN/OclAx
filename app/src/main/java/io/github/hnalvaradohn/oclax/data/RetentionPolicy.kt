package io.github.hnalvaradohn.oclax.data

import io.github.hnalvaradohn.oclax.model.StoredItem

object RetentionPolicy {
    const val DEFAULT_HOURS = 24
    val allowedHours = setOf(1, 24, 72, 168, 0)

    fun normalizeHours(value: Int): Int = value.takeIf { it in allowedHours } ?: DEFAULT_HOURS

    fun shouldExpire(item: StoredItem, nowMillis: Long, retentionHours: Int): Boolean {
        if (item.pinned) return false
        val hours = normalizeHours(retentionHours)
        if (hours == 0) return false
        val maxAgeMillis = hours * 60L * 60L * 1000L
        return item.createdAt <= nowMillis - maxAgeMillis
    }
}
