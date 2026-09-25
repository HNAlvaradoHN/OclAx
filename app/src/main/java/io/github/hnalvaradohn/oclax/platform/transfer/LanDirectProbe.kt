package io.github.hnalvaradohn.oclax.platform.transfer

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

internal data class LanDirectProbeResult(
    val scannedHostCount: Int,
    val openAddresses: List<String>,
)

private data class LanProbeTarget(
    val network: Network,
    val targetAddress: String,
)

internal fun isLanPrivateIpv4(address: String): Boolean {
    val value = parseIpv4(address) ?: return false
    val first = ((value shr 24) and 0xff).toInt()
    val second = ((value shr 16) and 0xff).toInt()

    return when {
        first == 10 -> true
        first == 169 && second == 254 -> true
        first == 172 && second in 16..31 -> true
        first == 192 && second == 168 -> true
        else -> false
    }
}

internal fun lanSubnetProbeCandidates(
    localAddress: String,
    prefixLength: Int,
    maxHosts: Int = 254,
): List<String> {
    val local = parseIpv4(localAddress) ?: return emptyList()
    if (!isLanPrivateIpv4(localAddress)) return emptyList()
    if (prefixLength !in 0..30 || maxHosts <= 0) return emptyList()

    // Never sweep more broadly than the current /24. This keeps the fallback
    // bounded to the immediate LAN segment instead of becoming a general
    // network scanner on large private networks.
    val effectivePrefix = prefixLength.coerceAtLeast(24)
    val hostBits = 32 - effectivePrefix
    val hostMask = (1L shl hostBits) - 1L
    val networkMask = 0xffff_ffffL xor hostMask
    val network = local and networkMask
    val broadcast = network or hostMask

    val result = ArrayList<String>(minOf(maxHosts, 254))
    var candidate = network + 1L
    while (candidate < broadcast && result.size < maxHosts) {
        if (candidate != local) {
            result += formatIpv4(candidate)
        }
        candidate += 1L
    }
    return result
}

internal class LanDirectProbe(context: Context) {
    private val connectivityManager =
        context.applicationContext.getSystemService(ConnectivityManager::class.java)

    fun scan(
        port: Int = SyncthingLanPolicy.SYNC_PORT,
    ): LanDirectProbeResult {
        val targets = lanTargets()
            .distinctBy { it.targetAddress }
            .take(MAX_SCAN_HOSTS)

        if (targets.isEmpty()) {
            return LanDirectProbeResult(
                scannedHostCount = 0,
                openAddresses = emptyList(),
            )
        }

        val executor = Executors.newFixedThreadPool(
            minOf(MAX_PARALLEL_PROBES, targets.size),
        )
        return try {
            val tasks = targets.map { target ->
                Callable {
                    if (canConnect(target, port)) {
                        target.targetAddress
                    } else {
                        null
                    }
                }
            }
            val futures = executor.invokeAll(
                tasks,
                MAX_SCAN_SECONDS,
                TimeUnit.SECONDS,
            )
            val open = futures
                .asSequence()
                .filterNot { it.isCancelled }
                .mapNotNull { future -> runCatching { future.get() }.getOrNull() }
                .distinct()
                .take(MAX_OPEN_HOSTS)
                .toList()

            LanDirectProbeResult(
                scannedHostCount = targets.size,
                openAddresses = open,
            )
        } finally {
            executor.shutdownNow()
        }
    }

    private fun lanTargets(): List<LanProbeTarget> {
        val manager = connectivityManager ?: return emptyList()
        val result = ArrayList<LanProbeTarget>()

        manager.allNetworks.forEach { network ->
            if (result.size >= MAX_SCAN_HOSTS) return@forEach

            val capabilities = manager.getNetworkCapabilities(network) ?: return@forEach
            val localTransport =
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            if (!localTransport) return@forEach

            val linkProperties = manager.getLinkProperties(network) ?: return@forEach
            for (linkAddress in linkProperties.linkAddresses) {
                val address = linkAddress.address as? Inet4Address ?: continue
                val local = address.hostAddress ?: continue
                if (!isLanPrivateIpv4(local)) continue

                val remaining = MAX_SCAN_HOSTS - result.size
                lanSubnetProbeCandidates(
                    localAddress = local,
                    prefixLength = linkAddress.prefixLength,
                    maxHosts = remaining,
                ).forEach { target ->
                    result += LanProbeTarget(
                        network = network,
                        targetAddress = target,
                    )
                }
                if (result.size >= MAX_SCAN_HOSTS) break
            }
        }

        return result
    }

    private fun canConnect(
        target: LanProbeTarget,
        port: Int,
    ): Boolean = runCatching {
        Socket().use { socket ->
            target.network.bindSocket(socket)
            socket.connect(
                InetSocketAddress(target.targetAddress, port),
                CONNECT_TIMEOUT_MILLIS,
            )
            true
        }
    }.getOrDefault(false)

    companion object {
        private const val MAX_SCAN_HOSTS = 254
        private const val MAX_OPEN_HOSTS = 8
        private const val MAX_PARALLEL_PROBES = 24
        private const val CONNECT_TIMEOUT_MILLIS = 250
        private const val MAX_SCAN_SECONDS = 6L
    }
}

private fun parseIpv4(address: String): Long? {
    val parts = address.split('.')
    if (parts.size != 4) return null

    var value = 0L
    for (part in parts) {
        if (part.isEmpty() || part.length > 3) return null
        val octet = part.toIntOrNull() ?: return null
        if (octet !in 0..255) return null
        value = (value shl 8) or octet.toLong()
    }
    return value and 0xffff_ffffL
}

private fun formatIpv4(value: Long): String =
    listOf(
        (value shr 24) and 0xff,
        (value shr 16) and 0xff,
        (value shr 8) and 0xff,
        value and 0xff,
    ).joinToString(".")
