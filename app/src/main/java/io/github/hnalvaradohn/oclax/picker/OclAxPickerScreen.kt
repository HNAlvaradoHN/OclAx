package io.github.hnalvaradohn.oclax.picker

import android.graphics.Bitmap
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image as ImageIcon
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.hnalvaradohn.oclax.model.ContentType
import io.github.hnalvaradohn.oclax.model.StoredItem
import io.github.hnalvaradohn.oclax.model.contentTypeFor
import io.github.hnalvaradohn.oclax.platform.DeviceFileInfo
import io.github.hnalvaradohn.oclax.ui.CategoryOverviewGrid
import io.github.hnalvaradohn.oclax.ui.CategoryOverviewItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

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
    onLoadOclAxThumbnail: (StoredItem, Int) -> Bitmap?,
    onLoadDeviceThumbnail: (DeviceFileInfo, Int) -> Bitmap?,
    onConfirmSelection: () -> Unit,
) {
    var source by remember { mutableStateOf(PickerSource.OCLAX) }
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<PickerCategory?>(null) }

    val compatibleOclAxItems = remember(oclaxItems, requestedMimeTypes) {
        oclaxItems.filter { item ->
            PickerMimeMatcher.matches(item.mimeType, requestedMimeTypes)
        }
    }
    val compatibleDeviceFiles = remember(deviceFiles, requestedMimeTypes) {
        deviceFiles.filter { file ->
            PickerMimeMatcher.matches(file.mimeType, requestedMimeTypes)
        }
    }

    val needle = query.trim().lowercase()
    val visibleOclAxItems = remember(compatibleOclAxItems, needle, category) {
        compatibleOclAxItems.filter { item ->
            (category == null || requireNotNull(category).matches(item.mimeType, item.pinned)) &&
                (
                    needle.isEmpty() ||
                        item.displayName.lowercase().contains(needle) ||
                        item.mimeType.lowercase().contains(needle)
                    )
        }
    }
    val visibleDeviceFiles = remember(compatibleDeviceFiles, needle, category) {
        compatibleDeviceFiles.filter { file ->
            (category == null || requireNotNull(category).matches(file.mimeType)) &&
                (
                    needle.isEmpty() ||
                        file.displayName.lowercase().contains(needle) ||
                        file.mimeType.lowercase().contains(needle) ||
                        file.relativePath.orEmpty().lowercase().contains(needle)
                    )
        }
    }
    val categoryOverview = remember(
        source,
        compatibleOclAxItems,
        compatibleDeviceFiles,
    ) {
        pickerCategoryOverviewItems(
            source = source,
            oclaxItems = compatibleOclAxItems,
            deviceFiles = compatibleDeviceFiles,
        )
    }
    val selectedKeys = remember(selected) { selected.mapTo(mutableSetOf()) { it.key } }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "OclAx",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        if (allowMultiple) {
                            "Elegí uno o varios archivos."
                        } else {
                            "Elegí un archivo para continuar."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                TextButton(onClick = onCancel) {
                    Text("Cancelar")
                }
            }

            Spacer(Modifier.height(12.dp))

            PickerSourceSwitch(
                selected = source,
                onSelect = { newSource ->
                    source = newSource
                    query = ""
                    category = null
                },
            )

            Spacer(Modifier.height(12.dp))

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

                query.isBlank() && category == null -> {
                    CategoryOverviewGrid(
                        items = categoryOverview,
                        onSelect = { selectedCategory ->
                            category = PickerCategory.entries
                                .firstOrNull { it.name == selectedCategory.key }
                        },
                        modifier = Modifier.weight(1f),
                        scrollable = true,
                    )
                }

                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = {
                                category = null
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
                            category?.label ?: "Resultados",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(Modifier.height(6.dp))

                    if (source == PickerSource.OCLAX) {
                        PickerList(
                            emptyMessage = "No se encontró contenido compatible.",
                            items = oclaxItemsToRows(
                                items = visibleOclAxItems,
                                onSelect = onSelectOclAxItem,
                                onLoadThumbnail = onLoadOclAxThumbnail,
                            ),
                            selectedKeys = selectedKeys,
                            allowMultiple = allowMultiple,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        PickerList(
                            emptyMessage = "No se encontraron archivos compatibles.",
                            items = deviceFilesToRows(
                                files = visibleDeviceFiles,
                                onSelect = onSelectDeviceFile,
                                onLoadThumbnail = onLoadDeviceThumbnail,
                            ),
                            selectedKeys = selectedKeys,
                            allowMultiple = allowMultiple,
                            modifier = Modifier.weight(1f),
                        )
                    }
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
                            "Usar ${selected.size} archivos"
                        },
                    )
                }
            }
        }
    }
}

private fun pickerCategoryOverviewItems(
    source: PickerSource,
    oclaxItems: List<StoredItem>,
    deviceFiles: List<DeviceFileInfo>,
): List<CategoryOverviewItem> {
    val categories = if (source == PickerSource.OCLAX) {
        PickerCategory.entries
    } else {
        PickerCategory.entries.filterNot { it == PickerCategory.PINNED }
    }

    return categories.map { category ->
        if (source == PickerSource.OCLAX) {
            val matching = oclaxItems.filter { category.matches(it.mimeType, it.pinned) }
            CategoryOverviewItem(
                key = category.name,
                label = category.label,
                icon = pickerCategoryIcon(category),
                count = matching.size,
                totalBytes = matching.sumOf { it.byteSize },
                note = if (category == PickerCategory.PINNED) {
                    "Guardados sin autolimpieza"
                } else {
                    null
                },
            )
        } else {
            val matching = deviceFiles.filter { category.matches(it.mimeType) }
            CategoryOverviewItem(
                key = category.name,
                label = category.label,
                icon = pickerCategoryIcon(category),
                count = matching.size,
                totalBytes = matching.sumOf { it.byteSize },
            )
        }
    }
}

