package io.github.hnalvaradohn.oclax

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import io.github.hnalvaradohn.oclax.data.ItemStore
import io.github.hnalvaradohn.oclax.model.ContentType
import io.github.hnalvaradohn.oclax.model.StoredItem
import io.github.hnalvaradohn.oclax.model.contentTypeFor
import io.github.hnalvaradohn.oclax.model.supportsClipboardCopy
import io.github.hnalvaradohn.oclax.platform.InstalledAppInfo
import io.github.hnalvaradohn.oclax.platform.InstalledAppsRepository
import io.github.hnalvaradohn.oclax.ui.theme.OclAxTheme
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    companion object {
        private const val MAX_CLIPBOARD_TEXT_BYTES = 2L * 1024L * 1024L
    }

    private val store by lazy { ItemStore(applicationContext) }
    private val installedAppsRepository by lazy { InstalledAppsRepository(applicationContext) }
    private var items by mutableStateOf<List<StoredItem>>(emptyList())
    private var installedApps by mutableStateOf<List<InstalledAppInfo>>(emptyList())
    private var retentionHours by mutableIntStateOf(24)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OclAxTheme {
                OclAxHome(
                    allItems = items,
                    installedApps = installedApps,
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
                    onShare = ::shareItem,
                    onCopy = ::copyItem,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        retentionHours = store.retentionHours()
        items = store.listItems()
        installedApps = installedAppsRepository.listLaunchableApps()
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
    APK("APK"),
    INSTALLED_APPS("Aplicaciones"),
    TEXT("Texto/Código"),
    VIDEO("Video"),
    AUDIO("Audio"),
    OTHER("Otros"),
}

@Composable
private fun OclAxHome(
    allItems: List<StoredItem>,
    installedApps: List<InstalledAppInfo>,
    retentionHours: Int,
    onRetentionChange: (Int) -> Unit,
    onPinToggle: (StoredItem) -> Unit,
    onDelete: (StoredItem) -> Unit,
    onShare: (StoredItem) -> Unit,
    onCopy: (StoredItem) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(ContentFilter.ALL) }
    var pendingDelete by remember { mutableStateOf<StoredItem?>(null) }

    val visibleItems = remember(query, filter, allItems) {
        val needle = query.trim().lowercase()
        if (filter == ContentFilter.INSTALLED_APPS) {
            emptyList()
        } else {
            allItems.filter { item ->
                matchesFilter(item, filter) &&
                    (
                        needle.isEmpty() ||
                            item.displayName.lowercase().contains(needle) ||
                            item.mimeType.lowercase().contains(needle)
                        )
            }
        }
    }

    val visibleApps = remember(query, filter, installedApps) {
        if (filter != ContentFilter.INSTALLED_APPS) {
            emptyList()
        } else {
            val needle = query.trim().lowercase()
            installedApps.filter { app ->
                needle.isEmpty() ||
                    app.label.lowercase().contains(needle) ||
                    app.packageName.lowercase().contains(needle)
            }
        }
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
            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Buscar") },
            )
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                ContentFilterMenu(
                    selected = filter,
                    onSelect = { filter = it },
                )
                Spacer(Modifier.width(12.dp))
                RetentionControl(
                    retentionHours = retentionHours,
                    onRetentionChange = onRetentionChange,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(10.dp))

            if (filter == ContentFilter.INSTALLED_APPS) {
                if (visibleApps.isEmpty()) {
                    Text(
                        "No hay aplicaciones visibles para esa búsqueda.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(visibleApps, key = { it.packageName }) { app ->
                            InstalledAppCard(app)
                        }
                    }
                }
            } else if (visibleItems.isEmpty()) {
                Text(
                    if (allItems.isEmpty()) {
                        "Todavía no hay elementos. Compartí contenido hacia OclAx."
                    } else {
                        "No hay elementos en ${filter.label.lowercase()} para esa búsqueda."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(visibleItems, key = { it.id }) { item ->
                        ItemCard(
                            item = item,
                            onPinToggle = onPinToggle,
                            onDeleteRequest = { pendingDelete = item },
                            onShare = onShare,
                            onCopy = onCopy,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContentFilterMenu(
    selected: ContentFilter,
    onSelect: (ContentFilter) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Button(
            onClick = { expanded = true },
            modifier = Modifier
                .widthIn(min = 104.dp, max = 132.dp)
                .height(40.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(
                selected.label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
            )
            Spacer(Modifier.width(2.dp))
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = "Abrir categorías",
                modifier = Modifier.size(18.dp),
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            ContentFilter.entries.forEach { option ->
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

private fun matchesFilter(item: StoredItem, filter: ContentFilter): Boolean {
    val type = contentTypeFor(item.mimeType)
    return when (filter) {
        ContentFilter.ALL -> true
        ContentFilter.PINNED -> item.pinned
        ContentFilter.IMAGES -> type == ContentType.IMAGE
        ContentFilter.PDF -> type == ContentType.PDF
        ContentFilter.APK -> type == ContentType.APP
        ContentFilter.INSTALLED_APPS -> false
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
    onShare: (StoredItem) -> Unit,
    onCopy: (StoredItem) -> Unit,
) {
    val type = contentTypeFor(item.mimeType)

    Card(
        modifier = Modifier.fillMaxWidth(),
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
                Surface(
                    modifier = Modifier.size(34.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = contentTypeIcon(type),
                            contentDescription = type.label,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

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
