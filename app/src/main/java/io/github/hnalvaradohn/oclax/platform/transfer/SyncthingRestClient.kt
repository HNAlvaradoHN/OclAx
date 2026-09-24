package io.github.hnalvaradohn.oclax.platform.transfer

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URL
import java.net.URLEncoder

internal data class TransferRuntimeProbeResult(
    val deviceId: String,
    val guiAddress: String,
    val nonLoopbackAddressesChecked: Int,
)

internal data class LanPeerConnectionResult(
    val deviceId: String,
    val address: String,
    val connectionType: String,
)

internal fun describeLanTimeout(
    discoveredLocally: Boolean,
    peerPaused: Boolean?,
): String = when {
    peerPaused == true ->
        "El dispositivo quedó pausado en el motor local. Volvé a intentar la prueba LAN."

    discoveredLocally ->
        "El otro teléfono apareció en discovery local, pero no se completó la conexión. " +
            "Confirmá que ambos tengan agregado el ID del otro y que ambos hayan tocado Probar LAN."

    else ->
        "El otro teléfono no apareció en discovery local. " +
            "Abrí OclAx en ambos teléfonos y tocá Probar LAN en los dos dentro de la misma ventana. " +
            "Si ambos lo hacen y sigue igual, la Wi-Fi puede estar aislando dispositivos o multicast."
}

