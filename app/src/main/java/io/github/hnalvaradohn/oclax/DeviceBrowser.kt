package io.github.hnalvaradohn.oclax

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Image as ImageIcon
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.core.graphics.drawable.toBitmap
import io.github.hnalvaradohn.oclax.data.ContentViewMode
import io.github.hnalvaradohn.oclax.model.ContentType
import io.github.hnalvaradohn.oclax.model.supportsClipboardCopy
import io.github.hnalvaradohn.oclax.platform.DeviceFileInfo
import io.github.hnalvaradohn.oclax.platform.InstalledAppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class DeviceFilter(
    val key: String,
    val label: String,
    val defaultViewMode: ContentViewMode,
) {
    APPS("apps", "Apps", ContentViewMode.GRID),
    IMAGES("images", "Imágenes", ContentViewMode.GRID),
    DOCUMENTS("documents", "Documentos", ContentViewMode.LIST),
    PDF("pdf", "PDF", ContentViewMode.LIST),
    APK("apk", "APK", ContentViewMode.LIST),
    TEXT("text", "Texto/Código", ContentViewMode.LIST),
    VIDEO("video", "Video", ContentViewMode.GRID),
    AUDIO("audio", "Audio", ContentViewMode.LIST),
    OTHER("other", "Otros", ContentViewMode.LIST),
}

