package io.github.hnalvaradohn.oclax.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ContentTypeTest {
    @Test
    fun `classifies common content types`() {
        assertEquals(ContentType.IMAGE, contentTypeFor("image/jpeg"))
        assertEquals(ContentType.PDF, contentTypeFor("application/pdf"))
        assertEquals(ContentType.APP, contentTypeFor("application/vnd.android.package-archive"))
        assertEquals(ContentType.DOCUMENT, contentTypeFor("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
        assertEquals(ContentType.TEXT, contentTypeFor("application/json"))
        assertEquals(ContentType.VIDEO, contentTypeFor("video/mp4"))
        assertEquals(ContentType.AUDIO, contentTypeFor("audio/mpeg"))
        assertEquals(ContentType.OTHER, contentTypeFor("application/zip"))
    }

    @Test
    fun `classification is case insensitive`() {
        assertEquals(ContentType.PDF, contentTypeFor("APPLICATION/PDF"))
        assertEquals(ContentType.IMAGE, contentTypeFor("IMAGE/PNG"))
    }
}
