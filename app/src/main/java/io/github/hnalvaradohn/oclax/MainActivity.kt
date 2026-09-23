package io.github.hnalvaradohn.oclax

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = ItemStore(applicationContext)
        setContent {
            MaterialTheme {
                OclAxHome(store)
            }
        }
    }
}

@Composable
private fun OclAxHome(store: ItemStore) {
    var query by remember { mutableStateOf("") }
    val allItems = remember { store.listItems() }
    val visibleItems = remember(query, allItems) {
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) allItems else allItems.filter {
            it.displayName.lowercase().contains(needle) || it.mimeType.lowercase().contains(needle)
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Text("OclAx", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Tu contenido, listo donde lo necesitás.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(18.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Buscar en Recientes") },
            )
            Spacer(Modifier.height(16.dp))

            if (visibleItems.isEmpty()) {
                Text(
                    if (allItems.isEmpty()) {
                        "Todavía no hay elementos. Compartí un texto, imagen o archivo hacia OclAx."
                    } else {
                        "No hay resultados para esa búsqueda."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    items(visibleItems, key = { it.id }) { item ->
                        ItemRow(item)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemRow(item: StoredItem) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text(typeGlyph(item.mimeType), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.displayName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(item.mimeType + " · " + formatBytes(item.byteSize), style = MaterialTheme.typography.bodySmall)
            Text(
                DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(item.createdAt)),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

private fun typeGlyph(mimeType: String): String = when {
    mimeType.startsWith("image/") -> "▣"
    mimeType == "application/pdf" -> "PDF"
    mimeType == "application/vnd.android.package-archive" -> "APK"
    mimeType.startsWith("video/") -> "▶"
    mimeType.startsWith("audio/") -> "♪"
    mimeType.startsWith("text/") -> "TXT"
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
