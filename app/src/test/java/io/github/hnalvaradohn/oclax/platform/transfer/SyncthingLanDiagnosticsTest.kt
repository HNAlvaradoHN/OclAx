package io.github.hnalvaradohn.oclax.platform.transfer

import org.json.JSONObject
import org.junit.Assert.assertEquals
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
            runtimeHealth = LanRuntimeHealth(
                ipv4LocalDiscoveryHealthy = false,
                lanListenerHealthy = false,
            ),
        )

        assertTrue(message.contains("quedó pausado"))
    }

    @Test
    fun runtimeHealthRecognizesHealthyLocalDiscoveryAndListener() {
        val health = inspectLanRuntimeHealth(
            JSONObject(
                """
                {
                  "discoveryStatus": {
                    "IPv4 local broadcast discovery on port 21027": {"error": null},
                    "IPv6 local multicast discovery": {"error": null}
                  },
                  "connectionServiceStatus": {
                    "tcp4://0.0.0.0:22000": {"error": null}
                  }
                }
                """.trimIndent(),
            ),
        )

        assertEquals(true, health.ipv4LocalDiscoveryHealthy)
        assertEquals(true, health.lanListenerHealthy)
    }

    @Test
    fun runtimeHealthReportsDiscoveryFailureWithoutLeakingRawError() {
        val health = inspectLanRuntimeHealth(
            JSONObject(
                """
                {
                  "discoveryStatus": {
                    "IPv4 local broadcast discovery on port 21027": {
                      "error": "bind udp 0.0.0.0:21027: private-address"
                    }
                  },
                  "connectionServiceStatus": {
                    "tcp4://0.0.0.0:22000": {"error": null}
                  }
                }
                """.trimIndent(),
            ),
        )
        val message = describeLanTimeout(
            discoveredLocally = false,
            peerPaused = false,
            runtimeHealth = health,
        )

        assertEquals(false, health.ipv4LocalDiscoveryHealthy)
        assertTrue(message.contains("Discovery local IPv4 no quedó activo"))
        assertTrue(!message.contains("private-address"))
    }

    @Test
    fun runtimeHealthReportsListenerFailureBeforeNetworkAdvice() {
        val health = inspectLanRuntimeHealth(
            JSONObject(
                """
                {
                  "discoveryStatus": {
                    "IPv4 local broadcast discovery on port 21027": {"error": null}
                  },
                  "connectionServiceStatus": {
                    "tcp4://0.0.0.0:22000": {"error": "listen failed"}
                  }
                }
                """.trimIndent(),
            ),
        )
        val message = describeLanTimeout(
            discoveredLocally = false,
            peerPaused = false,
            runtimeHealth = health,
        )

        assertEquals(false, health.lanListenerHealthy)
        assertTrue(message.contains("listener LAN"))
    }

    @Test
    fun healthyRuntimePointsToLanBroadcastPathWhenPeerIsMissing() {
        val message = describeLanTimeout(
            discoveredLocally = false,
            peerPaused = false,
            runtimeHealth = LanRuntimeHealth(
                ipv4LocalDiscoveryHealthy = true,
                lanListenerHealthy = true,
            ),
        )

        assertTrue(message.contains("Discovery local y el listener LAN están activos"))
        assertTrue(message.contains("misma Wi-Fi"))
        assertTrue(message.contains("broadcast"))
    }
}
