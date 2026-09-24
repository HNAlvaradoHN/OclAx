package io.github.hnalvaradohn.oclax.platform.transfer

import android.content.Context
import android.util.Base64
import java.io.File
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

internal class SyncthingRuntimeConfig(
    private val context: Context,
) {
    companion object {
        const val LOOPBACK_ADDRESS = "127.0.0.1"
        const val GUI_PORT = 8384
        const val GUI_ENDPOINT = "http://$LOOPBACK_ADDRESS:$GUI_PORT"
        const val BINARY_NAME = "libsyncthingnative.so"

        private const val PREFS_NAME = "oclax_transfer_runtime"
        private const val PREF_API_KEY = "api_key"

        internal fun buildCommand(
            binaryPath: String,
            homePath: String,
            logPath: String,
        ): List<String> = listOf(
            binaryPath,
            "--home",
            homePath,
            "serve",
            "--no-browser",
            "--no-restart",
            "--no-upgrade",
            "--no-port-probing",
            "--paused",
            "--gui-address=$LOOPBACK_ADDRESS:$GUI_PORT",
            "--log-file=$logPath",
            "--log-max-old-files=1",
            "--log-max-size=1048576",
        )

        internal fun buildGenerateCommand(
            binaryPath: String,
            homePath: String,
        ): List<String> = listOf(
            binaryPath,
            "--home",
            homePath,
            "generate",
            "--no-port-probing",
        )

        internal fun privateEnvironmentOverrides(
            homePath: String,
            tempPath: String,
            apiKey: String,
        ): Map<String, String> = buildMap {
            put("HOME", homePath)
            put("TMPDIR", tempPath)
            put("SQLITE_TMPDIR", tempPath)
            put("STHOMEDIR", homePath)
            put("STGUIAPIKEY", apiKey)
            put("STGUIADDRESS", "$LOOPBACK_ADDRESS:$GUI_PORT")
            put("STNOBROWSER", "yes")
            put("STNORESTART", "yes")
            put("STNOUPGRADE", "yes")

            // Syncthing's outer monitor re-execs the binary. Android wrappers run the
            // core directly as the already-monitored child process instead.
            put("STMONITORED", "1")
        }
    }

    val homeDir: File
        get() = File(context.filesDir, "oclax/syncthing")

    val tempDir: File
        get() = File(context.cacheDir, "syncthing")

    val logFile: File
        get() = File(homeDir, "syncthing.log")

    val configFile: File
        get() = File(homeDir, "config.xml")

    val binaryFile: File
        get() = File(context.applicationInfo.nativeLibraryDir, BINARY_NAME)

    fun prepare() {
        check(homeDir.exists() || homeDir.mkdirs()) {
            "No se pudo preparar el directorio privado del motor."
        }
        check(tempDir.exists() || tempDir.mkdirs()) {
            "No se pudo preparar el directorio temporal del motor."
        }
        check(binaryFile.isFile && binaryFile.canExecute()) {
            "El motor de transferencia no está disponible para este dispositivo."
        }

        if (!configFile.isFile) {
            generateBaseConfig()
        }

        SyncthingPrivateConfig.harden(
            configFile = configFile,
            apiKey = apiKey(),
        )
    }

    fun apiKey(): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.getString(PREF_API_KEY, null)?.takeIf { it.length >= 32 }?.let { return it }

        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        val generated = Base64.encodeToString(
            bytes,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
        )
        check(prefs.edit().putString(PREF_API_KEY, generated).commit()) {
            "No se pudo guardar la credencial local del motor."
        }
        return generated
    }

    private fun generateBaseConfig() {
        val builder = ProcessBuilder(
            buildGenerateCommand(
                binaryPath = binaryFile.absolutePath,
                homePath = homeDir.absolutePath,
            ),
        )
            .directory(homeDir)
            .redirectErrorStream(true)
            .redirectOutput(ProcessBuilder.Redirect.appendTo(logFile))
        applyPrivateEnvironment(builder)

        val process = builder.start()
        val completed = runCatching {
            process.waitFor(20, TimeUnit.SECONDS)
        }.getOrDefault(false)

        if (!completed) {
            process.destroyForcibly()
            error("El motor tardó demasiado en generar su configuración.")
        }

        check(process.exitValue() == 0 && configFile.isFile) {
            "El motor no pudo generar su configuración privada."
        }
    }

    fun command(): List<String> = buildCommand(
        binaryPath = binaryFile.absolutePath,
        homePath = homeDir.absolutePath,
        logPath = logFile.absolutePath,
    )

    fun applyPrivateEnvironment(builder: ProcessBuilder) {
        builder.environment().putAll(
            privateEnvironmentOverrides(
                homePath = homeDir.absolutePath,
                tempPath = tempDir.absolutePath,
                apiKey = apiKey(),
            ),
        )
    }
}
