package io.github.hnalvaradohn.oclax

import org.junit.Assert.assertEquals
import org.junit.Test

class BackNavigationPolicyTest {
    @Test
    fun categoryOrSearchReturnsToOclAxCategories() {
        assertEquals(
            HomeBackAction.CLEAR_OCLAX_VIEW,
            homeBackAction(
                sourceIsOclAx = true,
                hasContentDrillDown = true,
                exitArmed = false,
            ),
        )
    }

    @Test
    fun deviceRootReturnsToOclAxHome() {
        assertEquals(
            HomeBackAction.RETURN_TO_OCLAX,
            homeBackAction(
                sourceIsOclAx = false,
                hasContentDrillDown = false,
                exitArmed = false,
            ),
        )
    }

    @Test
    fun firstBackAtHomeShowsExitConfirmation() {
        assertEquals(
            HomeBackAction.SHOW_EXIT_CONFIRMATION,
            homeBackAction(
                sourceIsOclAx = true,
                hasContentDrillDown = false,
                exitArmed = false,
            ),
        )
    }

    @Test
    fun secondBackAtHomeExits() {
        assertEquals(
            HomeBackAction.EXIT_APP,
            homeBackAction(
                sourceIsOclAx = true,
                hasContentDrillDown = false,
                exitArmed = true,
            ),
        )
    }
}