private fun pickerCategoryIcon(category: PickerCategory): ImageVector = when (category) {
    PickerCategory.RECENT -> Icons.Outlined.InsertDriveFile
    PickerCategory.PINNED -> Icons.Filled.Star
    PickerCategory.IMAGES -> Icons.Outlined.ImageIcon
    PickerCategory.DOCUMENTS -> Icons.Outlined.Description
    PickerCategory.PDF -> Icons.Outlined.PictureAsPdf
    PickerCategory.APK -> Icons.Outlined.Android
    PickerCategory.TEXT -> Icons.Outlined.Code
    PickerCategory.VIDEO -> Icons.Outlined.Movie
    PickerCategory.AUDIO -> Icons.Outlined.AudioFile
    PickerCategory.OTHER -> Icons.Outlined.InsertDriveFile
}

@Composable
private fun PickerSourceSwitch(
    selected: PickerSource,
    onSelect: (PickerSource) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PickerSource.entries.forEach { option ->
            if (selected == option) {
                Button(
                    onClick = { onSelect(option) },
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                ) {
                    Text(option.label)
                }
            } else {
                OutlinedButton(
                    onClick = { onSelect(option) },
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                ) {
                    Text(option.label)
                }
            }
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
    val details: List<String>,
    val thumbnailVersion: Long,
    val onClick: () -> Unit,
    val onLoadThumbnail: (Int) -> Bitmap?,
)

private fun oclaxItemsToRows(
    items: List<StoredItem>,
    onSelect: (StoredItem) -> Unit,
    onLoadThumbnail: (StoredItem, Int) -> Bitmap?,
): List<PickerListItem> = items.map { item ->
    PickerListItem(
        key = "oclax:" + item.id,
        displayName = item.displayName,
        mimeType = item.mimeType,
        byteSize = item.byteSize,
        details = listOf(
            DateFormat
                .getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                .format(Date(item.createdAt)),
        ),
        thumbnailVersion = item.createdAt,
        onClick = { onSelect(item) },
        onLoadThumbnail = { targetPx -> onLoadThumbnail(item, targetPx) },
    )
}

private fun deviceFilesToRows(
    files: List<DeviceFileInfo>,
    onSelect: (DeviceFileInfo) -> Unit,
    onLoadThumbnail: (DeviceFileInfo, Int) -> Bitmap?,
): List<PickerListItem> = files.map { file ->
    PickerListItem(
        key = "device:" + file.uri,
        displayName = file.displayName,
        mimeType = file.mimeType,
        byteSize = file.byteSize,
        details = buildList {
            formatPickerModifiedAt(file.modifiedAt)?.let { add("Modificado · $it") }
            file.relativePath?.takeIf { it.isNotBlank() }?.let(::add)
        },
        thumbnailVersion = file.modifiedAt,
        onClick = { onSelect(file) },
        onLoadThumbnail = { targetPx -> onLoadThumbnail(file, targetPx) },
    )
}

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
        verticalArrangement = Arrangement.spacedBy(7.dp),
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
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PickerItemVisual(
                item = item,
                type = type,
            )

            Spacer(Modifier.width(8.dp))

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
                item.details.forEach { detail ->
                    Text(
                        detail,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                }
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

@Composable
private fun PickerItemVisual(
    item: PickerListItem,
    type: ContentType,
) {
    val supportsThumbnail =
        type == ContentType.IMAGE || type == ContentType.VIDEO || type == ContentType.PDF
    val targetPx = if (supportsThumbnail) 160 else 96
    var thumbnail by remember(item.key, item.thumbnailVersion, targetPx) {
        mutableStateOf<Bitmap?>(null)
    }

    LaunchedEffect(item.key, item.thumbnailVersion, targetPx) {
        thumbnail = if (supportsThumbnail) {
            withContext(Dispatchers.IO) {
                item.onLoadThumbnail(targetPx)
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
                .size(48.dp)
                .clip(MaterialTheme.shapes.small),
            contentScale = ContentScale.Crop,
        )
    } else {
        Surface(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = MaterialTheme.shapes.small,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = pickerContentTypeIcon(type),
                    contentDescription = type.label,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
    }
}

private fun pickerContentTypeIcon(type: ContentType): ImageVector = when (type) {
    ContentType.IMAGE -> Icons.Outlined.ImageIcon
    ContentType.PDF -> Icons.Outlined.PictureAsPdf
    ContentType.APP -> Icons.Outlined.Android
    ContentType.DOCUMENT -> Icons.Outlined.Description
    ContentType.TEXT -> Icons.Outlined.Code
    ContentType.VIDEO -> Icons.Outlined.Movie
    ContentType.AUDIO -> Icons.Outlined.AudioFile
    ContentType.OTHER -> Icons.Outlined.InsertDriveFile
}

private fun formatPickerModifiedAt(modifiedAt: Long): String? =
    modifiedAt
        .takeIf { it > 0L }
        ?.let { timestamp ->
            DateFormat
                .getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                .format(Date(timestamp))
        }

private fun formatPickerBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.1f MB", mb)
    return String.format("%.2f GB", mb / 1024.0)
}
