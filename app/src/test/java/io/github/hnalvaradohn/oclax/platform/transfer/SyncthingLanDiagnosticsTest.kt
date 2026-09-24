package io.github.hnalvaradohn.oclax.platform.transfer

import org.junit.Assert.assertTrue
import org.junit.Test

class SyncthingLanDiagnosticsTest {
    @Test
    fun timeoutExplainsMissingLocalDiscovery() {
        val message = describeLanTimeout(
            discoveredLocally = false,
            peerPaused = false,
        )

        assertTrue(message.contains("no apareció en discovery local"))
        assertTrue(message.contains("Probar LAN en los dos"))
    }

    @Test
    fun timeoutDistinguishesPeerSeenButNotConnected() {
        val message = describeLanTimeout(
            discoveredLocally = true,
            peerPaused = false,
        )

        assertTrue(message.contains("apareció en discovery local"))
        assertTrue(message.contains("no se completó la conexión"))
    }

    @Test
    fun timeoutReportsUnexpectedPausedPeerFirst() {
        val message = describeLanTimeout(
            discoveredLocally = true,
            peerPaused = true,
        )

        assertTrue(message.contains("quedó pausado"))
    }
}