internal class SyncthingRestClient(
    private val config: SyncthingRuntimeConfig,
) {
    fun awaitReady(timeoutMillis: Long = 20_000L) {
        val deadline = System.currentTimeMillis() + timeoutMillis
        var lastError: Exception? = null

        while (System.currentTimeMillis() < deadline) {
            try {
                val health = getJson("/rest/noauth/health", authenticated = false)
                if (health.optString("status") == "OK") return
            } catch (error: Exception) {
                lastError = error
            }
            Thread.sleep(250L)
        }

        throw IOException(
            "El motor no respondió a tiempo.",
            lastError,
        )
    }

    fun enforcePrivateOptions() {
        val options = getJson("/rest/config/options")
        options.put("listenAddresses", JSONArray().put(SyncthingPrivateConfig.SAFE_LISTEN_ADDRESS))
        options.put("globalAnnounceEnabled", false)
        options.put("localAnnounceEnabled", false)
        options.put("relaysEnabled", false)
        options.put("natEnabled", false)
        options.put("startBrowser", false)
        options.put("urAccepted", -1)
        options.put("crashReportingEnabled", false)
        putJson("/rest/config/options", options)

        val verified = getJson("/rest/config/options")
        val listenAddresses = verified.optJSONArray("listenAddresses")
            ?: error("El motor no devolvió sus direcciones de escucha.")
        check(
            listenAddresses.length() == 1 &&
                listenAddresses.optString(0) == SyncthingPrivateConfig.SAFE_LISTEN_ADDRESS
        ) {
            "El motor no confirmó el modo de red aislado."
        }
        check(!verified.optBoolean("globalAnnounceEnabled", true)) {
            "El motor no confirmó que discovery global esté desactivado."
        }
        check(!verified.optBoolean("localAnnounceEnabled", true)) {
            "El motor no confirmó que discovery local esté desactivado."
        }
        check(!verified.optBoolean("relaysEnabled", true)) {
            "El motor no confirmó que relay esté desactivado durante la prueba local."
        }
        check(!verified.optBoolean("natEnabled", true)) {
            "El motor no confirmó que NAT traversal esté desactivado durante la prueba local."
        }
        check(!verified.optBoolean("startBrowser", true)) {
            "El motor no confirmó que el navegador automático esté desactivado."
        }
        check(verified.optInt("urAccepted", 0) == -1) {
            "El motor no confirmó que la telemetría esté desactivada."
        }
        check(!verified.optBoolean("crashReportingEnabled", true)) {
            "El motor no confirmó que los reportes de fallos estén desactivados."
        }
    }

    fun probe(): TransferRuntimeProbeResult {
        val gui = getJson("/rest/config/gui").optString("address")
        check(gui == "${SyncthingRuntimeConfig.LOOPBACK_ADDRESS}:${SyncthingRuntimeConfig.GUI_PORT}") {
            "La API local no está limitada a loopback."
        }

        val system = getJson("/rest/system/status")
        val deviceId = system.optString("myID")
        check(deviceId.isNotBlank()) {
            "El motor no devolvió un identificador de dispositivo."
        }

        val unauthenticatedCode = unauthenticatedSystemStatusCode()
        check(unauthenticatedCode == HttpURLConnection.HTTP_UNAUTHORIZED || unauthenticatedCode == HttpURLConnection.HTTP_FORBIDDEN) {
            "La API local no exige autenticación."
        }

        val candidates = nonLoopbackIpv4Addresses()
        candidates.forEach { address ->
            check(!sameRuntimeRespondsAt(address, deviceId)) {
                "La API local respondió fuera de loopback."
            }
        }

        return TransferRuntimeProbeResult(
            deviceId = deviceId,
            guiAddress = gui,
            nonLoopbackAddressesChecked = candidates.size,
        )
    }

    fun canonicalDeviceId(rawDeviceId: String): String {
        val encoded = URLEncoder.encode(rawDeviceId, Charsets.UTF_8.name())
        val response = getJson("/rest/svc/deviceid?id=$encoded")
        return response.optString("id")
            .takeIf { it.isNotBlank() }
            ?: error("El motor rechazó el ID del dispositivo.")
    }

    fun configureLanPeer(
        rawDeviceId: String,
        name: String,
    ): String {
        val deviceId = canonicalDeviceId(rawDeviceId)
        val template = getJson("/rest/config/defaults/device")
        val device = SyncthingLanPolicy.applyToDevice(
            device = template,
            deviceId = deviceId,
            name = name,
        )
        postJson("/rest/config/devices", device)
        checkNoRestartRequired()
        return deviceId
    }

    fun enableLanOnlyOptions() {
        val options = getJson("/rest/config/options")
        putJson(
            "/rest/config/options",
            SyncthingLanPolicy.applyToOptions(options),
        )
        SyncthingLanPolicy.verifyDiscoveryOptions(
            getJson("/rest/config/options"),
        )
        checkNoRestartRequired()
    }

    fun stopLocalDiscoveryKeepLan() {
        val options = getJson("/rest/config/options")
        putJson(
            "/rest/config/options",
            SyncthingLanPolicy.applyConnectedOptions(options),
        )
        SyncthingLanPolicy.verifyConnectedOptions(
            getJson("/rest/config/options"),
        )
        checkNoRestartRequired()
    }

    fun resumeDevice(deviceId: String) {
        postEmpty(
            "/rest/system/resume?device=" +
                URLEncoder.encode(deviceId, Charsets.UTF_8.name()),
        )
    }

    fun pauseDevice(deviceId: String) {
        postEmpty(
            "/rest/system/pause?device=" +
                URLEncoder.encode(deviceId, Charsets.UTF_8.name()),
        )
    }

    fun awaitLanConnection(
        deviceId: String,
        timeoutMillis: Long = 45_000L,
    ): LanPeerConnectionResult {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            val connection = getJson("/rest/system/connections")
                .optJSONObject("connections")
                ?.optJSONObject(deviceId)

            if (
                connection != null &&
                connection.optBoolean("connected", false) &&
                connection.optBoolean("isLocal", false)
            ) {
                return LanPeerConnectionResult(
                    deviceId = deviceId,
                    address = connection.optString("address"),
                    connectionType = connection.optString("type"),
                )
            }
            Thread.sleep(500L)
        }

        val connection = runCatching {
            getJson("/rest/system/connections")
                .optJSONObject("connections")
                ?.optJSONObject(deviceId)
        }.getOrNull()
        val discoveredLocally = runCatching {
            val discovery = getJson("/rest/system/discovery")
            val addresses = discovery.optJSONArray(deviceId)
            addresses != null && addresses.length() > 0
        }.getOrDefault(false)
        val peerPaused = connection
            ?.takeIf { it.has("paused") }
            ?.optBoolean("paused")

        throw IOException(
            describeLanTimeout(
                discoveredLocally = discoveredLocally,
                peerPaused = peerPaused,
            ),
        )
    }

    private fun checkNoRestartRequired() {
        val status = getJson("/rest/config/restart-required")
        check(!status.optBoolean("requiresRestart", false)) {
            "El motor requiere reinicio para aplicar la red local."
        }
    }

    fun shutdown() {
        request(
            method = "POST",
            path = "/rest/system/shutdown",
            authenticated = true,
            body = ByteArray(0),
        )
    }

    fun awaitStopped(timeoutMillis: Long = 7_000L): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            if (!isHealthy()) return true
            Thread.sleep(200L)
        }
        return !isHealthy()
    }

    private fun isHealthy(): Boolean = runCatching {
        getJson("/rest/noauth/health", authenticated = false).optString("status") == "OK"
    }.getOrDefault(false)

    private fun getJson(
        path: String,
        authenticated: Boolean = true,
    ): JSONObject = JSONObject(
        request(
            method = "GET",
            path = path,
            authenticated = authenticated,
        ).toString(Charsets.UTF_8),
    )

    private fun putJson(path: String, body: JSONObject) {
        request(
            method = "PUT",
            path = path,
            authenticated = true,
            body = body.toString().toByteArray(Charsets.UTF_8),
            contentType = "application/json; charset=utf-8",
        )
    }

    private fun postJson(path: String, body: JSONObject) {
        request(
            method = "POST",
            path = path,
            authenticated = true,
            body = body.toString().toByteArray(Charsets.UTF_8),
            contentType = "application/json; charset=utf-8",
        )
    }

    private fun postEmpty(path: String) {
        request(
            method = "POST",
            path = path,
            authenticated = true,
            body = ByteArray(0),
        )
    }

    private fun request(
        method: String,
        path: String,
        authenticated: Boolean,
        body: ByteArray? = null,
        contentType: String? = null,
    ): ByteArray {
        val connection = (URL(SyncthingRuntimeConfig.GUI_ENDPOINT + path).openConnection() as HttpURLConnection)
        return connection.useConnection {
            connectTimeout = 1_500
            readTimeout = 2_500
            requestMethod = method
            useCaches = false
            if (authenticated) {
                setRequestProperty("X-API-Key", config.apiKey())
            }
            if (contentType != null) {
                setRequestProperty("Content-Type", contentType)
            }
            if (body != null) {
                doOutput = true
                outputStream.use { it.write(body) }
            }

            val code = responseCode
            val stream = if (code in 200..299) inputStream else errorStream
            val bytes = stream?.use { it.readBytes() } ?: ByteArray(0)
            if (code !in 200..299) {
                throw IOException("La API local respondió HTTP $code.")
            }
            bytes
        }
    }

    private fun nonLoopbackIpv4Addresses(): List<String> {
        val result = linkedSetOf<String>()
        val interfaces = NetworkInterface.getNetworkInterfaces() ?: return emptyList()
        while (interfaces.hasMoreElements()) {
            val networkInterface = interfaces.nextElement()
            if (!runCatching { networkInterface.isUp }.getOrDefault(false)) continue
            if (runCatching { networkInterface.isLoopback }.getOrDefault(false)) continue

            val addresses = networkInterface.inetAddresses
            while (addresses.hasMoreElements()) {
                val address = addresses.nextElement()
                if (address !is Inet4Address) continue
                if (address.isLoopbackAddress || address.isAnyLocalAddress || address.isLinkLocalAddress) continue
                result += address.hostAddress ?: continue
            }
        }
        return result.toList()
    }

    private fun unauthenticatedSystemStatusCode(): Int {
        val connection = (
            URL(SyncthingRuntimeConfig.GUI_ENDPOINT + "/rest/system/status")
                .openConnection() as HttpURLConnection
            )
        return connection.useConnection {
            connectTimeout = 1_000
            readTimeout = 1_000
            requestMethod = "GET"
            responseCode
        }
    }

    private fun sameRuntimeRespondsAt(
        address: String,
        expectedDeviceId: String,
    ): Boolean = runCatching {
        val connection = (
            URL("http://$address:${SyncthingRuntimeConfig.GUI_PORT}/rest/system/status")
                .openConnection() as HttpURLConnection
            )
        connection.useConnection {
            connectTimeout = 500
            readTimeout = 500
            requestMethod = "GET"
            setRequestProperty("X-API-Key", config.apiKey())

            if (responseCode !in 200..299) {
                return@useConnection false
            }
            val body = inputStream.use { it.readBytes() }.toString(Charsets.UTF_8)
            JSONObject(body).optString("myID") == expectedDeviceId
        }
    }.getOrDefault(false)

    private inline fun <T> HttpURLConnection.useConnection(block: HttpURLConnection.() -> T): T =
        try {
            block()
        } finally {
            disconnect()
        }
}

