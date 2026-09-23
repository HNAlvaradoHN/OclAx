package io.github.hnalvaradohn.oclax.platform.transfer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncthingLanPolicyTest {
    @Test
    fun lanPolicyUsesOnlyPrivateOrLinkLocalIpv4Ranges() {
        assertEquals(
            listOf(
                "10.0.0.0/8",
                "169.254.0.0/16",
                "172.16.0.0/12",
                "192.168.0.0/16",
            ),
            SyncthingLanPolicy.ALLOWED_NETWORKS,
        )
        assertFalse(SyncthingLanPolicy.ALLOWED_NETWORKS.contains("0.0.0.0/0"))
        assertFalse(SyncthingLanPolicy.ALLOWED_NETWORKS.contains("::/0"))
        assertFalse(SyncthingLanPolicy.ALLOWED_NETWORKS.contains("100.64.0.0/10"))
    }

    @Test
    fun lanListenerIsIpv4AndUsesSyncthingPort() {
        assertEquals("tcp4://0.0.0.0:22000", SyncthingLanPolicy.LISTEN_ADDRESS)
        assertTrue(SyncthingLanPolicy.LISTEN_ADDRESS.startsWith("tcp4://"))
    }
}
