package io.github.hnalvaradohn.oclax.platform

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

data class ShareableInstalledApp(
    val label: String,
    val uris: ArrayList<Uri>,
    val apkCount: Int,
)

class InstalledAppExporter(context: Context) {
    private val appContext = context.applicationContext
    private val packageManager = appContext.packageManager
    private val exportRoot = File(appContext.cacheDir, "oclax/app_exports")

    @Suppress("DEPRECATION")
    fun prepare(app: InstalledAppInfo): ShareableInstalledApp {
        val applicationInfo = packageManager.getApplicationInfo(app.packageName, 0)
        val sourcePaths = buildList {
            add(applicationInfo.sourceDir)
            applicationInfo.splitSourceDirs?.let(::addAll)
        }
            .map(::File)
            .filter { it.isFile }

        require(sourcePaths.isNotEmpty()) {
            "No se encontraron APK legibles para ${app.label}."
        }

        // Este directorio contiene únicamente copias temporales del código APK instalado.
        // No se toca dataDir ni ningún directorio de datos del usuario.
        if (exportRoot.exists()) {
            exportRoot.deleteRecursively()
        }
        check(exportRoot.mkdirs() || exportRoot.isDirectory) {
            "No se pudo preparar el directorio temporal de exportación."
        }

        val appDir = File(exportRoot, safeSegment(app.packageName))
        check(appDir.mkdirs() || appDir.isDirectory) {
            "No se pudo preparar la exportación de la aplicación."
        }

        val copiedApks = sourcePaths.mapIndexed { index, source ->
            val targetName = if (sourcePaths.size == 1) {
                safeSegment(app.label).ifBlank { "app" } + ".apk"
            } else if (index == 0) {
                "base.apk"
            } else {
                source.name
                    .takeIf { it.endsWith(".apk", ignoreCase = true) }
                    ?.let(::safeFileName)
                    ?: "split-$index.apk"
            }
            File(appDir, targetName).also { target ->
                source.inputStream().buffered().use { input ->
                    target.outputStream().buffered().use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }

        val authority = appContext.packageName + ".fileprovider"
        val uris = ArrayList(
            copiedApks.map { FileProvider.getUriForFile(appContext, authority, it) },
        )

        return ShareableInstalledApp(
            label = app.label,
            uris = uris,
            apkCount = uris.size,
        )
    }

    private fun safeSegment(value: String): String =
        value
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .trim('.', '_')
            .take(80)

    private fun safeFileName(value: String): String =
        safeSegment(value).ifBlank { "split.apk" }
}
