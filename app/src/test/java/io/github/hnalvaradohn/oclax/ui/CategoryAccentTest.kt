package io.github.hnalvaradohn.oclax.ui

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CategoryAccentTest {
    @Test
    fun darkThemeUsesSemanticCategoryColors() {
        assertEquals(Color(0xFFFF8A2A), categoryAccentFor("ALL", true)?.icon)
        assertEquals(Color(0xFFFFD54A), categoryAccentFor("PINNED", true)?.icon)
        assertEquals(Color(0xFF2EE6A6), categoryAccentFor("IMAGES", true)?.icon)
        assertEquals(Color(0xFF4DB8FF), categoryAccentFor("DOCUMENTS", true)?.icon)
        assertEquals(Color(0xFFFF626E), categoryAccentFor("PDF", true)?.icon)
        assertEquals(Color(0xFF9BEF45), categoryAccentFor("APK", true)?.icon)
        assertEquals(Color(0xFFB98AFF), categoryAccentFor("TEXT", true)?.icon)
        assertEquals(Color(0xFFFF61B5), categoryAccentFor("VIDEO", true)?.icon)
        assertEquals(Color(0xFF4CD7F7), categoryAccentFor("AUDIO", true)?.icon)
    }

    @Test
    fun deviceAndPickerAliasesReuseTheExpectedAccents() {
        assertEquals(
            categoryAccentFor("APK", true),
            categoryAccentFor("apps", true),
        )
        assertEquals(
            categoryAccentFor("ALL", true),
            categoryAccentFor("RECENT", true),
        )
    }

    @Test
    fun lightThemeKeepsCategoryIdentityWithReadableForegrounds() {
        assertEquals(Color(0xFFB42330), categoryAccentFor("pdf", false)?.icon)
        assertEquals(Color(0xFF477A00), categoryAccentFor("applications", false)?.icon)
        assertEquals(Color(0xFF6842A5), categoryAccentFor("text", false)?.icon)
    }

    @Test
    fun unknownCategoryFallsBackToThemeColors() {
        assertNull(categoryAccentFor("future-category", true))
        assertNull(categoryAccentFor("future-category", false))
    }
}
