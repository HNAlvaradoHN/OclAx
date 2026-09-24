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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import io.github.hnalvaradohn.oclax.data.ItemStore
import io.github.hnalvaradohn.oclax.model.ContentType
import io.github.hnalvaradohn.oclax.model.StoredItem
import io.github.hnalvaradohn.oclax.model.contentTypeFor
import io.github.hnalvaradohn.oclax.platform.DeviceContentRepository
import io.github.hnalvaradohn.oclax.platform.DeviceFileInfo
import io.github.hnalvaradohn.oclax.ui.theme.OclAxTheme
import java.util.concurrent.Executors

private data class PickerSelection(
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
            ?.filter { it.isNotBlank() }
            .orEmpty()

        return if (extras.isNotEmpty()) {
            extras
        } else {
            listOf(request.type?.takeIf { it.isNotBlank() } ?: "*/*")
        }
    }

    companion object {
        private const val LEGACY_STORAGE_PERMISSION_REQUEST = 4201
    }
}

private enum class PickerSource(val label: String) {
    OCLAX("OclAx"),
    DEVICE("Mi dispositivo"),
}

@Composable
private fun PickerScreen(
    oclaxItems: List<StoredItem>,
    deviceFiles: List<DeviceFileInfo>,
    requestedMimeTypes: List<String>,
    hasBroadFileAccess: Boolean,
    loading: Boolean,
    allowMultiple: Boolean,
    selected: List<PickerSelection>,
    onCancel: () -> Unit,
    onRequestBroadAccess: () -> Unit,
    onSelectOclAxItem: (StoredItem) -> Unit,
    onSelectDeviceFile: (DeviceFileInfo) -> Unit,
    onConfirmSelection: () -> Unit,
) {
    var source by remember { mutableStateOf(PickerSource.OCLAX) }
    var query by remember { mutableStateOf("") }

    val needle = query.trim().lowercase()
    val visibleOclAxItems = remember(oclaxItems, requestedMimeTypes, needle) {
        oclaxItems.filter { item ->
            PickerMimeMatcher.matches(item.mimeType, requestedMimeTypes) &&
                (
                    needle.isEmpty() ||
                        item.displayName.lowercase().contains(needle) ||
                        item.mimeType.lowercase().contains(needle)
                    )
        }
    }
    val visibleDeviceFiles = remember(deviceFiles, requestedMimeTypes, needle) {
        deviceFiles.filter { file ->
            PickerMimeMatcher.matches(file.mimeType, requestedMimeTypes) &&
                (
                    needle.isEmpty() ||
                        file.displayName.lowercase().contains(needle) ||
                        file.mimeType.lowercase().contains(needle) ||
                        file.relativePath.orEmpty().lowercase().contains(needle)
                    )
        }
    }
    val selectedKeys = remember(selected) { selected.mapTo(mutableSetOf()) { it.key } }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Elegir con OclAx",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        if (allowMultiple) {
                            "Seleccioná uno o varios archivos."
                        } else {
                            "Tocá un archivo para usarlo."
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                TextButton(onClick = onCancel) {
                    Text("Cancelar")
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                SourceButton(
                    label = PickerSource.OCLAX.label,
                    selected = source == PickerSource.OCLAX,
                    onClick = { source = PickerSource.OCLAX },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                SourceButton(
                    label = PickerSource.DEVICE.label,
                    selected = source == PickerSource.DEVICE,
                    onClick = { source = PickerSource.DEVICE },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = {
                    Text(
                        if (source == PickerSource.OCLAX) {
                            "Buscar en OclAx"
                        } else {
                            "Buscar en mi dispositivo"
                        },
                    )
                },
            )

            Spacer(Modifier.height(10.dp))

            when {
                source == PickerSource.DEVICE && !hasBroadFileAccess -> {
                    PickerAccessCard(onRequestBroadAccess)
                }

                loading && source == PickerSource.DEVICE -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Leyendo archivos del dispositivo…")
                    }
                }

                source == PickerSource.OCLAX -> {
                    PickerList(
                        emptyMessage = "No hay contenido compatible en OclAx.",
                        items = visibleOClAxItemsToRows(visibleOclAxItems, onSelectOclAxItem),
                        selectedKeys = selectedKeys,
                        allowMultiple = allowMultiple,
                        modifier = Modifier.weight(1f),
                    )
                }

                else -> {
                    PickerList(
                        emptyMessage = "No hay archivos compatibles en Mi dispositivo.",
                        items = visibleDeviceFilesToRows(visibleDeviceFiles, onSelectDeviceFile),
                        selectedKeys = selectedKeys,
                        allowMultiple = allowMultiple,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (allowMultiple && selected.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onConfirmSelection,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (selected.size == 1) {
                            "Usar 1 archivo"
                        } else {
                            "Usar " + selected.size + " archivos"
                        },
                    )
                }
            }
        }
    }
}

