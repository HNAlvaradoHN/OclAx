package io.github.hnalvaradohn.oclax.platform.transfer

import org.junit.Assert.assertFalse
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
        assertFalse(command.any { it.contains("0.0.0.0") })
        assertFalse(command.any { it.contains("api", ignoreCase = true) && it.contains("key", ignoreCase = true) })
    }
}
