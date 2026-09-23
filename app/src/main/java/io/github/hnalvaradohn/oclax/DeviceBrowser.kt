package io.github.hnalvaradohn.oclax

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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image as ImageIcon
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Share
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import io.github.hnalvaradohn.oclax.model.ContentType
import io.github.hnalvaradohn.oclax.model.supportsClipboardCopy
import io.github.hnalvaradohn.oclax.platform.DeviceFileInfo
import io.github.hnalvaradohn.oclax.platform.InstalledAppInfo

private enum class DeviceFilter(val label: String) {
    APPS("Apps"),
    IMAGES("Imágenes"),
    DOCUMENTS("Documentos"),
    PDF("PDF"),
    APK("APK"),
    TEXT("Texto/Código"),
    VIDEO("Video"),
    AUDIO("Audio"),
    OTHER("Otros"),
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
    onOpenApp: (InstalledAppInfo) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(DeviceFilter.APPS) }

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

        DeviceFilterMenu(
            selected = filter,
            onSelect = { filter = it },
        )
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(visibleApps, key = { it.packageName }) { app ->
                        DeviceAppCard(app, onOpenApp)
                    }
                }
            }
            visibleFiles.isEmpty() ->
                Text("No se encontraron ${filter.label.lowercase()} accesibles.")
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    items(visibleFiles, key = { it.uri.toString() }) { file ->
                        DeviceFileCard(
                            file = file,
                            onOpen = onOpenFile,
                            onShare = onShareFile,
                            onCopy = onCopyFile,
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
private fun DeviceAppCard(
    app: InstalledAppInfo,
    onOpen: (InstalledAppInfo) -> Unit,
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
        }
    }
}

@Composable
private fun DeviceFileCard(
    file: DeviceFileInfo,
    onOpen: (DeviceFileInfo) -> Unit,
    onShare: (DeviceFileInfo) -> Unit,
    onCopy: (DeviceFileInfo) -> Unit,
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
            Surface(
                modifier = Modifier.size(36.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = MaterialTheme.shapes.small,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = deviceTypeIcon(file.type),
                        contentDescription = file.type.label,
                        modifier = Modifier.size(21.dp),
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    file.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
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
            IconButton(
                onClick = { onShare(file) },
                modifier = Modifier.size(44.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = "Compartir ${file.displayName}",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(19.dp),
                )
            }
            if (file.type.supportsClipboardCopy()) {
                IconButton(
                    onClick = { onCopy(file) },
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = "Copiar ${file.displayName}",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(19.dp),
                    )
                }
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
