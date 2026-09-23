package io.github.hnalvaradohn.oclax.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class PairedDevice(
    val deviceId: String,
    val name: String,
    val allowWithoutAccept: Boolean,
)

class PairedDeviceStore(context: Context) {
    companion object {
        private const val PREFS_NAME = "oclax_paired_devices"
        private const val KEY_DEVICES = "devices_json"

        fun normalizeDeviceId(raw: String): String? {
            val cleaned = raw.trim().uppercase().filterNot { it.isWhitespace() }
            if (cleaned.any { it != '-' && it !in 'A'..'Z' && it !in '2'..'7' }) {
                return null
            }

            val compact = cleaned.replace("-", "")
            if (compact.length != 56 || compact.any { it !in 'A'..'Z' && it !in '2'..'7' }) {
                return null
            }

            return compact.chunked(7).joinToString("-")
        }
    }

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun list(): List<PairedDevice> =
        readAll().sortedBy { it.name.lowercase() }

    @Synchronized
    fun add(name: String, rawDeviceId: String): Result<PairedDevice> {
        val cleanName = name.trim()
        if (cleanName.isBlank()) {
            return Result.failure(IllegalArgumentException("Escribí un nombre para el dispositivo."))
        }
        if (cleanName.length > 80) {
            return Result.failure(IllegalArgumentException("El nombre del dispositivo es demasiado largo."))
        }

        val deviceId = normalizeDeviceId(rawDeviceId)
            ?: return Result.failure(IllegalArgumentException("El ID del dispositivo no tiene un formato válido."))

        val current = readAll().toMutableList()
        if (current.any { it.deviceId == deviceId }) {
            return Result.failure(IllegalArgumentException("Ese dispositivo ya está agregado."))
        }

        val device = PairedDevice(
            deviceId = deviceId,
            name = cleanName,
            allowWithoutAccept = false,
        )
        current += device
        writeAll(current)
        return Result.success(device)
    }

    @Synchronized
    fun setAllowWithoutAccept(deviceId: String, allowed: Boolean): Boolean {
        val normalized = normalizeDeviceId(deviceId) ?: return false
        val current = readAll().toMutableList()
        val index = current.indexOfFirst { it.deviceId == normalized }
        if (index < 0) return false

        current[index] = current[index].copy(allowWithoutAccept = allowed)
        writeAll(current)
        return true
    }

    @Synchronized
    fun remove(deviceId: String): Boolean {
        val normalized = normalizeDeviceId(deviceId) ?: return false
        val current = readAll()
        val updated = current.filterNot { it.deviceId == normalized }
        if (updated.size == current.size) return false

        writeAll(updated)
        return true
    }

    private fun readAll(): List<PairedDevice> {
        val raw = prefs.getString(KEY_DEVICES, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val deviceId = normalizeDeviceId(item.optString("deviceId")) ?: continue
                    val name = item.optString("name").trim()
                    if (name.isBlank()) continue

                    add(
                        PairedDevice(
                            deviceId = deviceId,
                            name = name.take(80),
                            allowWithoutAccept = item.optBoolean("allowWithoutAccept", false),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun writeAll(devices: List<PairedDevice>) {
        val array = JSONArray()
        devices.forEach { device ->
            array.put(
                JSONObject()
                    .put("deviceId", device.deviceId)
                    .put("name", device.name)
                    .put("allowWithoutAccept", device.allowWithoutAccept),
            )
        }

        check(prefs.edit().putString(KEY_DEVICES, array.toString()).commit()) {
            "No se pudo guardar la lista de dispositivos."
        }
    }
}