internal class TransferRuntimeController(context: Context) {
    private val appContext = context.applicationContext
    private val config = SyncthingRuntimeConfig(appContext)
    private val client = SyncthingRestClient(config)
    private val startupDiagnostics = RuntimeStartupDiagnostics(appContext)

    fun start() {
        startupDiagnostics.beginAttempt(config.logFile.length())
        SyncthingRuntimeService.start(appContext)
    }

    fun stop() {
        SyncthingRuntimeService.stop(appContext)
    }

    fun probe(timeoutMillis: Long = 25_000L): TransferRuntimeProbeResult {
        try {
            client.awaitReady(timeoutMillis)
            client.enforcePrivateOptions()
            return client.probe()
        } catch (error: Exception) {
            val snapshot = startupDiagnostics.snapshot()
            val logLine = config.safeLastLogLineSince(snapshot.logOffset)
            throw IOException(
                formatRuntimeStartupFailure(snapshot, logLine),
                error,
            )
        }
    }

    fun connectLan(
        rawDeviceId: String,
        name: String,
        timeoutMillis: Long = 45_000L,
    ): LanPeerConnectionResult {
        start()
        client.awaitReady()
        client.enforcePrivateOptions()

        val deviceId = client.configureLanPeer(rawDeviceId, name)

        return try {
            SyncthingRuntimeService.enableLanDiscovery(appContext)
            client.enableLanOnlyOptions()
            client.resumeDevice(deviceId)
            val connection = client.awaitLanConnection(deviceId, timeoutMillis)
            client.stopLocalDiscoveryKeepLan()
            SyncthingRuntimeService.disableLanDiscovery(appContext)
            connection
        } catch (error: Exception) {
            runCatching { client.pauseDevice(deviceId) }
            val isolated = runCatching {
                client.enforcePrivateOptions()
            }.isSuccess
            runCatching {
                SyncthingRuntimeService.disableLanDiscovery(appContext)
            }
            if (!isolated) {
                runCatching { stop() }
            }
            throw error
        }
    }

    fun disconnectLan(deviceId: String) {
        var isolated = false
        try {
            runCatching { client.pauseDevice(deviceId) }
            client.enforcePrivateOptions()
            isolated = true
        } finally {
            runCatching {
                SyncthingRuntimeService.disableLanDiscovery(appContext)
            }
            if (!isolated) {
                runCatching { stop() }
            }
        }
    }

    fun awaitStopped(timeoutMillis: Long = 7_000L): Boolean =
        client.awaitStopped(timeoutMillis)
}
