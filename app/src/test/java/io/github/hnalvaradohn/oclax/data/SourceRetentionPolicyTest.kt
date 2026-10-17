package io.github.hnalvaradohn.oclax.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceRetentionPolicyTest {
    private val now = 2_000_000_000_000L

    @Test
    fun recentMediaStoreScreenshotCanDeleteOriginalOnExpiry() {
        assertTrue(
            shouldDeleteScreenshotSourceOnExpiry(
                mimeType = "image/png",
                sourceIsMediaStore = true,
                displayName = "Screenshot_2026-10-17.png",
                relativePath = "Pictures/Screenshots/",
                sourceAddedAt = now - 45_000L,
                importedAt = now,
            ),
        )
    }

    @Test
    fun screenshotOlderThanSixtySecondsKeepsOriginal() {
        assertFalse(
            shouldDeleteScreenshotSourceOnExpiry(
                mimeType = "image/png",
                sourceIsMediaStore = true,
                displayName = "Screenshot_2026-10-17.png",
                relativePath = "Pictures/Screenshots/",
                sourceAddedAt = now - 60_001L,
                importedAt = now,
            ),
        )
    }

    @Test
    fun normalImagesNeverBecomeSourceDeleteEligible() {
        assertFalse(
            shouldDeleteScreenshotSourceOnExpiry(
                mimeType = "image/jpeg",
                sourceIsMediaStore = true,
                displayName = "vacaciones.jpg",
                relativePath = "DCIM/Camera/",
                sourceAddedAt = now - 10_000L,
                importedAt = now,
            ),
        )
    }

    @Test
    fun nonMediaStoreScreenshotKeepsOriginal() {
        assertFalse(
            shouldDeleteScreenshotSourceOnExpiry(
                mimeType = "image/png",
                sourceIsMediaStore = false,
                displayName = "Screenshot.png",
                relativePath = "Pictures/Screenshots/",
                sourceAddedAt = now - 5_000L,
                importedAt = now,
            ),
        )
    }
}
