package io.github.hnalvaradohn.oclax.platform.transfer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import javax.xml.parsers.ParserConfigurationException
import org.junit.Test
import java.nio.file.Files

class SyncthingPrivateConfigTest {
    @Test
    fun hardenReplacesNetworkAndReportingDefaults() {
        val dir = Files.createTempDirectory("oclax-syncthing-config").toFile()
        try {
            val config = dir.resolve("config.xml")
            config.writeText(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <configuration version="42">
                    <gui enabled="true" tls="false">
                        <address>0.0.0.0:8384</address>
                        <apikey>unsafe-default-key</apikey>
                    </gui>
                    <options>
                        <listenAddress>default</listenAddress>
                        <listenAddress>tcp://0.0.0.0:22000</listenAddress>
                        <globalAnnounceEnabled>true</globalAnnounceEnabled>
                        <localAnnounceEnabled>true</localAnnounceEnabled>
                        <relaysEnabled>true</relaysEnabled>
                        <natEnabled>true</natEnabled>
                        <startBrowser>true</startBrowser>
                        <urAccepted>0</urAccepted>
                        <crashReportingEnabled>true</crashReportingEnabled>
                    </options>
                </configuration>
                """.trimIndent(),
            )

            val apiKey = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFG"
            SyncthingPrivateConfig.harden(config, apiKey)

            val snapshot = SyncthingPrivateConfig.snapshot(config)
            assertEquals("127.0.0.1:8384", snapshot.guiAddress)
            assertEquals(apiKey, snapshot.apiKey)
            assertEquals(
                listOf(SyncthingPrivateConfig.SAFE_LISTEN_ADDRESS),
                snapshot.listenAddresses,
            )
            assertFalse(snapshot.globalDiscovery)
            assertFalse(snapshot.localDiscovery)
            assertFalse(snapshot.relays)
            assertFalse(snapshot.nat)
            assertFalse(snapshot.startBrowser)
            assertEquals(-1, snapshot.usageReportingAccepted)
            assertFalse(snapshot.crashReporting)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun unsupportedXmlFeatureDoesNotAbortSecuritySetup() {
        val applied = SyncthingPrivateConfig.applyOptionalXmlFeature(
            feature = "unsupported-feature",
            value = true,
        ) { _, _ ->
            throw ParserConfigurationException("unsupported")
        }

        assertFalse(applied)
    }

    @Test
    fun hardenRejectsDoctypeAndExternalEntities() {
        val dir = Files.createTempDirectory("oclax-syncthing-xxe").toFile()
        try {
            val config = dir.resolve("config.xml")
            config.writeText(
                """
                <?xml version="1.0"?>
                <!DOCTYPE configuration [
                    <!ENTITY external SYSTEM "file:///etc/passwd">
                ]>
                <configuration>
                    <gui><address>&external;</address><apikey>old</apikey></gui>
                    <options></options>
                </configuration>
                """.trimIndent(),
            )

            val result = runCatching {
                SyncthingPrivateConfig.harden(
                    config,
                    "0123456789abcdefghijklmnopqrstuvwxyzABCDEFG",
                )
            }
            assertTrue(result.isFailure)
        } finally {
            dir.deleteRecursively()
        }
    }
}
