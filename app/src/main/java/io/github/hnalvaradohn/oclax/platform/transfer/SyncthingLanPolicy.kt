package io.github.hnalvaradohn.oclax.platform.transfer

import org.json.JSONArray
import org.json.JSONObject

internal object SyncthingLanPolicy {
    const val LISTEN_ADDRESS = "tcp4://0.0.0.0:22000"

    val ALLOWED_NETWORKS: List<String> = listOf(
        "10.0.0.0/8",
        "169.254.0.0/16",
        "172.16.0.0/12",
        "192.168.0.0/16",
    )

    fun applyToOptions(options: JSONObject): JSONObject = options.apply {
        put("listenAddresses", JSONArray().put(LISTEN_ADDRESS))
        put("globalAnnounceEnabled", false)
        put("localAnnounceEnabled", true)
        put("relaysEnabled", false)
        put("natEnabled", false)
        put("startBrowser", false)
        put("urAccepted", -1)
        put("crashReportingEnabled", false)
    }

    fun applyToDevice(
        device: JSONObject,
        deviceId: String,
        name: String,
    ): JSONObject = device.apply {
        put("deviceID", deviceId)
        put("name", name.take(80))
        put("addresses", JSONArray().put("dynamic"))
        put("paused", true)
        put("allowedNetworks", JSONArray(ALLOWED_NETWORKS))
        put("autoAcceptFolders", false)
        put("introducer", false)
        put("skipIntroductionRemovals", false)
        put("untrusted", false)
    }

    fun verifyOptions(options: JSONObject) {
        val listen = options.optJSONArray("listenAddresses")
            ?: error("El motor no devolvió sus direcciones de escucha.")
        check(listen.length() == 1 && listen.optString(0) == LISTEN_ADDRESS) {
            "El motor no confirmó el listener LAN limitado."
        }
        check(options.optBoolean("localAnnounceEnabled", false)) {
            "El motor no confirmó discovery local."
        }
        check(!options.optBoolean("globalAnnounceEnabled", true)) {
            "Discovery global no debe estar activo en la prueba LAN."
        }
        check(!options.optBoolean("relaysEnabled", true)) {
            "Relay no debe estar activo en la prueba LAN."
        }
        check(!options.optBoolean("natEnabled", true)) {
            "NAT traversal no debe estar activo en la prueba LAN."
        }
        check(options.optInt("urAccepted", 0) == -1) {
            "El motor no confirmó telemetría desactivada."
        }
        check(!options.optBoolean("crashReportingEnabled", true)) {
            "El motor no confirmó reportes de fallos desactivados."
        }
    }
}
