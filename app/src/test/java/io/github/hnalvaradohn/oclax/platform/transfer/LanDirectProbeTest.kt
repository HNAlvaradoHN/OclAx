package io.github.hnalvaradohn.oclax.platform.transfer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LanDirectProbeTest {
    @Test
    fun privateLanRangesMatchTransportPolicy() {
        assertTrue(isLanPrivateIpv4("10.1.2.3"))
        assertTrue(isLanPrivateIpv4("169.254.9.8"))
        assertTrue(isLanPrivateIpv4("172.16.0.1"))
        assertTrue(isLanPrivateIpv4("172.31.255.254"))
        assertTrue(isLanPrivateIpv4("192.168.50.7"))

        assertFalse(isLanPrivateIpv4("172.32.0.1"))
        assertFalse(isLanPrivateIpv4("100.64.0.1"))
        assertFalse(isLanPrivateIpv4("8.8.8.8"))
        assertFalse(isLanPrivateIpv4("not-an-ip"))
    }

    @Test
    fun slash24ProbeStaysInSubnetAndExcludesSelf() {
        val candidates = lanSubnetProbeCandidates(
            localAddress = "192.168.12.44",
            prefixLength = 24,
        )

        assertEquals(253, candidates.size)
        assertTrue("192.168.12.1" in candidates)
        assertTrue("192.168.12.254" in candidates)
        assertFalse("192.168.12.44" in candidates)
        assertFalse("192.168.12.0" in candidates)
        assertFalse("192.168.12.255" in candidates)
    }

    @Test
    fun widerNetworksAreBoundedToCurrentSlash24() {
        val candidates = lanSubnetProbeCandidates(
            localAddress = "10.44.19.22",
            prefixLength = 16,
        )

        assertEquals(253, candidates.size)
        assertTrue(candidates.all { it.startsWith("10.44.19.") })
        assertFalse("10.44.18.22" in candidates)
    }

    @Test
    fun narrowerSubnetUsesActualPrefix() {
        val candidates = lanSubnetProbeCandidates(
            localAddress = "192.168.1.130",
            prefixLength = 25,
        )

        assertEquals(125, candidates.size)
        assertTrue("192.168.1.129" in candidates)
        assertTrue("192.168.1.254" in candidates)
        assertFalse("192.168.1.127" in candidates)
        assertFalse("192.168.1.130" in candidates)
    }

    @Test
    fun publicOrInvalidNetworkProducesNoProbeTargets() {
        assertTrue(
            lanSubnetProbeCandidates(
                localAddress = "8.8.8.8",
                prefixLength = 24,
            ).isEmpty(),
        )
        assertTrue(
            lanSubnetProbeCandidates(
                localAddress = "192.168.1.5",
                prefixLength = 31,
            ).isEmpty(),
        )
    }

    @Test
    fun maxHostsCapsProbeSize() {
        val candidates = lanSubnetProbeCandidates(
            localAddress = "192.168.1.5",
            prefixLength = 24,
            maxHosts = 10,
        )

        assertEquals(10, candidates.size)
    }
}
