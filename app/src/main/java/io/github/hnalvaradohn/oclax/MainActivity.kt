package io.github.hnalvaradohn.oclax

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.hnalvaradohn.oclax.data.ItemStore
import io.github.hnalvaradohn.oclax.model.StoredItem
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    private val store by lazy { ItemStore(applicationContext) }
    private var items by mutableStateOf<List<StoredItem>>(emptyList())
    private var retentionHours by mutableIntStateOf(24)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                OclAxHome(
                    allItems = items,
                    retentionHours = retentionHours,
                    onRetentionChange = { hours -> store.setRetentionHours(hours); refresh() },
                    onPinToggle = { item -> store.setPinned(item.id, !item.pinned); refresh() },
                )
            }
        }
    }

    override fun onResume() { super.onResume(); refresh() }

    private fun refresh() {
        retentionHours = store.retentionHours()
        items = store.listItems()
    }
}

private enum class ContentFilter(val label: String) {
    ALL("Todo"), PINNED("Fijados"), IMAGES("Imágenes"), DOCUMENTS("Documentos"),
    PDF("PDF"), APPS("Apps/APK"), TEXT("Texto/Código"), VIDEO("Video"), AUDIO("Audio"), OTHER("Otros")
}

@Composable
private fun OclAxHome(
    allItems: List<StoredItem>, retentionHours: Int,
    onRetentionChange: (Int) -> Unit, onPinToggle: (StoredItem) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(ContentFilter.ALL) }
    val visibleItems = remember(query, filter, allItems) {
        val needle = query.trim().lowercase()
        allItems.filter { item ->
            matchesFilter(item, filter) && (needle.isEmpty() ||
                item.displayName.lowercase().contains(needle) || item.mimeType.lowercase().contains(needle))
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Text("OclAx", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Tu contenido, listo donde lo necesitás.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(18.dp))
            OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
                singleLine = true, label = { Text("Buscar") })
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ContentFilter.entries.forEach { option ->
                    FilterChip(selected = filter == option, onClick = { filter = option }, label = { Text(option.label) })
                }
            }
            Spacer(Modifier.height(10.dp))
            RetentionControl(retentionHours, onRetentionChange)
            Spacer(Modifier.height(10.dp))

            if (visibleItems.isEmpty()) {
                Text(if (allItems.isEmpty()) "Todavía no hay elementos. Compartí contenido hacia OclAx."
                    else "No hay elementos en ${filter.label.lowercase()} para esa búsqueda.",
                    style = MaterialTheme.typography.bodyLarge)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    items(visibleItems, key = { it.id }) { item ->
                        ItemRow(item, onPinToggle)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

private fun matchesFilter(item: StoredItem, filter: ContentFilter): Boolean {
    val mime = item.mimeType.lowercase()
    return when (filter) {
        ContentFilter.ALL -> true
        ContentFilter.PINNED -> item.pinned
        ContentFilter.IMAGES -> mime.startsWith("image/")
        ContentFilter.PDF -> mime == "application/pdf"
        ContentFilter.APPS -> mime == "application/vnd.android.package-archive"
        ContentFilter.TEXT -> mime.startsWith("text/") || mime.contains("json") || mime.contains("xml")
        ContentFilter.VIDEO -> mime.startsWith("video/")
        ContentFilter.AUDIO -> mime.startsWith("audio/")
        ContentFilter.DOCUMENTS -> isDocumentMime(mime)
        ContentFilter.OTHER -> !isKnownMime(mime)
    }
}

private fun isDocumentMime(mime: String): Boolean = mime.contains("msword") ||
    mime.contains("officedocument") || mime.contains("opendocument") || mime == "application/rtf"

private fun isKnownMime(mime: String): Boolean = mime.startsWith("image/") || mime == "application/pdf" ||
    mime == "application/vnd.android.package-archive" || mime.startsWith("video/") || mime.startsWith("audio/") ||
    mime.startsWith("text/") || mime.contains("json") || mime.contains("xml") || isDocumentMime(mime)

@Composable
private fun RetentionControl(retentionHours: Int, onRetentionChange: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text("Borrar copias de OclAx después de", style = MaterialTheme.typography.labelMedium)
        OutlinedButton(onClick = { expanded = true }) { Text(retentionLabel(retentionHours)) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(1, 24, 72, 168, 0).forEach { hours ->
                DropdownMenuItem(text = { Text(retentionLabel(hours)) }, onClick = {
                    expanded = false; onRetentionChange(hours)
                })
            }
        }
        Text("Solo elimina copias internas de OclAx; nunca el archivo original del dispositivo.",
            style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ItemRow(item: StoredItem, onPinToggle: (StoredItem) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text(typeGlyph(item.mimeType), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.displayName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(typeLabel(item.mimeType) + " · " + formatBytes(item.byteSize), style = MaterialTheme.typography.bodySmall)
            Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(item.createdAt)),
                style = MaterialTheme.typography.labelSmall)
        }
        Text(if (item.pinned) "★" else "☆", modifier = Modifier.clickable { onPinToggle(item) }.padding(12.dp),
            style = MaterialTheme.typography.titleLarge)
    }
}

private fun retentionLabel(hours: Int): String = when (hours) {
    1 -> "1 hora"; 24 -> "24 horas"; 72 -> "3 días"; 168 -> "7 días"; 0 -> "Nunca"; else -> "24 horas"
}

private fun typeLabel(mimeType: String): String = when {
    mimeType.startsWith("image/") -> "Imagen"
    mimeType == "application/pdf" -> "PDF"
    mimeType == "application/vnd.android.package-archive" -> "Aplicación APK"
    mimeType.startsWith("video/") -> "Video"
    mimeType.startsWith("audio/") -> "Audio"
    mimeType.startsWith("text/") || mimeType.contains("json") || mimeType.contains("xml") -> "Texto/Código"
    isDocumentMime(mimeType.lowercase()) -> "Documento"
    else -> "Archivo"
}

private fun typeGlyph(mimeType: String): String = when {
    mimeType.startsWith("image/") -> "▣"
    mimeType == "application/pdf" -> "PDF"
    mimeType == "application/vnd.android.package-archive" -> "APK"
    mimeType.startsWith("video/") -> "▶"
    mimeType.startsWith("audio/") -> "♪"
    mimeType.startsWith("text/") || mimeType.contains("json") || mimeType.contains("xml") -> "TXT"
    isDocumentMime(mimeType.lowercase()) -> "DOC"
    else -> "FILE"
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.1f MB", mb)
    return String.format("%.2f GB", mb / 1024.0)
}
