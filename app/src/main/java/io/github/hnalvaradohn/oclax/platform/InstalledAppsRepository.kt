package io.github.hnalvaradohn.oclax.platform

import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
import android.os.Process

data class InstalledAppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable,
)

class InstalledAppsRepository(context: Context) {
    private val appContext = context.applicationContext
    private val launcherApps = appContext.getSystemService(LauncherApps::class.java)

    fun listLaunchableApps(): List<InstalledAppInfo> =
        launcherApps
            .getActivityList(null, Process.myUserHandle())
            .mapNotNull { activity ->
                val packageName = activity.componentName.packageName
                if (packageName == appContext.packageName) return@mapNotNull null

                runCatching {
                    InstalledAppInfo(
                        label = activity.label?.toString()
                            ?.takeIf { it.isNotBlank() }
                            ?: packageName,
                        packageName = packageName,
                        icon = activity.getIcon(0),
                    )
                }.getOrNull()
            }
            .distinctBy { it.packageName }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
}
