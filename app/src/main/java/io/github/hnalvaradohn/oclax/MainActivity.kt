package io.github.hnalvaradohn.oclax

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image as ImageIcon
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import io.github.hnalvaradohn.oclax.data.ContentViewMode
import io.github.hnalvaradohn.oclax.data.ItemStore
import io.github.hnalvaradohn.oclax.data.PairedDevice
import io.github.hnalvaradohn.oclax.data.PairedDeviceStore
import io.github.hnalvaradohn.oclax.data.ViewModePreferences
import io.github.hnalvaradohn.oclax.model.ContentType
import io.github.hnalvaradohn.oclax.model.StoredItem
import io.github.hnalvaradohn.oclax.model.contentTypeFor
import io.github.hnalvaradohn.oclax.model.supportsClipboardCopy
import io.github.hnalvaradohn.oclax.platform.ContentOpener
import io.github.hnalvaradohn.oclax.platform.DeviceContentRepository
import io.github.hnalvaradohn.oclax.platform.DeviceDeleteResult
import io.github.hnalvaradohn.oclax.platform.DeviceFileInfo
import io.github.hnalvaradohn.oclax.platform.InstalledAppExporter
import io.github.hnalvaradohn.oclax.platform.InstalledAppInfo
import io.github.hnalvaradohn.oclax.platform.InstalledAppsRepository
import io.github.hnalvaradohn.oclax.platform.ThumbnailLoader
import io.github.hnalvaradohn.oclax.platform.transfer.TransferRuntimeController
import io.github.hnalvaradohn.oclax.ui.CategoryOverviewGrid
import io.github.hnalvaradohn.oclax.ui.CategoryOverviewItem
import io.github.hnalvaradohn.oclax.ui.theme.OclAxTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    companion object {
        private const val MAX_CLIPBOARD_TEXT_BYTES = 2L * 1024L * 1024L
    }

    private val store by lazy { ItemStore(applicationContext) }
    private val pairedDeviceStore by lazy { PairedDeviceStore(applicationContext) }
    private val installedAppsRepository by lazy { InstalledAppsRepository(applicationContext) }
    private val installedAppExporter by lazy { InstalledAppExporter(applicationContext) }
    private val deviceContentRepository by lazy { DeviceContentRepository(applicationContext) }
    private val thumbnailLoader by lazy { ThumbnailLoader(applicationContext) }
    private val viewModePreferences by lazy { ViewModePreferences(applicationContext) }
    private val contentOpener by lazy { ContentOpener(this) }
    private val transferRuntimeController by lazy { TransferRuntimeController(applicationContext) }
    private val deviceExecutor = Executors.newSingleThreadExecutor()
    private val transferExecutor = Executors.newSingleThreadExecutor()
    private var items by mutableStateOf<List<StoredItem>>(emptyList())
    private var installedApps by mutableStateOf<List<InstalledAppInfo>>(emptyList())
    private var deviceFiles by mutableStateOf<List<DeviceFileInfo>>(emptyList())
    private var hasBroadFileAccess by mutableStateOf(false)
    private var deviceLoading by mutableStateOf(false)
    private var retentionHours by mutableIntStateOf(24)
    private var pairedDevices by mutableStateOf<List<PairedDevice>>(emptyList())
    private var transferDeviceId by mutableStateOf<String?>(null)
    private var transferRuntimeStatus by mutableStateOf("Motor de envío sin probar.")
    private var transferRuntimeBusy by mutableStateOf(false)
    private var lanBusyDeviceId by mutableStateOf<String?>(null)
    private var activeLanDeviceId by mutableStateOf<String?>(null)
    private var lanStatusByDevice by mutableStateOf<Map<String, String>>(emptyMap())
    private var pendingSystemDeleteName: String? = null

    private val deviceDeleteLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val name = pendingSystemDeleteName
        pendingSystemDeleteName = null

        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(
                this,
                if (name.isNullOrBlank()) "Archivo eliminado." else "Se eliminó “$name”.",
                Toast.LENGTH_SHORT,
            ).show()
            refresh()
        } else {
            Toast.makeText(this, "No se eliminó el archivo.", Toast.LENGTH_SHORT).show()
            refresh()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OclAxTheme {
                OclAxHome(
                    allItems = items,
                    installedApps = installedApps,
                    deviceFiles = deviceFiles,
                    hasBroadFileAccess = hasBroadFileAccess,
                    deviceLoading = deviceLoading,
                    retentionHours = retentionHours,
                    onRetentionChange = { hours ->
                        store.setRetentionHours(hours)
                        refresh()
                    },
                    onPinToggle = { item ->
                        store.setPinned(item.id, !item.pinned)
                        refresh()
                    },
                    onDelete = { item ->
                        if (!store.deleteItem(item.id)) {
                            Toast.makeText(
                                this,
                                "No se pudo eliminar la copia de OclAx.",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                        refresh()
                    },
                    onOpen = ::openItem,
                    onShare = ::shareItem,
                    onCopy = ::copyItem,
                    onLoadItemThumbnail = { item, targetPx ->
                        runCatching {
                            thumbnailLoader.loadFile(
                                store.payloadFile(item),
                                contentTypeFor(item.mimeType),
                                targetPx,
                            )
                        }.getOrNull()
                    },
                    onRequestBroadAccess = ::requestBroadFileAccess,
                    onOpenDeviceFile = ::openDeviceFile,
                    onShareDeviceFile = ::shareDeviceFile,
                    onCopyDeviceFile = ::copyDeviceFile,
                    onDeleteDeviceFile = ::deleteDeviceFile,
                    onOpenApp = ::openInstalledApp,
                    onShareApp = ::shareInstalledApp,
                    onLoadDeviceThumbnail = thumbnailLoader::loadDevice,
                    onLoadDeviceViewMode = viewModePreferences::getDeviceMode,
                    onSaveDeviceViewMode = viewModePreferences::setDeviceMode,
                    transferRuntimeStatus = transferRuntimeStatus,
                    transferRuntimeBusy = transferRuntimeBusy,
                    transferDeviceId = transferDeviceId,
                    pairedDevices = pairedDevices,
                    lanBusyDeviceId = lanBusyDeviceId,
                    activeLanDeviceId = activeLanDeviceId,
                    lanStatusByDevice = lanStatusByDevice,
                    onProbeTransferRuntime = ::probeTransferRuntime,
                    onStopTransferRuntime = ::stopTransferRuntime,
                    onShareTransferDeviceId = ::shareTransferDeviceId,
                    onAddPairedDevice = ::addPairedDevice,
                    onSetAllowWithoutAccept = ::setAllowWithoutAccept,
                    onRemovePairedDevice = ::removePairedDevice,
                    onTestLan = ::testLanConnection,
                    onDisconnectLan = ::disconnectLan,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onDestroy() {
        deviceExecutor.shutdownNow()
        transferExecutor.shutdownNow()
        super.onDestroy()
    }

    private fun probeTransferRuntime() {
        if (transferRuntimeBusy) return
        if (activeLanDeviceId != null) {
            Toast.makeText(
                this,
                "Desconectá la prueba LAN antes de volver a probar el motor.",
                Toast.LENGTH_SHORT,
            ).show()
            return
        }

        transferRuntimeBusy = true
        transferRuntimeStatus = "Iniciando motor de envío…"

        val started = runCatching {
            transferRuntimeController.start()
        }
        if (started.isFailure) {
            transferRuntimeBusy = false
            transferRuntimeStatus = "No se pudo iniciar el motor."
            Toast.makeText(
                this,
                "No se pudo iniciar el motor de envío.",
                Toast.LENGTH_SHORT,
            ).show()
            return
        }

        transferExecutor.execute {
            val result = runCatching {
                transferRuntimeController.probe()
            }
            runOnUiThread {
                transferRuntimeBusy = false
                result.onSuccess { probe ->
                    transferDeviceId = probe.deviceId
                    val shortId = probe.deviceId.take(7)
                    val isolation = if (probe.nonLoopbackAddressesChecked > 0) {
                        "loopback verificado"
                    } else {
                        "loopback configurado"
                    }
                    transferRuntimeStatus = "Listo · ID $shortId… · $isolation."
                }.onFailure { error ->
                    transferRuntimeStatus =
                        "Prueba falló: " + (error.message ?: "error desconocido")
                }
            }
        }
    }

    private fun stopTransferRuntime() {
        if (transferRuntimeBusy) return

        transferRuntimeBusy = true
        transferRuntimeStatus = "Deteniendo motor de envío…"
        val requested = runCatching {
            transferRuntimeController.stop()
        }
        if (requested.isFailure) {
            transferRuntimeBusy = false
            transferRuntimeStatus = "No se pudo solicitar la detención."
            return
        }

        transferExecutor.execute {
            val stopped = runCatching {
                transferRuntimeController.awaitStopped()
            }.getOrDefault(false)

            runOnUiThread {
                transferRuntimeBusy = false
                activeLanDeviceId = null
                lanBusyDeviceId = null
                lanStatusByDevice = emptyMap()
                transferRuntimeStatus = if (stopped) {
                    "Motor de envío detenido correctamente."
                } else {
                    "Android aún reporta el motor activo."
                }
            }
        }
    }

    private fun testLanConnection(device: PairedDevice) {
        if (transferRuntimeBusy || lanBusyDeviceId != null) return

        val active = activeLanDeviceId
        if (active != null && active != device.deviceId) {
            Toast.makeText(
                this,
                "Desconectá el otro dispositivo antes de probar uno diferente.",
                Toast.LENGTH_SHORT,
            ).show()
            return
        }
        if (active == device.deviceId) return

        transferRuntimeBusy = true
        lanBusyDeviceId = device.deviceId
        transferRuntimeStatus = "Buscando dispositivo por LAN…"
        lanStatusByDevice = lanStatusByDevice + (
            device.deviceId to "Buscando por LAN… Abrí OclAx en ambos teléfonos."
            )

        transferExecutor.execute {
            val result = runCatching {
                transferRuntimeController.connectLan(
                    rawDeviceId = device.deviceId,
                    name = device.name,
                )
            }

            runOnUiThread {
                transferRuntimeBusy = false
                lanBusyDeviceId = null
                result.onSuccess {
                    activeLanDeviceId = device.deviceId
                    lanStatusByDevice = lanStatusByDevice + (
                        device.deviceId to "Conectado por LAN."
                        )
                    transferRuntimeStatus = "Motor activo · conexión LAN verificada."
                }.onFailure { error ->
                    if (activeLanDeviceId == device.deviceId) {
                        activeLanDeviceId = null
                    }
                    lanStatusByDevice = lanStatusByDevice + (
                        device.deviceId to
                            ("No se conectó por LAN: " + (error.message ?: "error desconocido"))
                        )
                    transferRuntimeStatus = "LAN no conectó · motor volvió a modo aislado."
                }
            }
        }
    }

    private fun disconnectLan(device: PairedDevice) {
        if (transferRuntimeBusy || lanBusyDeviceId != null) return
        if (activeLanDeviceId != device.deviceId) return

        transferRuntimeBusy = true
        lanBusyDeviceId = device.deviceId
        lanStatusByDevice = lanStatusByDevice + (
            device.deviceId to "Desconectando LAN…"
            )

        transferExecutor.execute {
            val result = runCatching {
                transferRuntimeController.disconnectLan(device.deviceId)
            }

            runOnUiThread {
                transferRuntimeBusy = false
                lanBusyDeviceId = null
                activeLanDeviceId = null
                lanStatusByDevice = lanStatusByDevice + (
                    device.deviceId to if (result.isSuccess) {
                        "Desconectado · motor aislado."
                    } else {
                        "Se cerró la prueba LAN; revisá el motor antes de reintentar."
                    }
                    )
                transferRuntimeStatus = if (result.isSuccess) {
                    "Motor aislado."
                } else {
                    "Motor detenido por seguridad."
                }
            }
        }
    }

    private fun refresh() {
        retentionHours = store.retentionHours()
        items = store.listItems()
        pairedDevices = pairedDeviceStore.list()
        hasBroadFileAccess = deviceContentRepository.hasBroadFileAccess()
        refreshDeviceContent()
    }

    private fun addPairedDevice(name: String, rawDeviceId: String): String? {
        val normalized = PairedDeviceStore.normalizeDeviceId(rawDeviceId)
            ?: return "El ID del dispositivo no tiene un formato válido."

        if (transferDeviceId != null && normalized == transferDeviceId) {
            return "Ese ID pertenece a este mismo dispositivo."
        }

        return pairedDeviceStore.add(name, normalized)
            .fold(
                onSuccess = {
                    pairedDevices = pairedDeviceStore.list()
                    null
                },
                onFailure = { error ->
                    error.message ?: "No se pudo guardar el dispositivo."
                },
            )
    }

    private fun setAllowWithoutAccept(deviceId: String, allowed: Boolean) {
        if (pairedDeviceStore.setAllowWithoutAccept(deviceId, allowed)) {
            pairedDevices = pairedDeviceStore.list()
        }
    }

    private fun removePairedDevice(deviceId: String) {
        if (activeLanDeviceId == deviceId || lanBusyDeviceId == deviceId) {
            Toast.makeText(
                this,
                "Desconectá la prueba LAN antes de quitar ese dispositivo.",
                Toast.LENGTH_SHORT,
            ).show()
            return
        }

        if (pairedDeviceStore.remove(deviceId)) {
            pairedDevices = pairedDeviceStore.list()
            lanStatusByDevice = lanStatusByDevice - deviceId
        }
    }

    private fun shareTransferDeviceId() {
        val deviceId = transferDeviceId ?: return

        runCatching {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(
                    Intent.EXTRA_TEXT,
                    "OclAx · ID de dispositivo\n$deviceId",
                )
            }
            startActivity(Intent.createChooser(intent, "Compartir ID de OclAx"))
        }.onFailure {
            Toast.makeText(
                this,
                "No se pudo compartir el ID del dispositivo.",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun refreshDeviceContent() {
        deviceLoading = true
        deviceExecutor.execute {
            val apps = runCatching { installedAppsRepository.listInstalledApps() }.getOrDefault(emptyList())
            val files = if (deviceContentRepository.hasBroadFileAccess()) {
                runCatching { deviceContentRepository.listFiles() }.getOrDefault(emptyList())
            } else {
                emptyList()
            }

            runOnUiThread {
                installedApps = apps
                deviceFiles = files
                hasBroadFileAccess = deviceContentRepository.hasBroadFileAccess()
                deviceLoading = false
            }
        }
    }

    private fun requestBroadFileAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val appIntent = Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:$packageName"),
            )
            val fallbackIntent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            try {
                startActivity(appIntent)
            } catch (_: Exception) {
                startActivity(fallbackIntent)
            }
        } else {
            val permissions = buildList {
                if (
                    checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) !=
                    PackageManager.PERMISSION_GRANTED
                ) {
                    add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
                if (
                    Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                    PackageManager.PERMISSION_GRANTED
                ) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }

            if (permissions.isNotEmpty()) {
                requestPermissions(permissions.toTypedArray(), 4101)
            } else {
                refresh()
            }
        }
    }

    private fun openDeviceFile(file: DeviceFileInfo) {
        if (!contentOpener.open(file.uri, file.mimeType)) {
            Toast.makeText(
                this,
                "No hay una aplicación disponible para abrir este tipo de archivo.",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun shareDeviceFile(file: DeviceFileInfo) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = file.mimeType
                putExtra(Intent.EXTRA_STREAM, file.uri)
                clipData = ClipData.newUri(contentResolver, file.displayName, file.uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Compartir desde Mi dispositivo"))
        } catch (error: Exception) {
            Toast.makeText(
                this,
                "No se pudo compartir: " + (error.message ?: "error desconocido"),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun copyDeviceFile(file: DeviceFileInfo) {
        if (!file.type.supportsClipboardCopy()) return

        try {
            val clipboard = getSystemService(ClipboardManager::class.java)
            when (file.type) {
                ContentType.TEXT -> {
                    if (file.byteSize > MAX_CLIPBOARD_TEXT_BYTES) {
                        Toast.makeText(
                            this,
                            "Ese texto es demasiado grande para copiarlo al portapapeles.",
                            Toast.LENGTH_SHORT,
                        ).show()
                        return
                    }
                    val text = contentResolver.openInputStream(file.uri)
                        ?.bufferedReader(Charsets.UTF_8)
                        ?.use { it.readText() }
                        ?: return
                    clipboard.setPrimaryClip(ClipData.newPlainText(file.displayName, text))
                }

                ContentType.IMAGE -> {
                    clipboard.setPrimaryClip(
                        ClipData(
                            ClipDescription(file.displayName, arrayOf(file.mimeType)),
                            ClipData.Item(file.uri),
                        ),
                    )
                }

                else -> return
            }
            Toast.makeText(this, "Copiado al portapapeles.", Toast.LENGTH_SHORT).show()
        } catch (error: Exception) {
            Toast.makeText(
                this,
                "No se pudo copiar: " + (error.message ?: "error desconocido"),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun deleteDeviceFile(file: DeviceFileInfo) {
        when (val result = deviceContentRepository.requestDelete(file)) {
            DeviceDeleteResult.Deleted -> {
                Toast.makeText(this, "Se eliminó “${file.displayName}”.", Toast.LENGTH_SHORT).show()
                refresh()
            }

            is DeviceDeleteResult.NeedsUserConfirmation -> {
                pendingSystemDeleteName = file.displayName
                val request = IntentSenderRequest.Builder(result.intentSender).build()
                deviceDeleteLauncher.launch(request)
            }

            is DeviceDeleteResult.Failed -> {
                Toast.makeText(
                    this,
                    "No se pudo eliminar: " + (result.reason ?: "error desconocido"),
                    Toast.LENGTH_SHORT,
                ).show()
                refresh()
            }
        }
    }

    private fun shareInstalledApp(app: InstalledAppInfo) {
        Toast.makeText(this, "Preparando ${app.label}…", Toast.LENGTH_SHORT).show()
        deviceExecutor.execute {
            val prepared = runCatching { installedAppExporter.prepare(app) }

            runOnUiThread {
                prepared.onSuccess { exported ->
                    try {
                        val sendIntent = if (exported.uris.size == 1) {
                            Intent(Intent.ACTION_SEND).apply {
                                type = "application/vnd.android.package-archive"
                                putExtra(Intent.EXTRA_STREAM, exported.uris.first())
                            }
                        } else {
                            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                type = "application/vnd.android.package-archive"
                                putParcelableArrayListExtra(Intent.EXTRA_STREAM, exported.uris)
                            }
                        }

                        val clip = ClipData.newUri(
                            contentResolver,
                            exported.label,
                            exported.uris.first(),
                        )
                        exported.uris.drop(1).forEach { uri ->
                            clip.addItem(ClipData.Item(uri))
                        }
                        sendIntent.clipData = clip
                        sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                        startActivity(
                            Intent.createChooser(
                                sendIntent,
                                "Compartir aplicación instalada",
                            ),
                        )

                        Toast.makeText(
                            this,
                            if (exported.apkCount == 1) {
                                "Se comparte solo el APK de instalación; no tus datos."
                            } else {
                                "Se comparten ${exported.apkCount} APK del paquete; no tus datos."
                            },
                            Toast.LENGTH_LONG,
                        ).show()
                    } catch (error: Exception) {
                        Toast.makeText(
                            this,
                            "No se pudo compartir: " + (error.message ?: "error desconocido"),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }.onFailure { error ->
                    Toast.makeText(
                        this,
                        "No se pudo preparar la app: " + (error.message ?: "error desconocido"),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }

    private fun openInstalledApp(app: InstalledAppInfo) {
        val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
        if (launchIntent == null) {
            Toast.makeText(
                this,
                "Esta aplicación no tiene una pantalla que se pueda abrir.",
                Toast.LENGTH_SHORT,
            ).show()
            return
        }

        runCatching { startActivity(launchIntent) }
            .onFailure {
                Toast.makeText(this, "No se pudo abrir " + app.label + ".", Toast.LENGTH_SHORT).show()
            }
    }

    private fun openItem(item: StoredItem) {
        try {
            val opened = contentOpener.open(item, store.payloadFile(item))
            if (!opened) {
                Toast.makeText(
                    this,
                    "No hay una aplicación disponible para abrir este tipo de archivo.",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        } catch (error: Exception) {
            Toast.makeText(
                this,
                "No se pudo abrir: " + (error.message ?: "error desconocido"),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun shareItem(item: StoredItem) {
        try {
            val file = store.payloadFile(item)
            val sendIntent = if (item.mimeType == "text/plain") {
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, file.readText())
                }
            } else {
                val uri = FileProvider.getUriForFile(
                    this,
                    packageName + ".fileprovider",
                    file,
                )
                Intent(Intent.ACTION_SEND).apply {
                    type = item.mimeType
                    putExtra(Intent.EXTRA_STREAM, uri)
                    clipData = ClipData.newUri(contentResolver, item.displayName, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }

            startActivity(Intent.createChooser(sendIntent, "Compartir desde OclAx"))
        } catch (error: Exception) {
            Toast.makeText(
                this,
                "No se pudo compartir: " + (error.message ?: "error desconocido"),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun copyItem(item: StoredItem) {
        val type = contentTypeFor(item.mimeType)
        if (!type.supportsClipboardCopy()) return

        try {
            val clipboard = getSystemService(ClipboardManager::class.java)
            val file = store.payloadFile(item)

            when (type) {
                ContentType.TEXT -> {
                    if (item.byteSize > MAX_CLIPBOARD_TEXT_BYTES) {
                        Toast.makeText(
                            this,
                            "Ese texto es demasiado grande para copiarlo al portapapeles.",
                            Toast.LENGTH_SHORT,
                        ).show()
                        return
                    }
                    clipboard.setPrimaryClip(
                        ClipData.newPlainText(item.displayName, file.readText(Charsets.UTF_8)),
                    )
                }

                ContentType.IMAGE -> {
                    val uri = FileProvider.getUriForFile(
                        this,
                        packageName + ".fileprovider",
                        file,
                    )
                    clipboard.setPrimaryClip(
                        ClipData(
                            ClipDescription(item.displayName, arrayOf(item.mimeType)),
                            ClipData.Item(uri),
                        ),
                    )
                }

                else -> return
            }

            Toast.makeText(this, "Copiado al portapapeles.", Toast.LENGTH_SHORT).show()
        } catch (error: Exception) {
            Toast.makeText(
                this,
                "No se pudo copiar: " + (error.message ?: "error desconocido"),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }
}

private enum class ContentFilter(val label: String) {
    ALL("Todo"),
    PINNED("Fijados"),
    IMAGES("Imágenes"),
    DOCUMENTS("Documentos"),
    PDF("PDF"),
    APK("APK guardados"),
    TEXT("Texto/Código"),
    VIDEO("Video"),
    AUDIO("Audio"),
    OTHER("Otros"),
}

private enum class SourceMode(val label: String) {
    OCLAX("OclAx"),
    DEVICE("Mi dispositivo"),
}

@Composable
private fun OclAxHome(
    allItems: List<StoredItem>,
    installedApps: List<InstalledAppInfo>,
    deviceFiles: List<DeviceFileInfo>,
    hasBroadFileAccess: Boolean,
    deviceLoading: Boolean,
    retentionHours: Int,
    onRetentionChange: (Int) -> Unit,
    onPinToggle: (StoredItem) -> Unit,
    onDelete: (StoredItem) -> Unit,
    onOpen: (StoredItem) -> Unit,
    onShare: (StoredItem) -> Unit,
    onCopy: (StoredItem) -> Unit,
    onLoadItemThumbnail: (StoredItem, Int) -> Bitmap?,
    onRequestBroadAccess: () -> Unit,
    onOpenDeviceFile: (DeviceFileInfo) -> Unit,
    onShareDeviceFile: (DeviceFileInfo) -> Unit,
    onCopyDeviceFile: (DeviceFileInfo) -> Unit,
    onDeleteDeviceFile: (DeviceFileInfo) -> Unit,
    onOpenApp: (InstalledAppInfo) -> Unit,
    onShareApp: (InstalledAppInfo) -> Unit,
    onLoadDeviceThumbnail: (DeviceFileInfo, Int) -> Bitmap?,
    onLoadDeviceViewMode: (String, ContentViewMode) -> ContentViewMode,
    onSaveDeviceViewMode: (String, ContentViewMode) -> Unit,
    transferRuntimeStatus: String,
    transferRuntimeBusy: Boolean,
    transferDeviceId: String?,
    pairedDevices: List<PairedDevice>,
    lanBusyDeviceId: String?,
    activeLanDeviceId: String?,
    lanStatusByDevice: Map<String, String>,
    onProbeTransferRuntime: () -> Unit,
    onStopTransferRuntime: () -> Unit,
    onShareTransferDeviceId: () -> Unit,
    onAddPairedDevice: (String, String) -> String?,
    onSetAllowWithoutAccept: (String, Boolean) -> Unit,
    onRemovePairedDevice: (String) -> Unit,
    onTestLan: (PairedDevice) -> Unit,
    onDisconnectLan: (PairedDevice) -> Unit,
) {
    var sourceMode by remember { mutableStateOf(SourceMode.OCLAX) }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf<ContentFilter?>(null) }
    var pendingDelete by remember { mutableStateOf<StoredItem?>(null) }

    val visibleItems = remember(query, filter, allItems) {
        val needle = query.trim().lowercase()
        val activeFilter = filter ?: ContentFilter.ALL
        allItems.filter { item ->
            matchesFilter(item, activeFilter) &&
                (
                    needle.isEmpty() ||
                        item.displayName.lowercase().contains(needle) ||
                        item.mimeType.lowercase().contains(needle)
                    )
        }
    }
    val categoryOverview = remember(allItems) {
        contentFilterOverviewItems(allItems)
    }

    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("¿Eliminar de OclAx?") },
            text = {
                Text(
                    "Se eliminará únicamente la copia interna de OclAx. " +
                        "El archivo original del teléfono no se tocará.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        onDelete(item)
                    },
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancelar")
                }
            },
        )
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 18.dp),
        ) {
            Text(
                "OclAx",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "Tu contenido, listo donde lo necesitás.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(12.dp))

            SourceModeSwitch(
                selected = sourceMode,
                onSelect = { selected ->
                    sourceMode = selected
                    query = ""
                    filter = null
                },
            )
            Spacer(Modifier.height(12.dp))

            if (sourceMode == SourceMode.DEVICE) {
                DeviceBrowser(
                    files = deviceFiles,
                    apps = installedApps,
                    hasBroadFileAccess = hasBroadFileAccess,
                    isLoading = deviceLoading,
                    onRequestBroadAccess = onRequestBroadAccess,
                    onOpenFile = onOpenDeviceFile,
                    onShareFile = onShareDeviceFile,
                    onCopyFile = onCopyDeviceFile,
                    onDeleteFile = onDeleteDeviceFile,
                    onOpenApp = onOpenApp,
                    onShareApp = onShareApp,
                    onLoadThumbnail = onLoadDeviceThumbnail,
                    onLoadViewMode = onLoadDeviceViewMode,
                    onSaveViewMode = onSaveDeviceViewMode,
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 18.dp),
                ) {
                    if (BuildConfig.DEBUG) {
                        item(key = "transfer-devices") {
                            TransferDevicesSection(
                                status = transferRuntimeStatus,
                                busy = transferRuntimeBusy,
                                ownDeviceId = transferDeviceId,
                                devices = pairedDevices,
                                lanBusyDeviceId = lanBusyDeviceId,
                                activeLanDeviceId = activeLanDeviceId,
                                lanStatusByDevice = lanStatusByDevice,
                                onProbe = onProbeTransferRuntime,
                                onStop = onStopTransferRuntime,
                                onShareOwnId = onShareTransferDeviceId,
                                onAddDevice = onAddPairedDevice,
                                onSetAllowWithoutAccept = onSetAllowWithoutAccept,
                                onRemoveDevice = onRemovePairedDevice,
                                onTestLan = onTestLan,
                                onDisconnectLan = onDisconnectLan,
                            )
                        }
                    }

                    item(key = "oclax-search") {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Buscar en OclAx") },
                        )
                    }

                    if (query.isBlank() && filter == null) {
                        item(key = "oclax-retention") {
                            RetentionControl(
                                retentionHours = retentionHours,
                                onRetentionChange = onRetentionChange,
                            )
                        }

                        item(key = "oclax-category-overview") {
                            CategoryOverviewGrid(
                                items = categoryOverview,
                                onSelect = { selected ->
                                    filter = ContentFilter.entries
                                        .firstOrNull { it.name == selected.key }
                                },
                            )
                        }
                    } else {
                        item(key = "oclax-content-header") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                TextButton(
                                    onClick = {
                                        filter = null
                                        query = ""
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ArrowBack,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Text("Categorías")
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    filter?.label ?: "Resultados",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }

                        if (visibleItems.isEmpty()) {
                            item(key = "oclax-empty") {
                                Text(
                                    if (allItems.isEmpty()) {
                                        "Todavía no hay elementos. Compartí contenido hacia OclAx."
                                    } else {
                                        "No se encontraron elementos para esa búsqueda."
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                        } else {
                            items(visibleItems, key = { it.id }) { item ->
                                ItemCard(
                                    item = item,
                                    onPinToggle = onPinToggle,
                                    onDeleteRequest = { pendingDelete = item },
                                    onOpen = onOpen,
                                    onShare = onShare,
                                    onCopy = onCopy,
                                    onLoadThumbnail = onLoadItemThumbnail,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceModeSwitch(
    selected: SourceMode,
    onSelect: (SourceMode) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SourceMode.entries.forEach { option ->
            val selectedOption = selected == option
            if (selectedOption) {
                Button(
                    onClick = { onSelect(option) },
                    modifier = Modifier.weight(1f).height(38.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                ) {
                    Text(option.label)
                }
            } else {
                OutlinedButton(
                    onClick = { onSelect(option) },
                    modifier = Modifier.weight(1f).height(38.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                ) {
                    Text(option.label)
                }
            }
        }
    }
}

private fun contentFilterOverviewItems(items: List<StoredItem>): List<CategoryOverviewItem> =
    ContentFilter.entries.map { filter ->
        val matching = items.filter { matchesFilter(it, filter) }
        CategoryOverviewItem(
            key = filter.name,
            label = filter.label,
            icon = contentFilterIcon(filter),
            count = matching.size,
            totalBytes = matching.sumOf { it.byteSize },
            note = when (filter) {
                ContentFilter.ALL -> "Todo tu contenido OclAx"
                ContentFilter.PINNED -> "No caduca automáticamente"
                else -> null
            },
        )
    }

private fun contentFilterIcon(filter: ContentFilter): ImageVector = when (filter) {
    ContentFilter.ALL -> Icons.Outlined.InsertDriveFile
    ContentFilter.PINNED -> Icons.Filled.Star
    ContentFilter.IMAGES -> Icons.Outlined.ImageIcon
    ContentFilter.DOCUMENTS -> Icons.Outlined.Description
    ContentFilter.PDF -> Icons.Outlined.PictureAsPdf
    ContentFilter.APK -> Icons.Outlined.Android
    ContentFilter.TEXT -> Icons.Outlined.Code
    ContentFilter.VIDEO -> Icons.Outlined.Movie
    ContentFilter.AUDIO -> Icons.Outlined.AudioFile
    ContentFilter.OTHER -> Icons.Outlined.InsertDriveFile
}

private fun matchesFilter(item: StoredItem, filter: ContentFilter): Boolean {
    val type = contentTypeFor(item.mimeType)
    return when (filter) {
        ContentFilter.ALL -> true
        ContentFilter.PINNED -> item.pinned
        ContentFilter.IMAGES -> type == ContentType.IMAGE
        ContentFilter.PDF -> type == ContentType.PDF
        ContentFilter.APK -> type == ContentType.APP
        ContentFilter.TEXT -> type == ContentType.TEXT
        ContentFilter.VIDEO -> type == ContentType.VIDEO
        ContentFilter.AUDIO -> type == ContentType.AUDIO
        ContentFilter.DOCUMENTS -> type == ContentType.DOCUMENT
        ContentFilter.OTHER -> type == ContentType.OTHER
    }
}

@Composable
private fun RetentionControl(
    retentionHours: Int,
    onRetentionChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            "Autolimpieza",
            style = MaterialTheme.typography.labelMedium,
        )
        Box(
            modifier = Modifier
                .height(48.dp)
                .clickable { expanded = true },
            contentAlignment = Alignment.CenterStart,
        ) {
            Surface(
                modifier = Modifier.height(30.dp),
                shape = RoundedCornerShape(50),
                color = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        retentionCompactLabel(retentionHours),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            listOf(1, 24, 72, 168, 0).forEach { hours ->
                DropdownMenuItem(
                    text = { Text(retentionLabel(hours)) },
                    onClick = {
                        expanded = false
                        onRetentionChange(hours)
                    },
                )
            }
        }
        Text(
            "Solo borra copias de OclAx; nunca el archivo original.",
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun ItemCard(
    item: StoredItem,
    onPinToggle: (StoredItem) -> Unit,
    onDeleteRequest: () -> Unit,
    onOpen: (StoredItem) -> Unit,
    onShare: (StoredItem) -> Unit,
    onCopy: (StoredItem) -> Unit,
    onLoadThumbnail: (StoredItem, Int) -> Bitmap?,
) {
    val type = contentTypeFor(item.mimeType)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen(item) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        border = BorderStroke(
            1.dp,
            if (item.pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StoredItemVisual(
                    item = item,
                    type = type,
                    onLoadThumbnail = onLoadThumbnail,
                )

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        type.label + " · " + formatBytes(item.byteSize),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        DateFormat
                            .getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                            .format(Date(item.createdAt)),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CompactActionButton(
                    icon = Icons.Outlined.Share,
                    description = "Compartir ${item.displayName}",
                    onClick = { onShare(item) },
                )

                if (type.supportsClipboardCopy()) {
                    CompactActionButton(
                        icon = Icons.Outlined.ContentCopy,
                        description = "Copiar ${item.displayName}",
                        onClick = { onCopy(item) },
                    )
                }

                CompactActionButton(
                    icon = if (item.pinned) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    description = if (item.pinned) {
                        "Desfijar ${item.displayName}"
                    } else {
                        "Fijar ${item.displayName}"
                    },
                    onClick = { onPinToggle(item) },
                )

                CompactActionButton(
                    icon = Icons.Outlined.Delete,
                    description = "Eliminar ${item.displayName}",
                    tint = MaterialTheme.colorScheme.error,
                    onClick = onDeleteRequest,
                )
            }
        }
    }
}

@Composable
private fun StoredItemVisual(
    item: StoredItem,
    type: ContentType,
    onLoadThumbnail: (StoredItem, Int) -> Bitmap?,
) {
    val supportsThumbnail =
        type == ContentType.IMAGE || type == ContentType.VIDEO || type == ContentType.PDF
    val targetPx = if (supportsThumbnail) 192 else 96
    var thumbnail by remember(item.id, item.createdAt, targetPx) {
        mutableStateOf<Bitmap?>(null)
    }

    LaunchedEffect(item.id, item.createdAt, targetPx) {
        thumbnail = if (supportsThumbnail) {
            withContext(Dispatchers.IO) {
                onLoadThumbnail(item, targetPx)
            }
        } else {
            null
        }
    }

    val bitmap = thumbnail
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Miniatura de ${item.displayName}",
            modifier = Modifier
                .size(62.dp)
                .clip(MaterialTheme.shapes.small),
            contentScale = ContentScale.Crop,
        )
    } else {
        Surface(
            modifier = Modifier.size(if (supportsThumbnail) 62.dp else 42.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = MaterialTheme.shapes.small,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = contentTypeIcon(type),
                    contentDescription = type.label,
                    modifier = Modifier.size(if (supportsThumbnail) 28.dp else 22.dp),
                )
            }
        }
    }
}

@Composable
private fun InstalledAppCard(app: InstalledAppInfo) {
    val bitmap = remember(app.packageName) {
        app.icon.toBitmap(width = 48, height = 48).asImageBitmap()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                bitmap = bitmap,
                contentDescription = "Icono de ${app.label}",
                modifier = Modifier.size(38.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    app.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    "Aplicación instalada",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

private fun contentTypeIcon(type: ContentType): ImageVector = when (type) {
    ContentType.IMAGE -> Icons.Outlined.ImageIcon
    ContentType.PDF -> Icons.Outlined.PictureAsPdf
    ContentType.APP -> Icons.Outlined.Android
    ContentType.DOCUMENT -> Icons.Outlined.Description
    ContentType.TEXT -> Icons.Outlined.Code
    ContentType.VIDEO -> Icons.Outlined.Movie
    ContentType.AUDIO -> Icons.Outlined.AudioFile
    ContentType.OTHER -> Icons.Outlined.InsertDriveFile
}

@Composable
private fun CompactActionButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    tint: Color? = null,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = tint ?: MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
    }
}

private fun retentionCompactLabel(hours: Int): String = when (hours) {
    1 -> "1 h"
    24 -> "24 h"
    72 -> "3 d"
    168 -> "7 d"
    0 -> "Nunca"
    else -> "24 h"
}

private fun retentionLabel(hours: Int): String = when (hours) {
    1 -> "1 hora"
    24 -> "24 horas"
    72 -> "3 días"
    168 -> "7 días"
    0 -> "Nunca"
    else -> "24 horas"
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.1f MB", mb)
    return String.format("%.2f GB", mb / 1024.0)
}
