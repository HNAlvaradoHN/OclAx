package io.github.hnalvaradohn.oclax.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SafeNamesTest {
    @Test
    fun sanitize_removesPathAndUnsafeCharacters() {
        assertEquals("factura_.pdf", SafeNames.sanitize("../carpeta/factura?.pdf"))
    }

    @Test
    fun sanitize_usesFallbackForBlankNames() {
        assertEquals("archivo", SafeNames.sanitize("..."))
    }

    @Test
    fun extension_acceptsNormalExtensionsAndRejectsLongGarbage() {
        assertEquals("docx", SafeNames.extension("reporte.DOCX"))
        assertNull(SafeNames.extension("archivo.abcdefghijklmnopq"))
    }

    @Test
    fun storageFileName_preservesUsefulType() {
        assertEquals("content.pdf", SafeNames.storageFileName("reporte.pdf", "application/pdf"))
        assertEquals("content.apk", SafeNames.storageFileName("sin-extension", "application/vnd.android.package-archive"))
    }
}
