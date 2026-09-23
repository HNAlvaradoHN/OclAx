package io.github.hnalvaradohn.oclax.data

import io.github.hnalvaradohn.oclax.model.StoredItem
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RetentionPolicyTest {
    private val now = 2_000_000_000_000L

    @Test
    fun `unpinned item expires after configured retention`() {
        val item = item(createdAt = now - 25L * 60L * 60L * 1000L)
        assertTrue(RetentionPolicy.shouldExpire(item, now, 24))
    }

    @Test
    fun `pinned item never expires`() {
        val item = item(createdAt = now - 30L * 24L * 60L * 60L * 1000L, pinned = true)
        assertFalse(RetentionPolicy.shouldExpire(item, now, 1))
    }

    @Test
    fun `never retention keeps unpinned items`() {
        val item = item(createdAt = 1L)
        assertFalse(RetentionPolicy.shouldExpire(item, now, 0))
    }

    @Test
    fun `unsupported retention falls back to 24 hours`() {
        val item = item(createdAt = now - 25L * 60L * 60L * 1000L)
        assertTrue(RetentionPolicy.shouldExpire(item, now, 999))
    }

    private fun item(createdAt: Long, pinned: Boolean = false) = StoredItem(
        id = "00000000-0000-0000-0000-000000000000",
        displayName = "test.txt",
        mimeType = "text/plain",
        payloadName = "payload.txt",
        byteSize = 4,
        createdAt = createdAt,
        pinned = pinned,
    )
}
