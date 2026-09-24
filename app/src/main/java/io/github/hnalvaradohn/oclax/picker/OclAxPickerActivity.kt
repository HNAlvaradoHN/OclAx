package io.github.hnalvaradohn.oclax.picker

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import io.github.hnalvaradohn.oclax.data.ItemStore
import io.github.hnalvaradohn.oclax.model.StoredItem
import io.github.hnalvaradohn.oclax.platform.DeviceContentRepository
import io.github.hnalvaradohn.oclax.platform.DeviceFileInfo
import io.github.hnalvaradohn.oclax.ui.theme.OclAxTheme
import java.util.concurrent.Executors

internal data class PickerSelection(
    val key: String,
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
)

class OclAxPickerActivity : ComponentActivity() {
    private val store by lazy { ItemStore(applicationContext) }
    private val deviceContentRepository by lazy { DeviceContentRepository(applicationContext) }
    private val executor = Executors.newSingleThreadExecutor()

    private var oclaxItems by mutableStateOf<List<StoredItem>>(emptyList())
    private var deviceFiles by mutableStateOf<List<DeviceFileInfo>>(emptyList())
    private var hasBroadFileAccess by mutableStateOf(false)
    private var loading by mutableStateOf(false)
    private var selected by mutableStateOf<List<PickerSelection>>(emptyList())

    private var requestedMimeTypes: List<String> = listOf("*/*")
    private var allowMultiple: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent.action != Intent.ACTION_GET_CONTENT) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        requestedMimeTypes = resolveRequestedMimeTypes(intent)
        allowMultiple = intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)

        setContent {
            OclAxTheme {
                PickerScreen(
                    oclaxItems = oclaxItems,
                    deviceFiles = deviceFiles,
                    requestedMimeTypes = requestedMimeTypes,
                    hasBroadFileAccess = hasBroadFileAccess,
                    loading = loading,
                    allowMultiple = allowMultiple,
                    selected = selected,
                    onCancel = {
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    },
                    onRequestBroadAccess = ::requestBroadFileAccess,
                    onSelectOclAxItem = ::selectOclAxItem,
                    onSelectDeviceFile = ::selectDeviceFile,
                    onConfirmSelection = { finishWithSelections(selected) },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshContent()
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LEGACY_STORAGE_PERMISSION_REQUEST) {
            refreshContent()
        }
    }

    private fun refreshContent() {
        hasBroadFileAccess = deviceContentRepository.hasBroadFileAccess()
        loading = true

        executor.execute {
            val localItems = runCatching { store.listItems() }.getOrDefault(emptyList())
            val files = if (deviceContentRepository.hasBroadFileAccess()) {
                runCatching { deviceContentRepository.listFiles() }.getOrDefault(emptyList())
            } else {
                emptyList()
            }

            if (isDestroyed) return@execute
            runOnUiThread {
                oclaxItems = localItems
                deviceFiles = files
                hasBroadFileAccess = deviceContentRepository.hasBroadFileAccess()
                loading = false
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
            return
        }

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

        if (permissions.isEmpty()) {
            refreshContent()
        } else {
            requestPermissions(
                permissions.toTypedArray(),
                LEGACY_STORAGE_PERMISSION_REQUEST,
            )
        }
    }

    private fun selectOclAxItem(item: StoredItem) {
        val selection = runCatching {
            val file = store.payloadFile(item)
            PickerSelection(
                key = "oclax:" + item.id,
                uri = FileProvider.getUriForFile(
                    this,
                    "$packageName.fileprovider",
                    file,
                ),
                displayName = item.displayName,
                mimeType = item.mimeType,
            )
        }.getOrElse { error ->
            Toast.makeText(
                this,
                "No se pudo seleccionar: " + (error.message ?: "error desconocido"),
                Toast.LENGTH_SHORT,
            ).show()
            return
        }

        select(selection)
    }

    private fun selectDeviceFile(file: DeviceFileInfo) {
        select(
            PickerSelection(
                key = "device:" + file.uri,
                uri = file.uri,
                displayName = file.displayName,
                mimeType = file.mimeType,
            ),
        )
    }

    private fun select(selection: PickerSelection) {
        if (!allowMultiple) {
            finishWithSelections(listOf(selection))
            return
        }

        selected = if (selected.any { it.key == selection.key }) {
            selected.filterNot { it.key == selection.key }
        } else {
            if (selected.size >= MAX_SELECTIONS) {
                Toast.makeText(
                    this,
                    "Podés seleccionar hasta $MAX_SELECTIONS archivos por vez.",
                    Toast.LENGTH_SHORT,
                ).show()
                return
            }
            selected + selection
        }
    }

    private fun finishWithSelections(selections: List<PickerSelection>) {
        if (selections.isEmpty()) return

        val first = selections.first()
        val clip = ClipData.newUri(
            contentResolver,
            first.displayName,
            first.uri,
        )
        selections.drop(1).forEach { item ->
            clip.addItem(ClipData.Item(item.uri))
        }

        val result = Intent().apply {
            if (selections.size == 1) {
                data = first.uri
            }
            type = resultMimeType(selections)
            clipData = clip
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        setResult(Activity.RESULT_OK, result)
        finish()
    }

    private fun resultMimeType(selections: List<PickerSelection>): String {
        val distinct = selections
            .map { it.mimeType.lowercase() }
            .distinct()

        return if (distinct.size == 1) distinct.first() else "*/*"
    }

    private fun resolveRequestedMimeTypes(request: Intent): List<String> {
        val extras = request.getStringArrayExtra(Intent.EXTRA_MIME_TYPES)
            ?.asSequence()
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() && it.length <= MAX_MIME_LENGTH }
            ?.take(MAX_REQUESTED_MIME_TYPES)
            ?.toList()
            .orEmpty()

        return if (extras.isNotEmpty()) {
            extras
        } else {
            listOf(
                request.type
                    ?.trim()
                    ?.takeIf { it.isNotBlank() && it.length <= MAX_MIME_LENGTH }
                    ?: "*/*",
            )
        }
    }

    companion object {
        private const val LEGACY_STORAGE_PERMISSION_REQUEST = 4201
        private const val MAX_REQUESTED_MIME_TYPES = 32
        private const val MAX_MIME_LENGTH = 127
        private const val MAX_SELECTIONS = 100
    }
}
