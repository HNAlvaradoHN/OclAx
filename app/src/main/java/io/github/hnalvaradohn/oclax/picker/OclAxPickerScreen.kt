package io.github.hnalvaradohn.oclax.picker

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
import io.github.hnalvaradohn.oclax.model.ContentType
import io.github.hnalvaradohn.oclax.model.StoredItem
import io.github.hnalvaradohn.oclax.model.contentTypeFor
import io.github.hnalvaradohn.oclax.platform.DeviceFileInfo

private enum class PickerSource(val label: String) {
    OCLAX("OclAx"),
    DEVICE("Mi dispositivo"),
}

@Composable
internal fun PickerScreen(
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
                    onClick = {\n                        source = PickerSource.OCLAX\n                        query = \"\"\n                    },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                SourceButton(
                    label = PickerSource.DEVICE.label,
                    selected = source == PickerSource.DEVICE,
                    onClick = {\n                        source = PickerSource.DEVICE\n                        query = \"\"\n                    },
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
                        items = oclaxItemsToRows(visibleOclAxItems, onSelectOclAxItem),
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

private fun oclaxItemsToRows(
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
