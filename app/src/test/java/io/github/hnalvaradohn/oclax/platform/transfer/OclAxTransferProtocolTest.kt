package io.github.hnalvaradohn.oclax.platform.transfer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class OclAxTransferProtocolTest {
    @Test
    fun folderIdsUseStrictOclAxNamespace() {
        assertTrue(
            isOclAxTransferFolderId(
                "oclax-0123456789abcdef0123456789abcdef",
            ),
        )
        assertFalse(isOclAxTransferFolderId("default"))
        assertFalse(isOclAxTransferFolderId("oclax-012345"))
        assertFalse(
            isOclAxTransferFolderId(
                "oclax-0123456789ABCDEF0123456789ABCDEF",
            ),
        )
    }

    @Test
    fun labelsAreSingleLineAndBounded() {
        val label = transferLabelFor("  foto\nprivada.jpg  ")
        assertEquals("OclAx · foto privada.jpg", label)
        assertTrue(label.length <= OCLAX_TRANSFER_LABEL_PREFIX.length + 80)
    }

    @Test
    fun onlyOclAxLabelsExposeANameHint() {
        assertEquals(
            "documento.pdf",
            displayNameHintFromTransferLabel("OclAx · documento.pdf"),
        )
        assertEquals(null, displayNameHintFromTransferLabel("Carpeta personal"))
        assertEquals(
            "foto privada.jpg",
            displayNameHintFromTransferLabel("OclAx · foto\nprivada.jpg"),
        )
        assertTrue(
            displayNameHintFromTransferLabel("OclAx · " + "a".repeat(200))!!.length <= 80,
        )
    }

    @Test
    fun percentIsBoundedForUi() {
        assertEquals(0, transferPercent(-1.0))
        assertEquals(42, transferPercent(42.9))
        assertEquals(100, transferPercent(101.0))
    }

    @Test
    fun sendTimeoutScalesAndRemainsBounded() {
        val small = OclAxTransferChannel.sendTimeoutMillis(1024L)
        val maximumItem = OclAxTransferChannel.sendTimeoutMillis(
            4L * 1024L * 1024L * 1024L,
        )

        assertTrue(small >= TimeUnit.MINUTES.toMillis(5))
        assertTrue(maximumItem > small)
        assertEquals(TimeUnit.HOURS.toMillis(2), maximumItem)
    }
}
