package io.github.hnalvaradohn.oclax.platform.transfer

import android.content.Context
import android.util.Base64
import java.io.File
import java.security.SecureRandom

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
            "--gui-address=$LOOPBACK_ADDRESS:$GUI_PORT",
            "--log-file=$logPath",
            "--log-max-old-files=1",
            "--log-max-size=1048576",
        )
    }

    val homeDir: File
        get() = File(context.filesDir, "oclax/syncthing")

    val tempDir: File
        get() = File(context.cacheDir, "syncthing")

    val logFile: File
        get() = File(homeDir, "syncthing.log")

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

    fun command(): List<String> = buildCommand(
        binaryPath = binaryFile.absolutePath,
        homePath = homeDir.absolutePath,
        logPath = logFile.absolutePath,
    )

    fun applyPrivateEnvironment(builder: ProcessBuilder) {
        val environment = builder.environment()
        environment["HOME"] = homeDir.absolutePath
        environment["TMPDIR"] = tempDir.absolutePath
        environment["STGUIAPIKEY"] = apiKey()
        environment["STGUIADDRESS"] = "$LOOPBACK_ADDRESS:$GUI_PORT"
        environment["STNOBROWSER"] = "yes"
        environment["STNORESTART"] = "yes"
        environment["STNOUPGRADE"] = "yes"
    }
}
