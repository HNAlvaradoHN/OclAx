package io.github.hnalvaradohn.oclax.picker

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PickerCategoryTest {
    @Test
    fun recentAcceptsEveryContentType() {
        assertTrue(PickerCategory.RECENT.matches("application/pdf"))
        assertTrue(PickerCategory.RECENT.matches("image/jpeg"))
    }

    @Test
    fun pinnedOnlyMatchesPinnedOclaxItems() {
        assertTrue(PickerCategory.PINNED.matches("application/pdf", pinned = true))
        assertFalse(PickerCategory.PINNED.matches("application/pdf", pinned = false))
    }

    @Test
    fun contentCategoriesMatchSameRulesAsMainApp() {
        assertTrue(PickerCategory.IMAGES.matches("image/png"))
        assertTrue(PickerCategory.PDF.matches("application/pdf"))
        assertTrue(PickerCategory.TEXT.matches("text/plain"))
        assertTrue(PickerCategory.VIDEO.matches("video/mp4"))
        assertFalse(PickerCategory.AUDIO.matches("video/mp4"))
    }

    @Test
    fun everyTypedCategoryRejectsUnrelatedContent() {
        assertFalse(PickerCategory.IMAGES.matches("application/pdf"))
        assertFalse(PickerCategory.PDF.matches("image/png"))
        assertFalse(PickerCategory.APK.matches("text/plain"))
    }

}
