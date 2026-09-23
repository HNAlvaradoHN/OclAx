package io.github.hnalvaradohn.oclax

import android.content.ClipData
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Delete
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import io.github.hnalvaradohn.oclax.data.ItemStore
import io.github.hnalvaradohn.oclax.model.ContentType
import io.github.hnalvaradohn.oclax.model.StoredItem
import io.github.hnalvaradohn.oclax.model.contentTypeFor
import io.github.hnalvaradohn.oclax.ui.theme.OclAxTheme
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    private val store by lazy { ItemStore(applicationContext) }
    private var items by mutableStateOf<List<StoredItem>>(emptyList())
    private var retentionHours by mutableIntStateOf(24)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OclAxTheme {
                OclAxHome(
                    allItems = items,
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
                            Toast.makeText(this, "No se pudo eliminar la copia de OclAx.", Toast.LENGTH_SHORT).show()
                        }
                        refresh()
                    },
                    onShare = ::shareItem,
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
}

private enum class ContentFilter(val label: String) {
    ALL("Todo"),
    PINNED("Fijados"),
    IMAGES("Imágenes"),
    DOCUMENTS("Documentos"),
    PDF("PDF"),
    APPS("Apps/APK"),
    TEXT("Texto/Código"),
    VIDEO("Video"),
    AUDIO("Audio"),
    OTHER("Otros"),
}

@Composable
private fun OclAxHome(
    allItems: List<StoredItem>,
    retentionHours: Int,
    onRetentionChange: (Int) -> Unit,
    onPinToggle: (StoredItem) -> Unit,
    onDelete: (StoredItem) -> Unit,
    onShare: (StoredItem) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(ContentFilter.ALL) }
    var pendingDelete by remember { mutableStateOf<StoredItem?>(null) }

    val visibleItems = remember(query, filter, allItems) {
        val needle = query.trim().lowercase()
        allItems.filter { item ->
            matchesFilter(item, filter) &&
                (
                    needle.isEmpty() ||
                        item.displayName.lowercase().contains(needle) ||
                        item.mimeType.lowercase().contains(needle)
                    )
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
                .padding(start = 20.dp, top = 20.dp, end = 12.dp, bottom = 20.dp),
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
            Spacer(Modifier.height(18.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Buscar") },
            )
            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(end = 10.dp),
                ) {
                    RetentionControl(retentionHours, onRetentionChange)
                    Spacer(Modifier.height(12.dp))

                    if (visibleItems.isEmpty()) {
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
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(visibleItems, key = { it.id }) { item ->
                                ItemCard(
                                    item = item,
                                    onPinToggle = onPinToggle,
                                    onDeleteRequest = { pendingDelete = item },
                                    onShare = onShare,
                                )
                            }
                        }
                    }
                }

                ContentFilterRail(
                    selected = filter,
                    onSelect = { filter = it },
                    modifier = Modifier
                        .width(112.dp)
                        .fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun ContentFilterRail(
    selected: ContentFilter,
    onSelect: (ContentFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ContentFilter.entries.forEach { option ->
            if (selected == option) {
                Button(
                    onClick = { onSelect(option) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text(
                        option.label,
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                    )
                }
            } else {
                OutlinedButton(
                    onClick = { onSelect(option) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    Text(
                        option.label,
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                    )
                }
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
        ContentFilter.APPS -> type == ContentType.APP
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
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(
            "Borrar copias de OclAx después de",
            style = MaterialTheme.typography.labelMedium,
        )
        OutlinedButton(
            onClick = { expanded = true },
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Text(retentionLabel(retentionHours))
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
            "Solo elimina copias internas de OclAx; nunca el archivo original del dispositivo.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun ItemCard(
    item: StoredItem,
    onPinToggle: (StoredItem) -> Unit,
    onDeleteRequest: () -> Unit,
    onShare: (StoredItem) -> Unit,
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
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        type.glyph,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.displayName,
                        style = MaterialTheme.typography.bodyLarge,
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
                IconButton(onClick = { onShare(item) }) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Compartir ${item.displayName}",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(onClick = { onPinToggle(item) }) {
                    Icon(
                        imageVector = if (item.pinned) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (item.pinned) {
                            "Desfijar ${item.displayName}"
                        } else {
                            "Fijar ${item.displayName}"
                        },
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(onClick = onDeleteRequest) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Eliminar ${item.displayName}",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
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
