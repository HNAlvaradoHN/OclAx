package io.github.hnalvaradohn.oclax.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PairedDeviceStoreTest {
    @Test
    fun normalizeDeviceIdAcceptsCompactOrFormattedIds() {
        val compact =
            "ABCDEFG" +
                "HIJKLMN" +
                "OPQRSTU" +
                "VWXYZ23" +
                "4567ABC" +
                "DEFGHIJ" +
                "KLMNOPQ" +
                "RSTUVWX"

        val expected = compact.chunked(7).joinToString("-")

        assertEquals(expected, PairedDeviceStore.normalizeDeviceId(compact.lowercase()))
        assertEquals(expected, PairedDeviceStore.normalizeDeviceId(expected))
    }

    @Test
    fun normalizeDeviceIdRejectsWrongLengthOrCharacters() {
        assertNull(PairedDeviceStore.normalizeDeviceId("ABC"))
        assertNull(
            PairedDeviceStore.normalizeDeviceId(
                "ABCDEFG-HIJKLMN-OPQRSTU-VWXYZ23-4567ABC-DEFGHIJ-KLMNOPQ-RSTUVX1",
            ),
        )
    }
}
