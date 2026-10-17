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

data class InstalledAppDocumentInfo(
    val label: String,
    val packageName: String,
    val isSystemApp: Boolean,
    val sourceApkPaths: List<String>,
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
                        label = labelFor(application),
                        packageName = application.packageName,
                        icon = packageManager.getApplicationIcon(application),
                        isSystemApp = isSystemApp(application),
                    )
                }.getOrNull()
            }
            .distinctBy { it.packageName }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
            .toList()

    @Suppress("DEPRECATION")
    fun listInstalledAppDocuments(): List<InstalledAppDocumentInfo> =
        packageManager
            .getInstalledApplications(0)
            .asSequence()
            .filter { it.packageName != appContext.packageName }
            .mapNotNull(::toDocumentInfo)
            .distinctBy { it.packageName }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
            .toList()

    @Suppress("DEPRECATION")
    fun findInstalledAppDocument(packageName: String): InstalledAppDocumentInfo? =
        runCatching { packageManager.getApplicationInfo(packageName, 0) }
            .getOrNull()
            ?.takeIf { it.packageName != appContext.packageName }
            ?.let(::toDocumentInfo)

    fun loadInstalledAppIcon(packageName: String): Drawable? =
        packageName
            .takeIf { it.isNotBlank() && it != appContext.packageName }
            ?.let { safePackage ->
                runCatching { packageManager.getApplicationIcon(safePackage) }.getOrNull()
            }

    private fun toDocumentInfo(application: ApplicationInfo): InstalledAppDocumentInfo? =
        runCatching {
            val sourceApkPaths = buildList {
                application.sourceDir
                    ?.takeIf { it.isNotBlank() }
                    ?.let(::add)
                application.splitSourceDirs
                    ?.asSequence()
                    ?.filterNotNull()
                    ?.filter { it.isNotBlank() }
                    ?.forEach(::add)
            }
                .distinct()

            if (sourceApkPaths.isEmpty()) return@runCatching null

            InstalledAppDocumentInfo(
                label = labelFor(application),
                packageName = application.packageName,
                isSystemApp = isSystemApp(application),
                sourceApkPaths = sourceApkPaths,
            )
        }.getOrNull()

    private fun labelFor(application: ApplicationInfo): String =
        packageManager.getApplicationLabel(application)
            .toString()
            .takeIf { it.isNotBlank() }
            ?: application.packageName

    private fun isSystemApp(application: ApplicationInfo): Boolean =
        application.flags and ApplicationInfo.FLAG_SYSTEM != 0
}