private fun visibleOClAxItemsToRows(
    items: List<StoredItem>,
    onSelect: (StoredItem) -> Unit,
): List<PickerListItem> = items.map { item ->
    PickerListItem(
        key = "oclax:" + item.id,
        displayName = item.displayName,
        mimeType = item.mimeType,
        byteSize = item.byteSize,
        secondary = "OclAx",
        onClick = { onSelect(item) },
    )
}

private fun visibleDeviceFilesToRows(
    files: List<DeviceFileInfo>,
    onSelect: (DeviceFileInfo) -> Unit,
): List<PickerListItem> = files.map { file ->
    PickerListItem(
        key = "device:" + file.uri,
        displayName = file.displayName,
        mimeType = file.mimeType,
        byteSize = file.byteSize,
        secondary = file.relativePath?.takeIf { it.isNotBlank() } ?: "Mi dispositivo",
        onClick = { onSelect(file) },
    )
}

@Composable
private fun SourceButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier.height(46.dp),
        ) {
            Text(label)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(46.dp),
        ) {
            Text(label)
        }
    }
}

@Composable
private fun PickerAccessCard(onRequestBroadAccess: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                "Acceso a Mi dispositivo",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Para elegir archivos reales del teléfono, autorizá el acceso desde Android. " +
                    "La bandeja privada de OclAx sigue disponible sin ese permiso.",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(10.dp))
            Button(onClick = onRequestBroadAccess) {
                Text("Conceder acceso")
            }
        }
    }
}

private data class PickerListItem(
    val key: String,
    val displayName: String,
    val mimeType: String,
    val byteSize: Long,
    val secondary: String,
    val onClick: () -> Unit,
)

@Composable
private fun PickerList(
    emptyMessage: String,
    items: List<PickerListItem>,
    selectedKeys: Set<String>,
    allowMultiple: Boolean,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(emptyMessage, style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 12.dp),
    ) {
        items(items, key = { it.key }) { item ->
            PickerItemCard(
                item = item,
                selected = item.key in selectedKeys,
                showSelection = allowMultiple,
            )
        }
    }
}

@Composable
private fun PickerItemCard(
    item: PickerListItem,
    selected: Boolean,
    showSelection: Boolean,
) {
    val type = contentTypeFor(item.mimeType)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = item.onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        border = BorderStroke(
            1.dp,
            if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = MaterialTheme.shapes.small,
            ) {
                Icon(
                    imageVector = pickerContentTypeIcon(type),
                    contentDescription = type.label,
                    modifier = Modifier.padding(9.dp),
                )
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                )
                Text(
                    type.label + " · " + formatPickerBytes(item.byteSize),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    item.secondary,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            }

            if (showSelection && selected) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Seleccionado",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private fun pickerContentTypeIcon(type: ContentType): ImageVector = when (type) {
    ContentType.IMAGE -> Icons.Outlined.Image
    ContentType.PDF -> Icons.Outlined.PictureAsPdf
    ContentType.APP -> Icons.Outlined.Android
    ContentType.DOCUMENT -> Icons.Outlined.Description
    ContentType.TEXT -> Icons.Outlined.Code
    ContentType.VIDEO -> Icons.Outlined.Movie
    ContentType.AUDIO -> Icons.Outlined.AudioFile
    ContentType.OTHER -> Icons.Outlined.InsertDriveFile
}

private fun formatPickerBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.1f MB", mb)
    return String.format("%.2f GB", mb / 1024.0)
}
