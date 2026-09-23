package io.github.hnalvaradohn.oclax.platform

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable

data class InstalledAppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable,
)

class InstalledAppsRepository(context: Context) {
    private val appContext = context.applicationContext
    private val packageManager = appContext.packageManager

    fun listLaunchableApps(): List<InstalledAppInfo> {
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        return packageManager
            .queryIntentActivities(launcherIntent, 0)
            .mapNotNull { resolveInfo ->
                val packageName = resolveInfo.activityInfo?.packageName ?: return@mapNotNull null
                if (packageName == appContext.packageName) return@mapNotNull null

                runCatching {
                    InstalledAppInfo(
                        label = resolveInfo.loadLabel(packageManager)?.toString()
                            ?.takeIf { it.isNotBlank() }
                            ?: packageName,
                        packageName = packageName,
                        icon = resolveInfo.loadIcon(packageManager),
                    )
                }.getOrNull()
            }
            .distinctBy { it.packageName }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
    }
}
