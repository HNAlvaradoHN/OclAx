package io.github.hnalvaradohn.oclax.share

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareIngestorPolicyTest {
    @Test
    fun incomingUriCount_acceptsConfiguredLimit() {
        assertTrue(isSupportedIncomingUriCount(MAX_INCOMING_SHARED_URIS))
    }

    @Test
    fun incomingUriCount_rejectsAboveConfiguredLimit() {
        assertFalse(isSupportedIncomingUriCount(MAX_INCOMING_SHARED_URIS + 1))
    }

    @Test
    fun incomingUriCount_rejectsNegativeValues() {
        assertFalse(isSupportedIncomingUriCount(-1))
    }
}
