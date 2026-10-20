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
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
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
import androidx.compose.material.icons.outlined.Edit
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
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import io.github.hnalvaradohn.oclax.data.ContentViewMode
import io.github.hnalvaradohn.oclax.data.ItemStore
import io.github.hnalvaradohn.oclax.data.ViewModePreferences
import io.github.hnalvaradohn.oclax.model.ContentType
import io.github.hnalvaradohn.oclax.model.StoredItem
import io.github.hnalvaradohn.oclax.model.bulkPinTarget
import io.github.hnalvaradohn.oclax.model.bulkShareMimeType
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
import io.github.hnalvaradohn.oclax.ui.CategoryOverviewGrid
import io.github.hnalvaradohn.oclax.ui.CategoryOverviewItem
import io.github.hnalvaradohn.oclax.ui.theme.OclAxTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.Executors

private const val MAX_BULK_SELECTION = 100
private const val MAX_BULK_INSTALLED_APPS = 20

class MainActivity : ComponentActivity() {
    companion object {
        private const val MAX_CLIPBOARD_TEXT_BYTES = 2L * 1024L * 1024L
    }

    private val store by lazy { ItemStore(applicationContext) }
    private val installedAppsRepository by lazy { InstalledAppsRepository(applicationContext) }
    private val installedAppExporter by lazy { InstalledAppExporter(applicationContext) }
    private val deviceContentRepository by lazy { DeviceContentRepository(applicationContext) }
    private val thumbnailLoader by lazy { ThumbnailLoader(applicationContext) }
    private val viewModePreferences by lazy { ViewModePreferences(applicationContext) }
    private val contentOpener by lazy { ContentOpener(this) }
    private val deviceExecutor = Executors.newSingleThreadExecutor()
    private var items by mutableStateOf<List<StoredItem>>(emptyList())
    private var installedApps by mutableStateOf<List<InstalledAppInfo>>(emptyList())
    private var deviceFiles by mutableStateOf<List<DeviceFileInfo>>(emptyList())
    private var hasBroadFileAccess by mutableStateOf(false)
    private var deviceLoading by mutableStateOf(false)
    private var retentionHours by mutableIntStateOf(24)
    private var pendingSystemDeleteName: String? = null

