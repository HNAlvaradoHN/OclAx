package io.github.hnalvaradohn.oclax.platform.transfer

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
        val health = evaluateLanRuntimeHealth(
            discoveryStatusPresent = true,
            discoveryEntries = listOf(
                LanRuntimeStatusEntry("IPv4 local", healthy = true),
                LanRuntimeStatusEntry("IPv6 local", healthy = true),
            ),
            connectionStatusPresent = true,
            connectionEntries = listOf(
                LanRuntimeStatusEntry("tcp://0.0.0.0:22000", healthy = true),
            ),
        )

        assertEquals(true, health.ipv4LocalDiscoveryHealthy)
        assertEquals(true, health.lanListenerHealthy)
    }

    @Test
    fun runtimeHealthReportsDiscoveryFailureWithoutLeakingRawError() {
        val health = evaluateLanRuntimeHealth(
            discoveryStatusPresent = true,
            discoveryEntries = listOf(
                LanRuntimeStatusEntry("IPv4 local", healthy = false),
            ),
            connectionStatusPresent = true,
            connectionEntries = listOf(
                LanRuntimeStatusEntry("tcp://0.0.0.0:22000", healthy = true),
            ),
        )
        val message = describeLanTimeout(
            discoveredLocally = false,
            peerPaused = false,
            runtimeHealth = health,
        )

        assertEquals(false, health.ipv4LocalDiscoveryHealthy)
        assertTrue(message.contains("Discovery local IPv4 no quedó activo"))
    }

    @Test
    fun isolatedLoopbackListenerDoesNotCountAsLanListener() {
        val health = evaluateLanRuntimeHealth(
            discoveryStatusPresent = true,
            discoveryEntries = listOf(
                LanRuntimeStatusEntry("IPv4 local", healthy = true),
            ),
            connectionStatusPresent = true,
            connectionEntries = listOf(
                LanRuntimeStatusEntry("tcp4://127.0.0.1:22000", healthy = true),
            ),
        )

        assertEquals(true, health.ipv4LocalDiscoveryHealthy)
        assertEquals(false, health.lanListenerHealthy)
    }

    @Test
    fun runtimeHealthReportsListenerFailureBeforeNetworkAdvice() {
        val health = evaluateLanRuntimeHealth(
            discoveryStatusPresent = true,
            discoveryEntries = listOf(
                LanRuntimeStatusEntry("IPv4 local", healthy = true),
            ),
            connectionStatusPresent = true,
            connectionEntries = listOf(
                LanRuntimeStatusEntry("tcp4://0.0.0.0:22000", healthy = false),
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
    fun missingRuntimeSectionsRemainUnknown() {
        val health = evaluateLanRuntimeHealth(
            discoveryStatusPresent = false,
            discoveryEntries = emptyList(),
            connectionStatusPresent = false,
            connectionEntries = emptyList(),
        )

        assertEquals(null, health.ipv4LocalDiscoveryHealthy)
        assertEquals(null, health.lanListenerHealthy)
    }

    @Test
    fun directProbeFailureExplainsClientIsolationWhenRuntimeIsHealthy() {
        val message = describeLanTimeout(
            discoveredLocally = false,
            peerPaused = false,
            runtimeHealth = LanRuntimeHealth(
                ipv4LocalDiscoveryHealthy = true,
                lanListenerHealthy = true,
            ),
            directProbeAttempted = true,
            directCandidateFound = false,
        )

        assertTrue(message.contains("tampoco se encontró"))
        assertTrue(message.contains("Wi-Fi puede estar aislando clientes"))
    }

    @Test
    fun directCandidateExplainsUnverifiedSessionWithoutClaimingIdentityMismatch() {
        val message = describeLanTimeout(
            discoveredLocally = false,
            peerPaused = false,
            runtimeHealth = LanRuntimeHealth(
                ipv4LocalDiscoveryHealthy = true,
                lanListenerHealthy = true,
            ),
            directProbeAttempted = true,
            directCandidateFound = true,
        )

        assertTrue(message.contains("puerto Syncthing candidato"))
        assertTrue(message.contains("sesión verificada"))
    }

    @Test
    fun verifiedConnectionAddressExtractsOnlyPrivateIpv4Host() {
        assertEquals(
            "192.168.1.44",
            lanPrivateIpv4FromConnectionAddress("192.168.1.44:22000"),
        )
        assertEquals(
            "10.20.30.40",
            lanPrivateIpv4FromConnectionAddress("tcp4://10.20.30.40:53117"),
        )
        assertEquals(
            "172.16.8.9",
            lanPrivateIpv4FromConnectionAddress("172.16.8.9"),
        )
        assertEquals(null, lanPrivateIpv4FromConnectionAddress("8.8.8.8:22000"))
        assertEquals(null, lanPrivateIpv4FromConnectionAddress("[fd00::1]:22000"))
        assertEquals(null, lanPrivateIpv4FromConnectionAddress(""))
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