@Composable
fun DeviceBrowser(
    files: List<DeviceFileInfo>,
    apps: List<InstalledAppInfo>,
    hasBroadFileAccess: Boolean,
    isLoading: Boolean,
    onRequestBroadAccess: () -> Unit,
    onOpenFile: (DeviceFileInfo) -> Unit,
    onShareFile: (DeviceFileInfo) -> Unit,
    onCopyFile: (DeviceFileInfo) -> Unit,
    onDeleteFile: (DeviceFileInfo) -> Unit,
    onOpenApp: (InstalledAppInfo) -> Unit,
    onShareApp: (InstalledAppInfo) -> Unit,
    onLoadThumbnail: (DeviceFileInfo, Int) -> Bitmap?,
    onLoadViewMode: (String, ContentViewMode) -> ContentViewMode,
    onSaveViewMode: (String, ContentViewMode) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(DeviceFilter.APPS) }
    var pendingDelete by remember { mutableStateOf<DeviceFileInfo?>(null) }
    var viewMode by remember(filter) {
        mutableStateOf(onLoadViewMode(filter.key, filter.defaultViewMode))
    }

    pendingDelete?.let { file ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("¿Eliminar del dispositivo?") },
            text = {
                Text(
                    "Se eliminará el archivo original “${file.displayName}”. " +
                        "Esta acción no es la autolimpieza de OclAx y puede no tener deshacer.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        onDeleteFile(file)
                    },
                ) {
                    Text("Eliminar original", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancelar")
                }
            },
        )
    }

    val needle = query.trim().lowercase()
    val visibleApps = remember(apps, needle, filter) {
        if (filter != DeviceFilter.APPS) {
            emptyList()
        } else {
            apps.filter {
                needle.isEmpty() ||
                    it.label.lowercase().contains(needle) ||
                    it.packageName.lowercase().contains(needle)
            }
        }
    }

    val visibleFiles = remember(files, needle, filter) {
        if (filter == DeviceFilter.APPS) {
            emptyList()
        } else {
            files.filter { file ->
                matchesDeviceFilter(file, filter) &&
                    (
                        needle.isEmpty() ||
                            file.displayName.lowercase().contains(needle) ||
                            file.mimeType.lowercase().contains(needle) ||
                            file.relativePath.orEmpty().lowercase().contains(needle)
                        )
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Buscar en mi dispositivo") },
        )
        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DeviceFilterMenu(
                selected = filter,
                onSelect = { filter = it },
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    viewMode = if (viewMode == ContentViewMode.LIST) {
                        ContentViewMode.GRID
                    } else {
                        ContentViewMode.LIST
                    }
                    onSaveViewMode(filter.key, viewMode)
                },
            ) {
                Icon(
                    imageVector = if (viewMode == ContentViewMode.LIST) {
                        Icons.Outlined.GridView
                    } else {
                        Icons.Outlined.ViewList
                    },
                    contentDescription = if (viewMode == ContentViewMode.LIST) {
                        "Ver ${filter.label} en cuadrícula"
                    } else {
                        "Ver ${filter.label} como lista"
                    },
                )
            }
            Text(
                if (viewMode == ContentViewMode.LIST) "Lista" else "Cuadrícula",
                style = MaterialTheme.typography.labelMedium,
            )
        }
        Spacer(Modifier.height(10.dp))

        if (filter != DeviceFilter.APPS && !hasBroadFileAccess) {
            AccessRequiredCard(onRequestBroadAccess)
            return@Column
        }

        when {
            isLoading -> Text("Leyendo contenido del dispositivo…")
            filter == DeviceFilter.APPS && visibleApps.isEmpty() ->
                Text("No se encontraron aplicaciones para esa búsqueda.")
            filter == DeviceFilter.APPS -> {
                if (viewMode == ContentViewMode.GRID) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(148.dp),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        gridItems(visibleApps, key = { it.packageName }) { app ->
                            DeviceAppGridCard(app, onOpenApp, onShareApp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(visibleApps, key = { it.packageName }) { app ->
                            DeviceAppListCard(app, onOpenApp, onShareApp)
                        }
                    }
                }
            }
            visibleFiles.isEmpty() ->
                Text("No se encontraron ${filter.label.lowercase()} accesibles.")
            viewMode == ContentViewMode.GRID -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(148.dp),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    gridItems(visibleFiles, key = { it.uri.toString() }) { file ->
                        DeviceFileGridCard(
                            file = file,
                            onOpen = onOpenFile,
                            onShare = onShareFile,
                            onCopy = onCopyFile,
                            onDeleteRequest = { pendingDelete = file },
                            onLoadThumbnail = onLoadThumbnail,
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    items(visibleFiles, key = { it.uri.toString() }) { file ->
                        DeviceFileListCard(
                            file = file,
                            onOpen = onOpenFile,
                            onShare = onShareFile,
                            onCopy = onCopyFile,
                            onDeleteRequest = { pendingDelete = file },
                            onLoadThumbnail = onLoadThumbnail,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccessRequiredCard(onRequestBroadAccess: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                "Acceso a archivos necesario",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Para mostrar imágenes, videos, documentos, PDF, APK y otros archivos reales " +
                    "del teléfono, Android necesita que autoricés a OclAx a administrar archivos.",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(10.dp))
            Button(onClick = onRequestBroadAccess) {
                Text("Conceder acceso")
            }
        }
    }
}

@Composable
private fun DeviceFilterMenu(
    selected: DeviceFilter,
    onSelect: (DeviceFilter) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Button(
            onClick = { expanded = true },
            modifier = Modifier
                .widthIn(min = 104.dp, max = 150.dp)
                .height(40.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(selected.label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
            Spacer(Modifier.width(2.dp))
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = "Abrir categorías del dispositivo",
                modifier = Modifier.size(18.dp),
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DeviceFilter.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun DeviceAppListCard(
    app: InstalledAppInfo,
    onOpen: (InstalledAppInfo) -> Unit,
    onShare: (InstalledAppInfo) -> Unit,
) {
    val bitmap = remember(app.packageName) {
        app.icon.toBitmap(width = 48, height = 48).asImageBitmap()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen(app) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
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
                    if (app.isSystemApp) "Aplicación del sistema" else "Aplicación instalada",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            IconButton(onClick = { onShare(app) }) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = "Compartir ${app.label}",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun DeviceAppGridCard(
    app: InstalledAppInfo,
    onOpen: (InstalledAppInfo) -> Unit,
    onShare: (InstalledAppInfo) -> Unit,
) {
    val bitmap = remember(app.packageName) {
        app.icon.toBitmap(width = 96, height = 96).asImageBitmap()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen(app) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                bitmap = bitmap,
                contentDescription = "Icono de ${app.label}",
                modifier = Modifier.size(58.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                app.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
            )
            Text(
                if (app.isSystemApp) "Sistema" else "Instalada",
                style = MaterialTheme.typography.labelSmall,
            )
            IconButton(onClick = { onShare(app) }) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = "Compartir ${app.label}",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun DeviceFileListCard(
    file: DeviceFileInfo,
    onOpen: (DeviceFileInfo) -> Unit,
    onShare: (DeviceFileInfo) -> Unit,
    onCopy: (DeviceFileInfo) -> Unit,
    onDeleteRequest: () -> Unit,
    onLoadThumbnail: (DeviceFileInfo, Int) -> Bitmap?,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen(file) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DeviceFileVisual(
                file = file,
                sizePx = 160,
                modifier = Modifier.size(48.dp),
                onLoadThumbnail = onLoadThumbnail,
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    file.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                )
                Text(
                    file.type.label + " · " + formatDeviceBytes(file.byteSize),
                    style = MaterialTheme.typography.bodySmall,
                )
                if (!file.relativePath.isNullOrBlank()) {
                    Text(
                        file.relativePath,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                }
            }
            DeviceFileActions(file, onShare, onCopy, onDeleteRequest)
        }
    }
}

@Composable
private fun DeviceFileGridCard(
    file: DeviceFileInfo,
    onOpen: (DeviceFileInfo) -> Unit,
    onShare: (DeviceFileInfo) -> Unit,
    onCopy: (DeviceFileInfo) -> Unit,
    onDeleteRequest: () -> Unit,
    onLoadThumbnail: (DeviceFileInfo, Int) -> Bitmap?,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen(file) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            DeviceFileVisual(
                file = file,
                sizePx = 420,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp),
                onLoadThumbnail = onLoadThumbnail,
            )
            Spacer(Modifier.height(7.dp))
            Text(
                file.displayName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
            )
            Text(
                formatDeviceBytes(file.byteSize),
                style = MaterialTheme.typography.labelSmall,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                DeviceFileActions(file, onShare, onCopy, onDeleteRequest)
            }
        }
    }
}

@Composable
private fun DeviceFileActions(
    file: DeviceFileInfo,
    onShare: (DeviceFileInfo) -> Unit,
    onCopy: (DeviceFileInfo) -> Unit,
    onDeleteRequest: () -> Unit,
) {
    IconButton(
        onClick = { onShare(file) },
        modifier = Modifier.size(40.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Share,
            contentDescription = "Compartir ${file.displayName}",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
    }
    if (file.type.supportsClipboardCopy()) {
        IconButton(
            onClick = { onCopy(file) },
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.ContentCopy,
                contentDescription = "Copiar ${file.displayName}",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
    IconButton(
        onClick = onDeleteRequest,
        modifier = Modifier.size(40.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Delete,
            contentDescription = "Eliminar original ${file.displayName}",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun DeviceFileVisual(
    file: DeviceFileInfo,
    sizePx: Int,
    modifier: Modifier,
    onLoadThumbnail: (DeviceFileInfo, Int) -> Bitmap?,
) {
    var thumbnail by remember(file.uri, file.modifiedAt, sizePx) {
        mutableStateOf<Bitmap?>(null)
    }

    LaunchedEffect(file.uri, file.modifiedAt, sizePx) {
        thumbnail = if (
            file.type == ContentType.IMAGE ||
            file.type == ContentType.VIDEO ||
            file.type == ContentType.PDF
        ) {
            withContext(Dispatchers.IO) {
                onLoadThumbnail(file, sizePx)
            }
        } else {
            null
        }
    }

    val bitmap = thumbnail
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Miniatura de ${file.displayName}",
            modifier = modifier.clip(MaterialTheme.shapes.small),
            contentScale = ContentScale.Crop,
        )
    } else {
        Surface(
            modifier = modifier,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = MaterialTheme.shapes.small,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = deviceTypeIcon(file.type),
                    contentDescription = file.type.label,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}

private fun matchesDeviceFilter(file: DeviceFileInfo, filter: DeviceFilter): Boolean =
    when (filter) {
        DeviceFilter.APPS -> false
        DeviceFilter.IMAGES -> file.type == ContentType.IMAGE
        DeviceFilter.DOCUMENTS -> file.type == ContentType.DOCUMENT
        DeviceFilter.PDF -> file.type == ContentType.PDF
        DeviceFilter.APK -> file.type == ContentType.APP
        DeviceFilter.TEXT -> file.type == ContentType.TEXT
        DeviceFilter.VIDEO -> file.type == ContentType.VIDEO
        DeviceFilter.AUDIO -> file.type == ContentType.AUDIO
        DeviceFilter.OTHER -> file.type == ContentType.OTHER
    }

private fun deviceTypeIcon(type: ContentType): ImageVector = when (type) {
    ContentType.IMAGE -> Icons.Outlined.ImageIcon
    ContentType.PDF -> Icons.Outlined.PictureAsPdf
    ContentType.APP -> Icons.Outlined.Android
    ContentType.DOCUMENT -> Icons.Outlined.Description
    ContentType.TEXT -> Icons.Outlined.Code
    ContentType.VIDEO -> Icons.Outlined.Movie
    ContentType.AUDIO -> Icons.Outlined.AudioFile
    ContentType.OTHER -> Icons.Outlined.InsertDriveFile
}

private fun formatDeviceBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.1f MB", mb)
    return String.format("%.2f GB", mb / 1024.0)
}