    private val deviceDeleteLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val name = pendingSystemDeleteName
        pendingSystemDeleteName = null

        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(
                this,
                if (name.isNullOrBlank()) {
                    getString(R.string.toast_file_deleted)
                } else {
                    getString(R.string.toast_named_file_deleted, name)
                },
                Toast.LENGTH_SHORT,
            ).show()
            refresh()
        } else {
            Toast.makeText(this, getString(R.string.toast_file_not_deleted), Toast.LENGTH_SHORT).show()
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
                                getString(R.string.toast_oclax_copy_delete_failed),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                        refresh()
                    },
                    onShareMany = ::shareItems,
                    onSetPinnedMany = ::setItemsPinned,
                    onDeleteMany = ::deleteItems,
                    onOpen = ::openItem,
                    onShare = ::shareItem,
                    onCopy = ::copyItem,
                    onRename = ::renameItem,
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
                    onShareDeviceFiles = ::shareDeviceFiles,
                    onCopyDeviceFile = ::copyDeviceFile,
                    onDeleteDeviceFile = ::deleteDeviceFile,
                    onRenameDeviceFile = ::renameDeviceFile,
                    onOpenApp = ::openInstalledApp,
                    onShareApp = ::shareInstalledApp,
                    onShareApps = ::shareInstalledApps,
                    onLoadDeviceThumbnail = thumbnailLoader::loadDevice,
                    onLoadDeviceViewMode = viewModePreferences::getDeviceMode,
                    onSaveDeviceViewMode = viewModePreferences::setDeviceMode,
                    onExitApp = { finish() },
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
        super.onDestroy()
    }

    private fun refresh() {
        retentionHours = store.retentionHours()
        items = store.listItems()
        hasBroadFileAccess = deviceContentRepository.hasBroadFileAccess()
        refreshDeviceContent()
    }

    private fun renameItem(item: StoredItem, newName: String) {
        deviceExecutor.execute {
            val result = runCatching { store.renameItem(item.id, newName) }
            runOnUiThread {
                result.onSuccess { renamed ->
                    val message = when (renamed.originalRenamed) {
                        true -> getString(R.string.toast_renamed_both)
                        false -> getString(R.string.toast_renamed_oclax_only)
                        null -> getString(R.string.toast_renamed_oclax)
                    }
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }.onFailure { error ->
                    Toast.makeText(
                        this,
                        getString(
                            R.string.toast_rename_failed,
                            error.message ?: getString(R.string.unknown_error),
                        ),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
                refresh()
            }
        }
    }

    private fun renameDeviceFile(file: DeviceFileInfo, newName: String) {
        deviceExecutor.execute {
            val result = runCatching { deviceContentRepository.rename(file, newName) }
            runOnUiThread {
                result.onSuccess {
                    Toast.makeText(
                        this,
                        getString(R.string.toast_original_renamed),
                        Toast.LENGTH_SHORT,
                    ).show()
                }.onFailure { error ->
                    Toast.makeText(
                        this,
                        getString(
                            R.string.toast_rename_failed,
                            error.message ?: getString(R.string.unknown_error),
                        ),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
                refresh()
            }
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
                getString(R.string.toast_no_opener),
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
            startActivity(Intent.createChooser(intent, getString(R.string.chooser_share_device)))
        } catch (error: Exception) {
            Toast.makeText(
                this,
                getString(
                    R.string.toast_share_failed,
                    error.message ?: getString(R.string.unknown_error),
                ),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun shareDeviceFiles(selectedFiles: List<DeviceFileInfo>) {
        if (selectedFiles.isEmpty()) return
        if (selectedFiles.size == 1) {
            shareDeviceFile(selectedFiles.first())
            return
        }
        if (selectedFiles.size > MAX_BULK_SELECTION) {
            Toast.makeText(
                this,
                getString(R.string.toast_share_limit, MAX_BULK_SELECTION),
                Toast.LENGTH_SHORT,
            ).show()
            return
        }

        try {
            val uris = ArrayList(selectedFiles.map { it.uri })
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = bulkShareMimeType(selectedFiles.map { it.mimeType })
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val clip = ClipData.newUri(
                contentResolver,
                selectedFiles.first().displayName,
                uris.first(),
            )
            uris.drop(1).forEach { uri -> clip.addItem(ClipData.Item(uri)) }
            intent.clipData = clip
            startActivity(Intent.createChooser(intent, getString(R.string.chooser_share_device)))
        } catch (error: Exception) {
            Toast.makeText(
                this,
                getString(
                    R.string.toast_share_many_failed,
                    error.message ?: getString(R.string.unknown_error),
                ),
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
                            getString(R.string.toast_text_too_large),
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
            Toast.makeText(this, getString(R.string.toast_copied), Toast.LENGTH_SHORT).show()
        } catch (error: Exception) {
            Toast.makeText(
                this,
                getString(
                    R.string.toast_copy_failed,
                    error.message ?: getString(R.string.unknown_error),
                ),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun deleteDeviceFile(file: DeviceFileInfo) {
        when (val result = deviceContentRepository.requestDelete(file)) {
            DeviceDeleteResult.Deleted -> {
                Toast.makeText(
                    this,
                    getString(R.string.toast_named_file_deleted, file.displayName),
                    Toast.LENGTH_SHORT,
                ).show()
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
                    getString(
                        R.string.toast_delete_failed,
                        result.reason ?: getString(R.string.unknown_error),
                    ),
                    Toast.LENGTH_SHORT,
                ).show()
                refresh()
            }
        }
    }

    private fun shareInstalledApp(app: InstalledAppInfo) {
        Toast.makeText(
            this,
            getString(R.string.toast_preparing_app, app.label),
            Toast.LENGTH_SHORT,
        ).show()
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
                                getString(R.string.chooser_share_installed_app),
                            ),
                        )

                        Toast.makeText(
                            this,
                            if (exported.apkCount == 1) {
                                getString(R.string.toast_share_single_apk)
                            } else {
                                getString(R.string.toast_share_split_apks, exported.apkCount)
                            },
                            Toast.LENGTH_LONG,
                        ).show()
                    } catch (error: Exception) {
                        Toast.makeText(
                            this,
                            getString(
                                R.string.toast_share_failed,
                                error.message ?: getString(R.string.unknown_error),
                            ),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }.onFailure { error ->
                    Toast.makeText(
                        this,
                        getString(
                            R.string.toast_prepare_app_failed,
                            error.message ?: getString(R.string.unknown_error),
                        ),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }

    private fun shareInstalledApps(selectedApps: List<InstalledAppInfo>) {
        if (selectedApps.isEmpty()) return
        if (selectedApps.size == 1) {
            shareInstalledApp(selectedApps.first())
            return
        }
        if (selectedApps.size > MAX_BULK_INSTALLED_APPS) {
            Toast.makeText(
                this,
                getString(R.string.toast_app_limit, MAX_BULK_INSTALLED_APPS),
                Toast.LENGTH_SHORT,
            ).show()
            return
        }

        Toast.makeText(
            this,
            getString(R.string.toast_preparing_apps, selectedApps.size),
            Toast.LENGTH_SHORT,
        ).show()

        deviceExecutor.execute {
            val prepared = runCatching {
                val exports = selectedApps.map { app -> installedAppExporter.prepare(app) }
                val uris = ArrayList(exports.flatMap { it.uris })
                require(uris.size <= MAX_BULK_SELECTION) {
                    getString(R.string.toast_too_many_apks)
                }
                uris
            }

            runOnUiThread {
                prepared.onSuccess { uris ->
                    try {
                        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                            type = "application/vnd.android.package-archive"
                            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        val clip = ClipData.newUri(
                            contentResolver,
                            getString(R.string.chooser_installed_apps),
                            uris.first(),
                        )
                        uris.drop(1).forEach { uri -> clip.addItem(ClipData.Item(uri)) }
                        intent.clipData = clip
                        startActivity(
                            Intent.createChooser(
                                intent,
                                getString(R.string.chooser_share_installed_apps),
                            ),
                        )
                    } catch (error: Exception) {
                        Toast.makeText(
                            this,
                            getString(
                                R.string.toast_share_apps_failed,
                                error.message ?: getString(R.string.unknown_error),
                            ),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }.onFailure { error ->
                    Toast.makeText(
                        this,
                        getString(
                            R.string.toast_prepare_apps_failed,
                            error.message ?: getString(R.string.unknown_error),
                        ),
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
                getString(R.string.toast_no_app_screen),
                Toast.LENGTH_SHORT,
            ).show()
            return
        }

        runCatching { startActivity(launchIntent) }
            .onFailure {
                Toast.makeText(
            this,
            getString(R.string.toast_open_app_failed, app.label),
            Toast.LENGTH_SHORT,
        ).show()
            }
    }

    private fun openItem(item: StoredItem) {
        try {
            val opened = contentOpener.open(item, store.payloadFile(item))
            if (!opened) {
                Toast.makeText(
                    this,
                    getString(R.string.toast_no_opener),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        } catch (error: Exception) {
            Toast.makeText(
                this,
                getString(
                    R.string.toast_open_failed,
                    error.message ?: getString(R.string.unknown_error),
                ),
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

            startActivity(Intent.createChooser(sendIntent, getString(R.string.chooser_share_oclax)))
        } catch (error: Exception) {
            Toast.makeText(
                this,
                getString(
                    R.string.toast_share_failed,
                    error.message ?: getString(R.string.unknown_error),
                ),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun shareItems(selectedItems: List<StoredItem>) {
        if (selectedItems.isEmpty()) return
        if (selectedItems.size > MAX_BULK_SELECTION) {
            Toast.makeText(
                this,
                getString(R.string.toast_share_items_limit, MAX_BULK_SELECTION),
                Toast.LENGTH_SHORT,
            ).show()
            return
        }
        if (selectedItems.size == 1) {
            shareItem(selectedItems.first())
            return
        }

        try {
            val uris = ArrayList(
                selectedItems.map { item ->
                    FileProvider.getUriForFile(
                        this,
                        packageName + ".fileprovider",
                        store.payloadFile(item),
                    )
                },
            )
            val sendIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = bulkShareMimeType(selectedItems.map { it.mimeType })
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val clip = ClipData.newUri(
                contentResolver,
                selectedItems.first().displayName,
                uris.first(),
            )
            uris.drop(1).forEach { uri ->
                clip.addItem(ClipData.Item(uri))
            }
            sendIntent.clipData = clip

            startActivity(Intent.createChooser(sendIntent, getString(R.string.chooser_share_oclax)))
        } catch (error: Exception) {
            Toast.makeText(
                this,
                getString(
                    R.string.toast_share_items_failed,
                    error.message ?: getString(R.string.unknown_error),
                ),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun setItemsPinned(selectedItems: List<StoredItem>, pinned: Boolean) {
        if (selectedItems.isEmpty()) return
        deviceExecutor.execute {
            val updated = selectedItems.count { item ->
                store.setPinned(item.id, pinned) != null
            }
            runOnUiThread {
                refresh()
                Toast.makeText(
                    this,
                    if (pinned) {
                        getString(R.string.toast_pinned_many, updated, selectedItems.size)
                    } else {
                        getString(R.string.toast_unpinned_many, updated, selectedItems.size)
                    },
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    private fun deleteItems(selectedItems: List<StoredItem>) {
        if (selectedItems.isEmpty()) return
        deviceExecutor.execute {
            val deleted = selectedItems.count { item -> store.deleteItem(item.id) }
            runOnUiThread {
                refresh()
                Toast.makeText(
                    this,
                    getString(R.string.toast_deleted_many, deleted, selectedItems.size),
                    Toast.LENGTH_SHORT,
                ).show()
            }
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
                            getString(R.string.toast_text_too_large),
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

            Toast.makeText(this, getString(R.string.toast_copied), Toast.LENGTH_SHORT).show()
        } catch (error: Exception) {
            Toast.makeText(
                this,
                getString(
                    R.string.toast_copy_failed,
                    error.message ?: getString(R.string.unknown_error),
                ),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }
}

private enum class ContentFilter(@StringRes val labelRes: Int) {
    ALL(R.string.filter_all),
    PINNED(R.string.filter_pinned),
    IMAGES(R.string.filter_images),
    DOCUMENTS(R.string.filter_documents),
    PDF(R.string.filter_pdf),
    APK(R.string.filter_saved_apk),
    TEXT(R.string.filter_text),
    VIDEO(R.string.filter_video),
    AUDIO(R.string.filter_audio),
    OTHER(R.string.filter_other),
}

private enum class SourceMode(@StringRes val labelRes: Int) {
    OCLAX(R.string.app_name),
    DEVICE(R.string.device_tab),
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
    onShareMany: (List<StoredItem>) -> Unit,
    onSetPinnedMany: (List<StoredItem>, Boolean) -> Unit,
    onDeleteMany: (List<StoredItem>) -> Unit,
    onOpen: (StoredItem) -> Unit,
    onShare: (StoredItem) -> Unit,
    onCopy: (StoredItem) -> Unit,
    onRename: (StoredItem, String) -> Unit,
    onLoadItemThumbnail: (StoredItem, Int) -> Bitmap?,
    onRequestBroadAccess: () -> Unit,
    onOpenDeviceFile: (DeviceFileInfo) -> Unit,
    onShareDeviceFile: (DeviceFileInfo) -> Unit,
    onShareDeviceFiles: (List<DeviceFileInfo>) -> Unit,
    onCopyDeviceFile: (DeviceFileInfo) -> Unit,
    onDeleteDeviceFile: (DeviceFileInfo) -> Unit,
    onRenameDeviceFile: (DeviceFileInfo, String) -> Unit,
    onOpenApp: (InstalledAppInfo) -> Unit,
    onShareApp: (InstalledAppInfo) -> Unit,
    onShareApps: (List<InstalledAppInfo>) -> Unit,
    onLoadDeviceThumbnail: (DeviceFileInfo, Int) -> Bitmap?,
    onLoadDeviceViewMode: (String, ContentViewMode) -> ContentViewMode,
    onSaveDeviceViewMode: (String, ContentViewMode) -> Unit,
    onExitApp: () -> Unit,
) {
    var sourceMode by remember { mutableStateOf(SourceMode.OCLAX) }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf<ContentFilter?>(null) }
    var pendingDelete by remember { mutableStateOf<StoredItem?>(null) }
    var pendingRename by remember { mutableStateOf<StoredItem?>(null) }
    var renameText by remember { mutableStateOf("") }
    var selecting by remember { mutableStateOf(false) }
    var selectedItemIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showBulkDeleteConfirmation by remember { mutableStateOf(false) }
    var showExitConfirmation by remember { mutableStateOf(false) }
    var exitArmed by remember { mutableStateOf(false) }

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
    val categoryOverview = contentFilterOverviewItems(allItems)
    val selectedItems = remember(allItems, selectedItemIds) {
        allItems.filter { it.id in selectedItemIds }
    }
    val bulkPinValue = bulkPinTarget(selectedItems.map { it.pinned })

    LaunchedEffect(allItems) {
        val validIds = allItems.mapTo(mutableSetOf()) { it.id }
        selectedItemIds = selectedItemIds.intersect(validIds)
        if (selectedItemIds.isEmpty() && selecting && allItems.isEmpty()) {
            selecting = false
        }
    }

    LaunchedEffect(sourceMode, query, filter) {
        if (sourceMode != SourceMode.OCLAX || query.isNotBlank() || filter != null) {
            showExitConfirmation = false
            exitArmed = false
        }
    }

    BackHandler {
        if (selecting) {
            selecting = false
            selectedItemIds = emptySet()
            return@BackHandler
        }

        when (
            homeBackAction(
                sourceIsOclAx = sourceMode == SourceMode.OCLAX,
                hasContentDrillDown = query.isNotBlank() || filter != null,
                exitArmed = exitArmed,
            )
        ) {
            HomeBackAction.CLEAR_OCLAX_VIEW -> {
                query = ""
                filter = null
            }
            HomeBackAction.RETURN_TO_OCLAX -> {
                sourceMode = SourceMode.OCLAX
                query = ""
                filter = null
            }
            HomeBackAction.SHOW_EXIT_CONFIRMATION -> {
                exitArmed = true
                showExitConfirmation = true
            }
            HomeBackAction.EXIT_APP -> onExitApp()
        }
    }

    if (showExitConfirmation) {
        AlertDialog(
            onDismissRequest = onExitApp,
            title = { Text(stringResource(R.string.exit_title)) },
            text = {
                Text(stringResource(R.string.exit_message))
            },
            confirmButton = {
                TextButton(onClick = onExitApp) {
                    Text(stringResource(R.string.exit))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmation = false }) {
                    Text(stringResource(R.string.no))
                }
            },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
            ),
        )
    }

    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.delete_oclax_title)) },
            text = {
                Text(stringResource(R.string.delete_oclax_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        onDelete(item)
                    },
                ) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    pendingRename?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingRename = null },
            title = { Text(stringResource(R.string.rename)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (item.sourceUri != null) {
                            stringResource(R.string.rename_linked_original)
                        } else {
                            stringResource(R.string.rename_unlinked)
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        singleLine = true,
                        label = { Text(stringResource(R.string.name)) },
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = pendingRename ?: return@TextButton
                        pendingRename = null
                        onRename(target, renameText)
                    },
                ) {
                    Text(stringResource(R.string.rename))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRename = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showBulkDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteConfirmation = false },
            title = {
                Text(stringResource(R.string.delete_many_title, selectedItems.size))
            },
            text = {
                Text(stringResource(R.string.delete_many_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBulkDeleteConfirmation = false
                        onDeleteMany(selectedItems)
                        selecting = false
                        selectedItemIds = emptySet()
                    },
                ) {
                    Text(stringResource(R.string.delete_copies), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
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
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                stringResource(R.string.tagline),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(12.dp))

            SourceModeSwitch(
                selected = sourceMode,
                onSelect = { selected ->
                    sourceMode = selected
                    query = ""
                    filter = null
                    selecting = false
                    selectedItemIds = emptySet()
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
                    onShareFiles = onShareDeviceFiles,
                    onCopyFile = onCopyDeviceFile,
                    onDeleteFile = onDeleteDeviceFile,
                    onRenameFile = onRenameDeviceFile,
                    onOpenApp = onOpenApp,
                    onShareApp = onShareApp,
                    onShareApps = onShareApps,
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
                    item(key = "oclax-search") {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !selecting,
                            singleLine = true,
                            label = { Text(stringResource(R.string.search_oclax)) },
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
                                categories = categoryOverview,
                                onSelect = { selected ->
                                    filter = ContentFilter.entries
                                        .firstOrNull { it.name == selected.key }
                                },
                            )
                        }
                    } else {
                        item(key = "oclax-content-header") {
                            if (selecting) {
                                BulkSelectionBar(
                                    selectedCount = selectedItems.size,
                                    allVisibleSelected = visibleItems.isNotEmpty() &&
                                        visibleItems.all { it.id in selectedItemIds },
                                    canAct = selectedItems.isNotEmpty(),
                                    pinValue = bulkPinValue,
                                    onToggleAll = {
                                        val visibleIds = visibleItems
                                            .take(MAX_BULK_SELECTION)
                                            .mapTo(mutableSetOf()) { it.id }
                                        selectedItemIds = if (
                                            visibleIds.isNotEmpty() &&
                                            visibleIds.all { it in selectedItemIds }
                                        ) {
                                            selectedItemIds - visibleIds
                                        } else {
                                            visibleIds
                                        }
                                    },
                                    onShare = {
                                        onShareMany(selectedItems)
                                        selecting = false
                                        selectedItemIds = emptySet()
                                    },
                                    onPin = {
                                        onSetPinnedMany(selectedItems, bulkPinValue)
                                        selecting = false
                                        selectedItemIds = emptySet()
                                    },
                                    onDelete = {
                                        showBulkDeleteConfirmation = true
                                    },
                                    onCancel = {
                                        selecting = false
                                        selectedItemIds = emptySet()
                                    },
                                )
                            } else {
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
                                        Text(stringResource(R.string.categories))
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        filter?.let { stringResource(it.labelRes) }
                                            ?: stringResource(R.string.results),
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    if (visibleItems.isNotEmpty()) {
                                        TextButton(
                                            onClick = {
                                                selecting = true
                                                selectedItemIds = emptySet()
                                            },
                                        ) {
                                            Text(stringResource(R.string.select))
                                        }
                                    }
                                }
                            }
                        }

                        if (visibleItems.isEmpty()) {
                            item(key = "oclax-empty") {
                                Text(
                                    if (allItems.isEmpty()) {
                                        stringResource(R.string.empty_oclax)
                                    } else {
                                        stringResource(R.string.empty_search)
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                        } else {
                            items(visibleItems, key = { it.id }) { item ->
                                ItemCard(
                                    item = item,
                                    selectionMode = selecting,
                                    selected = item.id in selectedItemIds,
                                    onToggleSelection = {
                                        selectedItemIds = if (item.id in selectedItemIds) {
                                            selectedItemIds - item.id
                                        } else if (selectedItemIds.size < MAX_BULK_SELECTION) {
                                            selectedItemIds + item.id
                                        } else {
                                            selectedItemIds
                                        }
                                    },
                                    onPinToggle = onPinToggle,
                                    onDeleteRequest = { pendingDelete = item },
                                    onOpen = onOpen,
                                    onShare = onShare,
                                    onCopy = onCopy,
                                    onRenameRequest = {
                                        pendingRename = item
                                        renameText = item.displayName
                                    },
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
                    Text(stringResource(option.labelRes))
                }
            } else {
                OutlinedButton(
                    onClick = { onSelect(option) },
                    modifier = Modifier.weight(1f).height(38.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                ) {
                    Text(stringResource(option.labelRes))
                }
            }
        }
    }
}

@Composable
private fun contentFilterOverviewItems(items: List<StoredItem>): List<CategoryOverviewItem> =
    ContentFilter.entries.map { filter ->
        val matching = items.filter { matchesFilter(it, filter) }
        CategoryOverviewItem(
            key = filter.name,
            label = stringResource(filter.labelRes),
            icon = contentFilterIcon(filter),
            count = matching.size,
            totalBytes = matching.sumOf { it.byteSize },
            note = when (filter) {
                ContentFilter.ALL -> stringResource(R.string.content_all_title)
                ContentFilter.PINNED -> stringResource(R.string.content_pinned_title)
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
            stringResource(R.string.retention_title),
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
            stringResource(R.string.retention_explainer),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun BulkSelectionBar(
    selectedCount: Int,
    allVisibleSelected: Boolean,
    canAct: Boolean,
    pinValue: Boolean,
    onToggleAll: () -> Unit,
    onShare: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.selected_count, selectedCount),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            TextButton(onClick = onToggleAll) {
                Text(
                    stringResource(
                        if (allVisibleSelected) R.string.select_none else R.string.select_all,
                    ),
                )
            }
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.cancel))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompactActionButton(
                icon = Icons.Outlined.Share,
                description = stringResource(R.string.share_selected),
                enabled = canAct,
                onClick = onShare,
            )
            CompactActionButton(
                icon = if (pinValue) Icons.Outlined.StarBorder else Icons.Filled.Star,
                description = stringResource(
                    if (pinValue) R.string.pin_selected else R.string.unpin_selected,
                ),
                enabled = canAct,
                onClick = onPin,
            )
            CompactActionButton(
                icon = Icons.Outlined.Delete,
                description = stringResource(R.string.delete_selected),
                tint = MaterialTheme.colorScheme.error,
                enabled = canAct,
                onClick = onDelete,
            )
        }
    }
}

@Composable
private fun ItemCard(
    item: StoredItem,
    selectionMode: Boolean,
    selected: Boolean,
    onToggleSelection: () -> Unit,
    onPinToggle: (StoredItem) -> Unit,
    onDeleteRequest: () -> Unit,
    onOpen: (StoredItem) -> Unit,
    onShare: (StoredItem) -> Unit,
    onCopy: (StoredItem) -> Unit,
    onRenameRequest: () -> Unit,
    onLoadThumbnail: (StoredItem, Int) -> Bitmap?,
) {
    val type = contentTypeFor(item.mimeType)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (selectionMode) onToggleSelection() else onOpen(item)
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            when {
                selected -> MaterialTheme.colorScheme.primary
                item.pinned -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outlineVariant
            },
        ),
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selectionMode) {
                    val selectDescription = stringResource(R.string.select_item, item.displayName)
                    Checkbox(
                        checked = selected,
                        onCheckedChange = { onToggleSelection() },
                        modifier = Modifier.semantics {
                            contentDescription = selectDescription
                        },
                    )
                    Spacer(Modifier.width(4.dp))
                }

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
                        stringResource(type.labelRes) + " · " + formatBytes(item.byteSize),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        DateFormat
                            .getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                            .format(Date(item.createdAt)),
                        style = MaterialTheme.typography.labelSmall,
                    )
                    if (item.deleteSourceOnExpiry) {
                        Text(
                            if (item.pinned) {
                                stringResource(R.string.recent_screenshot_pinned)
                            } else {
                                stringResource(R.string.recent_screenshot_cleanup)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            if (!selectionMode) {
                Spacer(Modifier.height(4.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CompactActionButton(
                        icon = Icons.Outlined.Edit,
                        description = stringResource(R.string.rename_item, item.displayName),
                        onClick = onRenameRequest,
                    )

                    CompactActionButton(
                        icon = Icons.Outlined.Share,
                        description = stringResource(R.string.share_item, item.displayName),
                        onClick = { onShare(item) },
                    )

                    if (type.supportsClipboardCopy()) {
                        CompactActionButton(
                            icon = Icons.Outlined.ContentCopy,
                            description = stringResource(R.string.copy_item, item.displayName),
                            onClick = { onCopy(item) },
                        )
                    }

                    CompactActionButton(
                        icon = if (item.pinned) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        description = stringResource(
                            if (item.pinned) R.string.unpin_item else R.string.pin_item,
                            item.displayName,
                        ),
                        onClick = { onPinToggle(item) },
                    )

                    CompactActionButton(
                        icon = Icons.Outlined.Delete,
                        description = stringResource(R.string.delete_item, item.displayName),
                        tint = MaterialTheme.colorScheme.error,
                        onClick = onDeleteRequest,
                    )
                }
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
            contentDescription = stringResource(R.string.thumbnail_of, item.displayName),
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
                    contentDescription = stringResource(type.labelRes),
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
                contentDescription = stringResource(R.string.icon_of, app.label),
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
                    stringResource(R.string.installed_app),
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
    enabled: Boolean = true,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
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

@Composable
private fun retentionCompactLabel(hours: Int): String = stringResource(
    when (hours) {
        1 -> R.string.retention_1h
        24 -> R.string.retention_24h
        72 -> R.string.retention_3d
        168 -> R.string.retention_7d
        0 -> R.string.never
        else -> R.string.retention_24h
    },
)

@Composable
private fun retentionLabel(hours: Int): String = stringResource(
    when (hours) {
        1 -> R.string.retention_1_hour
        24 -> R.string.retention_24_hours
        72 -> R.string.retention_3_days
        168 -> R.string.retention_7_days
        0 -> R.string.never
        else -> R.string.retention_24_hours
    },
)

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.1f MB", mb)
    return String.format("%.2f GB", mb / 1024.0)
}
