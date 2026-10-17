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
    fun installedApplicationsAreSeparateFromApkFiles() {
        assertFalse(PickerCategory.APPLICATIONS.matches(INSTALLED_APP_MIME_TYPE))
        assertTrue(PickerCategory.APK.matches(INSTALLED_APP_MIME_TYPE))
    }

    @Test
    fun installedApplicationsRespectCallerMimeFilter() {
        assertTrue(installedAppsCompatible(listOf("*/*")))
        assertTrue(installedAppsCompatible(listOf("application/*")))
        assertTrue(installedAppsCompatible(listOf(INSTALLED_APP_MIME_TYPE)))
        assertFalse(installedAppsCompatible(listOf("image/*")))
        assertFalse(installedAppsCompatible(listOf("application/pdf")))
    }

    @Test
    fun everyTypedCategoryRejectsUnrelatedContent() {
        assertFalse(PickerCategory.IMAGES.matches("application/pdf"))
        assertFalse(PickerCategory.PDF.matches("image/png"))
        assertFalse(PickerCategory.APK.matches("text/plain"))
    }
}
