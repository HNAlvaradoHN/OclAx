package io.github.hnalvaradohn.oclax.platform

import android.content.Context
import android.net.Uri
import android.os.StatFs
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException

data class ShareableInstalledApp(
    val label: String,
    val uris: ArrayList<Uri>,
    val apkCount: Int,
)

class InstalledAppExporter(context: Context) {
    companion object {
        private const val EXPORT_TTL_MILLIS = 24L * 60L * 60L * 1000L
        private const val MAX_EXPORT_BYTES = 4L * 1024L * 1024L * 1024L
        private const val RESERVED_FREE_BYTES = 64L * 1024L * 1024L
    }
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

        val totalBytes = sourcePaths.sumOf { source -> source.length().coerceAtLeast(0L) }
        if (totalBytes <= 0L || totalBytes > MAX_EXPORT_BYTES) {
            throw IOException("La aplicación supera el límite seguro de exportación de OclAx.")
        }
        val availableBytes = StatFs(appContext.cacheDir.absolutePath).availableBytes
        if (availableBytes - RESERVED_FREE_BYTES < totalBytes) {
            throw IOException("No hay espacio libre suficiente para preparar la aplicación.")
        }

        // Este directorio contiene únicamente copias temporales del código APK instalado.
        // No se toca dataDir ni ningún directorio de datos del usuario.
        check(exportRoot.mkdirs() || exportRoot.isDirectory) {
            "No se pudo preparar el directorio temporal de exportación."
        }
        cleanupOldExports()

        val appDir = File(
            exportRoot,
            safeSegment(app.packageName) + "-" + System.currentTimeMillis(),
        )
        check(appDir.mkdirs()) {
            "No se pudo preparar la exportación de la aplicación."
        }

        val copiedApks = try {
            sourcePaths.mapIndexed { index, source ->
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
        } catch (error: Exception) {
            appDir.deleteRecursively()
            throw error
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

    private fun cleanupOldExports() {
        val cutoff = System.currentTimeMillis() - EXPORT_TTL_MILLIS
        exportRoot.listFiles()
            .orEmpty()
            .filter { it.lastModified() in 1 until cutoff }
            .forEach { it.deleteRecursively() }
    }

    private fun safeSegment(value: String): String =
        value
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .trim('.', '_')
            .take(80)

    private fun safeFileName(value: String): String =
        safeSegment(value).ifBlank { "split.apk" }
}
