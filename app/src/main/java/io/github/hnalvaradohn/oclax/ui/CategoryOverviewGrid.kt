package io.github.hnalvaradohn.oclax.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class CategoryOverviewItem(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val count: Int,
    val totalBytes: Long? = null,
    val note: String? = null,
)

internal data class CategoryAccent(
    val container: Color,
    val icon: Color,
)

@Composable
fun CategoryOverviewGrid(
    categories: List<CategoryOverviewItem>,
    onSelect: (CategoryOverviewItem) -> Unit,
    modifier: Modifier = Modifier,
    scrollable: Boolean = false,
) {
    val rows = categories.chunked(2)
    if (scrollable) {
        LazyColumn(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(rows, key = { row -> row.joinToString("|") { it.key } }) { rowItems ->
                CategoryOverviewRow(
                    items = rowItems,
                    onSelect = onSelect,
                )
            }
        }
    } else {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            rows.forEach { rowItems ->
                CategoryOverviewRow(
                    items = rowItems,
                    onSelect = onSelect,
                )
            }
        }
    }
}

@Composable
private fun CategoryOverviewRow(
    items: List<CategoryOverviewItem>,
    onSelect: (CategoryOverviewItem) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items.forEach { item ->
            CategoryOverviewCard(
                item = item,
                onClick = { onSelect(item) },
                modifier = Modifier.weight(1f),
            )
        }
        if (items.size == 1) {
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun CategoryOverviewCard(
    item: CategoryOverviewItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = categoryAccentFor(item.key, isSystemInDarkTheme())
    val iconContainer = accent?.container ?: MaterialTheme.colorScheme.primaryContainer
    val iconColor = accent?.icon ?: MaterialTheme.colorScheme.onPrimaryContainer

    Card(
        modifier = modifier
            .heightIn(min = 132.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                modifier = Modifier.size(54.dp),
                color = iconContainer,
                contentColor = iconColor,
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        modifier = Modifier.size(30.dp),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                item.label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Text(
                categorySummary(item),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            item.note?.takeIf { it.isNotBlank() }?.let { note ->
                Text(
                    note,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

internal fun categoryAccentFor(
    key: String,
    darkTheme: Boolean,
): CategoryAccent? {
    val normalized = key.trim().lowercase()
    val dark = when (normalized) {
        "all", "recent" -> CategoryAccent(Color(0xFF4A2207), Color(0xFFFF8A2A))
        "pinned" -> CategoryAccent(Color(0xFF493C05), Color(0xFFFFD54A))
        "images" -> CategoryAccent(Color(0xFF083F31), Color(0xFF2EE6A6))
        "documents" -> CategoryAccent(Color(0xFF073654), Color(0xFF4DB8FF))
        "pdf" -> CategoryAccent(Color(0xFF50151B), Color(0xFFFF626E))
        "apk", "apps", "applications" ->
            CategoryAccent(Color(0xFF1F4709), Color(0xFF9BEF45))
        "text" -> CategoryAccent(Color(0xFF31205E), Color(0xFFB98AFF))
        "video" -> CategoryAccent(Color(0xFF56123B), Color(0xFFFF61B5))
        "audio" -> CategoryAccent(Color(0xFF063D4B), Color(0xFF4CD7F7))
        "other" -> CategoryAccent(Color(0xFF30343A), Color(0xFFC7CED8))
        else -> null
    }
    if (darkTheme || dark == null) return dark

    return when (normalized) {
        "all", "recent" -> CategoryAccent(Color(0xFFFFF0E3), Color(0xFFB84C00))
        "pinned" -> CategoryAccent(Color(0xFFFFF7D6), Color(0xFF8A6500))
        "images" -> CategoryAccent(Color(0xFFE0F7EF), Color(0xFF007A57))
        "documents" -> CategoryAccent(Color(0xFFE1F2FF), Color(0xFF00639A))
        "pdf" -> CategoryAccent(Color(0xFFFFE7EA), Color(0xFFB42330))
        "apk", "apps", "applications" ->
            CategoryAccent(Color(0xFFEBF8DD), Color(0xFF477A00))
        "text" -> CategoryAccent(Color(0xFFF0E8FF), Color(0xFF6842A5))
        "video" -> CategoryAccent(Color(0xFFFFE5F2), Color(0xFFA60A5E))
        "audio" -> CategoryAccent(Color(0xFFE0F7FC), Color(0xFF00758B))
        "other" -> CategoryAccent(Color(0xFFECEFF3), Color(0xFF505862))
        else -> null
    }
}

private fun categorySummary(item: CategoryOverviewItem): String {
    val countLabel = if (item.count == 1) "1 elemento" else "${item.count} elementos"
    val total = item.totalBytes
    return if (total == null) {
        countLabel
    } else {
        "${formatCategoryBytes(total)} · $countLabel"
    }
}

private fun formatCategoryBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.1f MB", mb)
    return String.format("%.2f GB", mb / 1024.0)
}
