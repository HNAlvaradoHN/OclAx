package io.github.hnalvaradohn.oclax.platform

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable

data class InstalledAppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable,
    val isSystemApp: Boolean,
)

class InstalledAppsRepository(context: Context) {
    private val appContext = context.applicationContext
    private val packageManager = appContext.packageManager

    @Suppress("DEPRECATION")
    fun listInstalledApps(): List<InstalledAppInfo> =
        packageManager
            .getInstalledApplications(0)
            .asSequence()
            .filter { it.packageName != appContext.packageName }
            .mapNotNull { application ->
                runCatching {
                    InstalledAppInfo(
                        label = packageManager.getApplicationLabel(application)
                            .toString()
                            .takeIf { it.isNotBlank() }
                            ?: application.packageName,
                        packageName = application.packageName,
                        icon = packageManager.getApplicationIcon(application),
                        isSystemApp = application.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                    )
                }.getOrNull()
            }
            .distinctBy { it.packageName }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
            .toList()
}
