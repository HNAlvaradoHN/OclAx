package io.github.hnalvaradohn.oclax.platform.transfer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncthingRuntimeConfigTest {
    @Test
    fun commandLocksGuiToLoopbackAndDisablesSelfUpgrade() {
        val command = SyncthingRuntimeConfig.buildCommand(
            binaryPath = "/private/libsyncthingnative.so",
            homePath = "/private/home",
            logPath = "/private/home/runtime.log",
        )

        assertTrue(command.contains("--gui-address=127.0.0.1:8384"))
        assertTrue(command.contains("--no-upgrade"))
        assertTrue(command.contains("--no-browser"))
        assertTrue(command.contains("--no-restart"))
        assertTrue(command.contains("--no-port-probing"))
        assertTrue(command.contains("--paused"))
        assertFalse(command.any { it.contains("0.0.0.0") })
        assertFalse(command.any { it.contains("api", ignoreCase = true) && it.contains("key", ignoreCase = true) })
    }

    @Test
    fun androidRuntimeEnvironmentSkipsOuterMonitorAndUsesPrivateTempStorage() {
        val environment = SyncthingRuntimeConfig.privateEnvironmentOverrides(
            homePath = "/private/home",
            tempPath = "/private/cache",
            apiKey = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFG",
            gatewayIpv4 = "192.168.1.1",
        )

        assertEquals("yes", environment["STMONITORED"])
        assertEquals("/private/home", environment["STHOMEDIR"])
        assertEquals("/private/cache", environment["SQLITE_TMPDIR"])
        assertEquals("127.0.0.1:8384", environment["STGUIADDRESS"])
        assertEquals("192.168.1.1", environment["FALLBACK_NET_GATEWAY_IPV4"])
    }

    @Test
    fun androidRuntimeEnvironmentOmitsMissingGatewayHint() {
        val environment = SyncthingRuntimeConfig.privateEnvironmentOverrides(
            homePath = "/private/home",
            tempPath = "/private/cache",
            apiKey = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFG",
            gatewayIpv4 = null,
        )

        assertNull(environment["FALLBACK_NET_GATEWAY_IPV4"])
    }

    @Test
    fun generateCommandCreatesConfigWithoutStartingServeMode() {
        val command = SyncthingRuntimeConfig.buildGenerateCommand(
            binaryPath = "/private/libsyncthingnative.so",
            homePath = "/private/home",
        )

        assertTrue(command.contains("generate"))
        assertTrue(command.contains("--no-port-probing"))
        assertFalse(command.contains("serve"))
        assertFalse(command.any { it.contains("0.0.0.0") })
    }
}
