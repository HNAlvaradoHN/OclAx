package io.github.hnalvaradohn.oclax.picker

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PickerMimeMatcherTest {
    @Test
    fun wildcardAcceptsAnyValidMime() {
        assertTrue(PickerMimeMatcher.matches("application/pdf", listOf("*/*")))
        assertTrue(PickerMimeMatcher.matches("image/jpeg", listOf("*/*")))
    }

    @Test
    fun familyWildcardOnlyAcceptsSameFamily() {
        assertTrue(PickerMimeMatcher.matches("image/png", listOf("image/*")))
        assertFalse(PickerMimeMatcher.matches("video/mp4", listOf("image/*")))
    }

    @Test
    fun exactMimeOnlyAcceptsExactType() {
        assertTrue(PickerMimeMatcher.matches("application/pdf", listOf("application/pdf")))
        assertFalse(PickerMimeMatcher.matches("application/zip", listOf("application/pdf")))
    }

    @Test
    fun multipleRequestedTypesAreUnioned() {
        val requested = listOf("image/*", "application/pdf")

        assertTrue(PickerMimeMatcher.matches("image/webp", requested))
        assertTrue(PickerMimeMatcher.matches("application/pdf", requested))
        assertFalse(PickerMimeMatcher.matches("audio/mpeg", requested))
    }

    @Test
    fun mimeParametersAndCaseAreNormalized() {
        assertTrue(
            PickerMimeMatcher.matches(
                "IMAGE/JPEG; charset=binary",
                listOf("image/*"),
            ),
        )
    }

    @Test
    fun invalidRequestedTypesDoNotBroadenSelection() {
        assertFalse(PickerMimeMatcher.matches("image/jpeg", listOf("not-a-mime")))
    }
}
