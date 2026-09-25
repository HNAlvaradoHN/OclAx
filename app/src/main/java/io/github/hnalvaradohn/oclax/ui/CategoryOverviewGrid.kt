package io.github.hnalvaradohn.oclax.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
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
