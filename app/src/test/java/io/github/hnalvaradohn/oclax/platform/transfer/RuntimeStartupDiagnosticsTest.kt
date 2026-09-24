package io.github.hnalvaradohn.oclax.platform.transfer

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeStartupDiagnosticsTest {
    @Test
    fun processStartedFailureExplainsThatRestNeverAnswered() {
        val message = formatRuntimeStartupFailure(
            RuntimeStartupSnapshot(
                stage = RuntimeStartupStage.PROCESS_STARTED,
                failureStage = RuntimeStartupStage.PROCESS_STARTED,
                failureMessage = "El motor no respondió a tiempo.",
                exitCode = 1,
                logOffset = 0L,
            ),
            safeLogLine = "ERROR: failed to start",
        )

        assertTrue(message.contains("proceso nativo arrancó"))
        assertTrue(message.contains("código de salida 1"))
        assertTrue(message.contains("último registro"))
    }

    @Test
    fun sanitizerRemovesPrivatePathsIdsIpsAndLongValues() {
        val raw = "/data/user/0/io.github.hnalvaradohn.oclax/files " +
            "AAAAAAA-BBBBBBB-CCCCCCC-DDDDDDD-EEEEEEE-FFFFFFF-GGGGGGG-HHHHHHH " +
            "192.168.1.10 abcdefghijklmnopqrstuvwxyz0123456789"

        val sanitized = sanitizeRuntimeDiagnosticText(raw).orEmpty()

        assertFalse(sanitized.contains("/data/user/0"))
        assertFalse(sanitized.contains("192.168.1.10"))
        assertFalse(sanitized.contains("AAAAAAA-BBBBBBB"))
        assertFalse(sanitized.contains("abcdefghijklmnopqrstuvwxyz0123456789"))
        assertTrue(sanitized.contains("<ruta privada>"))
    }
}
