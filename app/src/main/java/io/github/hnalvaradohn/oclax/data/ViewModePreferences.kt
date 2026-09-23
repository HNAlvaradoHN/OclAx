package io.github.hnalvaradohn.oclax.data

import android.content.Context

enum class ContentViewMode {
    LIST,
    GRID,
}

class ViewModePreferences(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences("oclax_view_modes", Context.MODE_PRIVATE)

    fun getDeviceMode(
        categoryKey: String,
        defaultMode: ContentViewMode,
    ): ContentViewMode {
        val raw = preferences.getString(deviceKey(categoryKey), null)
        return raw
            ?.let { value -> ContentViewMode.entries.firstOrNull { it.name == value } }
            ?: defaultMode
    }

    fun setDeviceMode(
        categoryKey: String,
        mode: ContentViewMode,
    ) {
        preferences.edit()
            .putString(deviceKey(categoryKey), mode.name)
            .apply()
    }

    private fun deviceKey(categoryKey: String): String =
        "device.$categoryKey"
}
