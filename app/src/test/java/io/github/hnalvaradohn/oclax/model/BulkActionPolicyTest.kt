package io.github.hnalvaradohn.oclax.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BulkActionPolicyTest {
    @Test
    fun identicalMimeTypesRemainSpecific() {
        assertEquals(
            "image/png",
            bulkShareMimeType(listOf("image/png", "image/png")),
        )
    }

    @Test
    fun mixedMimeTypesUseWildcard() {
        assertEquals(
            "*/*",
            bulkShareMimeType(listOf("image/png", "application/pdf")),
        )
    }

    @Test
    fun pinTargetPinsWhenAnySelectedItemIsNotPinned() {
        assertTrue(bulkPinTarget(listOf(true, false, true)))
        assertFalse(bulkPinTarget(listOf(true, true)))
    }
}
