package io.github.hnalvaradohn.oclax.provider

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InstalledAppDocumentIdsTest {
    @Test
    fun roundTripsSingleAndDirectoryIds() {
        val packageName = "com.example.app"

        assertEquals(
            InstalledAppDocumentRef.Single(packageName),
            InstalledAppDocumentIds.parse(InstalledAppDocumentIds.single(packageName)),
        )
        assertEquals(
            InstalledAppDocumentRef.Directory(packageName),
            InstalledAppDocumentIds.parse(InstalledAppDocumentIds.directory(packageName)),
        )
    }

    @Test
    fun roundTripsPartId() {
        assertEquals(
            InstalledAppDocumentRef.Part("com.example.app", 3),
            InstalledAppDocumentIds.parse(
                InstalledAppDocumentIds.part("com.example.app", 3),
            ),
        )
    }

    @Test
    fun rejectsMalformedIds() {
        assertNull(InstalledAppDocumentIds.parse("installed-app-part:com.example.app:-1"))
        assertNull(InstalledAppDocumentIds.parse("installed-app-part:../../data:0"))
        assertNull(InstalledAppDocumentIds.parse("installed-app-single:com/example/app"))
        assertNull(InstalledAppDocumentIds.parse("unknown:com.example.app"))
    }
}
