package io.github.hnalvaradohn.oclax.platform.transfer

import android.content.Context
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URL

internal data class TransferRuntimeProbeResult(
    val deviceId: String,
    val guiAddress: String,
    val nonLoopbackAddressesChecked: Int,
)

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
        options.put("urAccepted", -1)
        options.put("crashReportingEnabled", false)
        putJson("/rest/config/options", options)

        val verified = getJson("/rest/config/options")
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

        val candidates = nonLoopbackIpv4Addresses()
        candidates.forEach { address ->
            check(!healthRespondsAt(address)) {
                "La API local respondió fuera de loopback."
            }
        }

        return TransferRuntimeProbeResult(
            deviceId = deviceId,
            guiAddress = gui,
            nonLoopbackAddressesChecked = candidates.size,
        )
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

    private fun healthRespondsAt(address: String): Boolean = runCatching {
        val connection = (
            URL("http://$address:${SyncthingRuntimeConfig.GUI_PORT}/rest/noauth/health")
                .openConnection() as HttpURLConnection
            )
        connection.useConnection {
            connectTimeout = 400
            readTimeout = 400
            requestMethod = "GET"
            responseCode in 200..299
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

    fun start() {
        SyncthingRuntimeService.start(appContext)
    }

    fun stop() {
        SyncthingRuntimeService.stop(appContext)
    }

    fun probe(timeoutMillis: Long = 25_000L): TransferRuntimeProbeResult {
        client.awaitReady(timeoutMillis)
        client.enforcePrivateOptions()
        return client.probe()
    }

    fun awaitStopped(timeoutMillis: Long = 7_000L): Boolean =
        client.awaitStopped(timeoutMillis)
}
