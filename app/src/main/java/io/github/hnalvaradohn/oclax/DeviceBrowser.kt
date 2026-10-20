package io.github.hnalvaradohn.oclax

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Image as ImageIcon
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import io.github.hnalvaradohn.oclax.data.ContentViewMode
import io.github.hnalvaradohn.oclax.model.ContentType
import io.github.hnalvaradohn.oclax.model.supportsClipboardCopy
import io.github.hnalvaradohn.oclax.platform.DeviceFileInfo
import io.github.hnalvaradohn.oclax.platform.InstalledAppInfo
import io.github.hnalvaradohn.oclax.ui.CategoryOverviewGrid
import io.github.hnalvaradohn.oclax.ui.CategoryOverviewItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

private const val MAX_DEVICE_FILE_SELECTION = 100
private const val MAX_DEVICE_APP_SELECTION = 20

private enum class DeviceFilter(
    val key: String,
    @StringRes val labelRes: Int,
    val defaultViewMode: ContentViewMode,
) {
    APPS("apps", R.string.filter_apps, ContentViewMode.GRID),
    IMAGES("images", R.string.filter_images, ContentViewMode.GRID),
    DOCUMENTS("documents", R.string.filter_documents, ContentViewMode.LIST),
    PDF("pdf", R.string.filter_pdf, ContentViewMode.LIST),
    APK("apk", R.string.filter_apk, ContentViewMode.LIST),
    TEXT("text", R.string.filter_text, ContentViewMode.LIST),
    VIDEO("video", R.string.filter_video, ContentViewMode.GRID),
    AUDIO("audio", R.string.filter_audio, ContentViewMode.LIST),
    OTHER("other", R.string.filter_other, ContentViewMode.LIST),
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
    onShareFiles: (List<DeviceFileInfo>) -> Unit,
    onCopyFile: (DeviceFileInfo) -> Unit,
    onDeleteFile: (DeviceFileInfo) -> Unit,
    onRenameFile: (DeviceFileInfo, String) -> Unit,
    onOpenApp: (InstalledAppInfo) -> Unit,
    onShareApp: (InstalledAppInfo) -> Unit,
    onShareApps: (List<InstalledAppInfo>) -> Unit,
    onLoadThumbnail: (DeviceFileInfo, Int) -> Bitmap?,
    onLoadViewMode: (String, ContentViewMode) -> ContentViewMode,
    onSaveViewMode: (String, ContentViewMode) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf<DeviceFilter?>(null) }
    var pendingDelete by remember { mutableStateOf<DeviceFileInfo?>(null) }
    var pendingRename by remember { mutableStateOf<DeviceFileInfo?>(null) }
    var renameText by remember { mutableStateOf("") }
    var viewMode by remember { mutableStateOf(ContentViewMode.LIST) }
    var selecting by remember { mutableStateOf(false) }
    var selectedFileUris by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedAppPackages by remember { mutableStateOf<Set<String>>(emptySet()) }

    BackHandler(enabled = selecting || filter != null || query.isNotBlank()) {
        if (selecting) {
            selecting = false
            selectedFileUris = emptySet()
            selectedAppPackages = emptySet()
        } else {
            filter = null
            query = ""
        }
    }

    pendingDelete?.let { file ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.delete_device_title)) },
            text = {
                Text(stringResource(R.string.delete_device_message, file.displayName))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        onDeleteFile(file)
                    },
                ) {
                    Text(stringResource(R.string.delete_original), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    pendingRename?.let { file ->
        AlertDialog(
            onDismissRequest = { pendingRename = null },
            title = { Text(stringResource(R.string.rename_original_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.rename_original_message),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        singleLine = true,
                        label = { Text(stringResource(R.string.name)) },
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = pendingRename ?: return@TextButton
                        pendingRename = null
                        onRenameFile(target, renameText)
                    },
                ) {
                    Text(stringResource(R.string.rename))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRename = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    val needle = query.trim().lowercase()
    val visibleApps = remember(apps, needle, filter) {
        when {
            filter != null && filter != DeviceFilter.APPS -> emptyList()
            needle.isEmpty() && filter == null -> emptyList()
            else -> apps.filter {
                needle.isEmpty() ||
                    it.label.lowercase().contains(needle) ||
                    it.packageName.lowercase().contains(needle)
            }
        }
    }

    val visibleFiles = remember(files, needle, filter) {
        when {
            filter == DeviceFilter.APPS -> emptyList()
            needle.isEmpty() && filter == null -> emptyList()
            else -> files.filter { file ->
                (filter == null || matchesDeviceFilter(file, requireNotNull(filter))) &&
                    (
                        needle.isEmpty() ||
                            file.displayName.lowercase().contains(needle) ||
                            file.mimeType.lowercase().contains(needle) ||
                            file.relativePath.orEmpty().lowercase().contains(needle)
                        )
            }
        }
    }

    val categoryOverview = remember(files, apps, hasBroadFileAccess) {
        deviceCategoryOverviewItems(
            files = files,
            apps = apps,
            hasBroadFileAccess = hasBroadFileAccess,
        )
    }
    val selectedFiles = remember(files, selectedFileUris) {
        files.filter { it.uri.toString() in selectedFileUris }
    }
    val selectedApps = remember(apps, selectedAppPackages) {
        apps.filter { it.packageName in selectedAppPackages }
    }

    LaunchedEffect(files, apps) {
        selectedFileUris = selectedFileUris.intersect(files.mapTo(mutableSetOf()) { it.uri.toString() })
        selectedAppPackages =
            selectedAppPackages.intersect(apps.mapTo(mutableSetOf()) { it.packageName })
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            enabled = !selecting,
            singleLine = true,
            label = { Text(stringResource(R.string.search_device)) },
        )
        Spacer(Modifier.height(10.dp))

        if (filter == null && query.isBlank()) {
            CategoryOverviewGrid(
                categories = categoryOverview,
                onSelect = { selected ->
                    val next = DeviceFilter.entries.firstOrNull { it.key == selected.key }
                        ?: return@CategoryOverviewGrid
                    filter = next
                    selecting = false
                    selectedFileUris = emptySet()
                    selectedAppPackages = emptySet()
                    viewMode = onLoadViewMode(next.key, next.defaultViewMode)
                },
                modifier = Modifier.weight(1f),
                scrollable = true,
            )
            return@Column
        }

        if (filter == null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = { query = "" },
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(stringResource(R.string.categories))
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.results),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(8.dp))

            if (visibleApps.isEmpty() && visibleFiles.isEmpty()) {
                Text(stringResource(R.string.empty_device_search))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    items(visibleApps, key = { "app:" + it.packageName }) { app ->
                        DeviceAppListCard(
                            app = app,
                            selectionMode = false,
                            selected = false,
                            onToggleSelection = {},
                            onOpen = onOpenApp,
                            onShare = onShareApp,
                        )
                    }
                    items(visibleFiles, key = { "file:" + it.uri.toString() }) { file ->
                        DeviceFileListCard(
                            file = file,
                            selectionMode = false,
                            selected = false,
                            onToggleSelection = {},
                            onOpen = onOpenFile,
                            onShare = onShareFile,
                            onCopy = onCopyFile,
                            onRenameRequest = {
                                pendingRename = file
                                renameText = file.displayName
                            },
                            onDeleteRequest = { pendingDelete = file },
                            onLoadThumbnail = onLoadThumbnail,
                        )
                    }
                }
            }
            return@Column
        }

        val activeFilter = requireNotNull(filter)
        val selectingApps = activeFilter == DeviceFilter.APPS
        val selectionCount = if (selectingApps) selectedApps.size else selectedFiles.size
        val allVisibleSelected = if (selectingApps) {
            visibleApps.isNotEmpty() && visibleApps.all { it.packageName in selectedAppPackages }
        } else {
            visibleFiles.isNotEmpty() && visibleFiles.all { it.uri.toString() in selectedFileUris }
        }

        if (selecting) {
            DeviceBulkSelectionBar(
                selectedCount = selectionCount,
                allVisibleSelected = allVisibleSelected,
                onToggleAll = {
                    if (selectingApps) {
                        val visiblePackages = visibleApps
                            .take(MAX_DEVICE_APP_SELECTION)
                            .mapTo(mutableSetOf()) { it.packageName }
                        selectedAppPackages = if (
                            visiblePackages.isNotEmpty() &&
                            visiblePackages.all { it in selectedAppPackages }
                        ) {
                            selectedAppPackages - visiblePackages
                        } else {
                            visiblePackages
                        }
                    } else {
                        val visibleUris = visibleFiles
                            .take(MAX_DEVICE_FILE_SELECTION)
                            .mapTo(mutableSetOf()) { it.uri.toString() }
                        selectedFileUris = if (
                            visibleUris.isNotEmpty() &&
                            visibleUris.all { it in selectedFileUris }
                        ) {
                            selectedFileUris - visibleUris
                        } else {
                            visibleUris
                        }
                    }
                },
                onShare = {
                    if (selectingApps) {
                        onShareApps(selectedApps)
                    } else {
                        onShareFiles(selectedFiles)
                    }
                    selecting = false
                    selectedFileUris = emptySet()
                    selectedAppPackages = emptySet()
                },
                onCancel = {
                    selecting = false
                    selectedFileUris = emptySet()
                    selectedAppPackages = emptySet()
                },
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = {
                        filter = null
                        query = ""
                    },
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(stringResource(R.string.categories))
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(activeFilter.labelRes),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                val hasSelectableContent =
                    if (activeFilter == DeviceFilter.APPS) visibleApps.isNotEmpty() else visibleFiles.isNotEmpty()
                if (hasSelectableContent) {
                    TextButton(
                        onClick = {
                            selecting = true
                            selectedFileUris = emptySet()
                            selectedAppPackages = emptySet()
                        },
                    ) {
                        Text(stringResource(R.string.select))
                    }
                }
                IconButton(
                    onClick = {
                        viewMode = if (viewMode == ContentViewMode.LIST) {
                            ContentViewMode.GRID
                        } else {
                            ContentViewMode.LIST
                        }
                        onSaveViewMode(activeFilter.key, viewMode)
                    },
                ) {
                    Icon(
                        imageVector = if (viewMode == ContentViewMode.LIST) {
                            Icons.Outlined.GridView
                        } else {
                            Icons.Outlined.ViewList
                        },
                        contentDescription = if (viewMode == ContentViewMode.LIST) {
                            stringResource(
                                R.string.grid_view,
                                stringResource(activeFilter.labelRes),
                            )
                        } else {
                            stringResource(
                                R.string.list_view,
                                stringResource(activeFilter.labelRes),
                            )
                        },
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        if (activeFilter != DeviceFilter.APPS && !hasBroadFileAccess) {
            AccessRequiredCard(onRequestBroadAccess)
            return@Column
        }

        when {
            isLoading -> Text(stringResource(R.string.loading_device))
            activeFilter == DeviceFilter.APPS && visibleApps.isEmpty() ->
                Text(stringResource(R.string.empty_apps_search))
            activeFilter == DeviceFilter.APPS -> {
                if (viewMode == ContentViewMode.GRID) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(148.dp),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        gridItems(visibleApps, key = { it.packageName }) { app ->
                            DeviceAppGridCard(
                                app = app,
                                selectionMode = selecting,
                                selected = app.packageName in selectedAppPackages,
                                onToggleSelection = {
                                    selectedAppPackages = if (app.packageName in selectedAppPackages) {
                                        selectedAppPackages - app.packageName
                                    } else if (selectedAppPackages.size < MAX_DEVICE_APP_SELECTION) {
                                        selectedAppPackages + app.packageName
                                    } else {
                                        selectedAppPackages
                                    }
                                },
                                onOpen = onOpenApp,
                                onShare = onShareApp,
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(visibleApps, key = { it.packageName }) { app ->
                            DeviceAppListCard(
                                app = app,
                                selectionMode = selecting,
                                selected = app.packageName in selectedAppPackages,
                                onToggleSelection = {
                                    selectedAppPackages = if (app.packageName in selectedAppPackages) {
                                        selectedAppPackages - app.packageName
                                    } else if (selectedAppPackages.size < MAX_DEVICE_APP_SELECTION) {
                                        selectedAppPackages + app.packageName
                                    } else {
                                        selectedAppPackages
                                    }
                                },
                                onOpen = onOpenApp,
                                onShare = onShareApp,
                            )
                        }
                    }
                }
            }
            visibleFiles.isEmpty() ->
                Text(
                    stringResource(
                        R.string.empty_device_category,
                        stringResource(activeFilter.labelRes).lowercase(),
                    ),
                )
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
                            selectionMode = selecting,
                            selected = file.uri.toString() in selectedFileUris,
                            onToggleSelection = {
                                val key = file.uri.toString()
                                selectedFileUris = if (key in selectedFileUris) {
                                    selectedFileUris - key
                                } else if (selectedFileUris.size < MAX_DEVICE_FILE_SELECTION) {
                                    selectedFileUris + key
                                } else {
                                    selectedFileUris
                                }
                            },
                            onOpen = onOpenFile,
                            onShare = onShareFile,
                            onCopy = onCopyFile,
                            onRenameRequest = {
                                pendingRename = file
                                renameText = file.displayName
                            },
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
                            selectionMode = selecting,
                            selected = file.uri.toString() in selectedFileUris,
                            onToggleSelection = {
                                val key = file.uri.toString()
                                selectedFileUris = if (key in selectedFileUris) {
                                    selectedFileUris - key
                                } else if (selectedFileUris.size < MAX_DEVICE_FILE_SELECTION) {
                                    selectedFileUris + key
                                } else {
                                    selectedFileUris
                                }
                            },
                            onOpen = onOpenFile,
                            onShare = onShareFile,
                            onCopy = onCopyFile,
                            onRenameRequest = {
                                pendingRename = file
                                renameText = file.displayName
                            },
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
private fun deviceCategoryOverviewItems(
    files: List<DeviceFileInfo>,
    apps: List<InstalledAppInfo>,
    hasBroadFileAccess: Boolean,
): List<CategoryOverviewItem> =
    DeviceFilter.entries.map { filter ->
        if (filter == DeviceFilter.APPS) {
            CategoryOverviewItem(
                key = filter.key,
                label = stringResource(R.string.filter_applications),
                icon = Icons.Outlined.Android,
                count = apps.size,
                note = stringResource(R.string.installed_on_device),
            )
        } else {
            val matching = files.filter { matchesDeviceFilter(it, filter) }
            CategoryOverviewItem(
                key = filter.key,
                label = stringResource(filter.labelRes),
                icon = deviceFilterIcon(filter),
                count = matching.size,
                totalBytes = if (hasBroadFileAccess) {
                    matching.sumOf { it.byteSize }
                } else {
                    null
                },
                note = if (hasBroadFileAccess) null else stringResource(R.string.requires_access),
            )
        }
    }

private fun deviceFilterIcon(filter: DeviceFilter): ImageVector = when (filter) {
    DeviceFilter.APPS -> Icons.Outlined.Android
    DeviceFilter.IMAGES -> Icons.Outlined.ImageIcon
    DeviceFilter.DOCUMENTS -> Icons.Outlined.Description
    DeviceFilter.PDF -> Icons.Outlined.PictureAsPdf
    DeviceFilter.APK -> Icons.Outlined.Android
    DeviceFilter.TEXT -> Icons.Outlined.Code
    DeviceFilter.VIDEO -> Icons.Outlined.Movie
    DeviceFilter.AUDIO -> Icons.Outlined.AudioFile
    DeviceFilter.OTHER -> Icons.Outlined.InsertDriveFile
}

@Composable
private fun DeviceBulkSelectionBar(
    selectedCount: Int,
    allVisibleSelected: Boolean,
    onToggleAll: () -> Unit,
    onShare: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.selected_count, selectedCount),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            TextButton(onClick = onToggleAll) {
                Text(
                stringResource(
                    if (allVisibleSelected) R.string.select_none else R.string.select_all,
                ),
            )
            }
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.cancel))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            IconButton(
                onClick = onShare,
                enabled = selectedCount > 0,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = stringResource(R.string.share_selected),
                    tint = MaterialTheme.colorScheme.primary,
                )
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
                stringResource(R.string.file_access_title),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.file_access_message),
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(10.dp))
            Button(onClick = onRequestBroadAccess) {
                Text(stringResource(R.string.grant_access))
            }
        }
    }
}

@Composable
private fun DeviceAppListCard(
    app: InstalledAppInfo,
    selectionMode: Boolean,
    selected: Boolean,
    onToggleSelection: () -> Unit,
    onOpen: (InstalledAppInfo) -> Unit,
    onShare: (InstalledAppInfo) -> Unit,
) {
    val bitmap = remember(app.packageName) {
        app.icon.toBitmap(width = 48, height = 48).asImageBitmap()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (selectionMode) onToggleSelection() else onOpen(app)
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selectionMode) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = { onToggleSelection() },
                    modifier = Modifier.semantics {
                        contentDescription = stringResource(R.string.select_item, app.label)
                    },
                )
                Spacer(Modifier.width(4.dp))
            }
            Image(
                bitmap = bitmap,
                contentDescription = stringResource(R.string.icon_of, app.label),
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
                    stringResource(
                        if (app.isSystemApp) R.string.system_app else R.string.installed_app,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            if (!selectionMode) {
                IconButton(onClick = { onShare(app) }) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = stringResource(R.string.share_item, app.label),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceAppGridCard(
    app: InstalledAppInfo,
    selectionMode: Boolean,
    selected: Boolean,
    onToggleSelection: () -> Unit,
    onOpen: (InstalledAppInfo) -> Unit,
    onShare: (InstalledAppInfo) -> Unit,
) {
    val bitmap = remember(app.packageName) {
        app.icon.toBitmap(width = 96, height = 96).asImageBitmap()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (selectionMode) onToggleSelection() else onOpen(app)
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (selectionMode) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = { onToggleSelection() },
                    modifier = Modifier.semantics {
                        contentDescription = stringResource(R.string.select_item, app.label)
                    },
                )
            }
            Image(
                bitmap = bitmap,
                contentDescription = stringResource(R.string.icon_of, app.label),
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
            if (!selectionMode) {
                IconButton(onClick = { onShare(app) }) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = stringResource(R.string.share_item, app.label),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceFileListCard(
    file: DeviceFileInfo,
    selectionMode: Boolean,
    selected: Boolean,
    onToggleSelection: () -> Unit,
    onOpen: (DeviceFileInfo) -> Unit,
    onShare: (DeviceFileInfo) -> Unit,
    onCopy: (DeviceFileInfo) -> Unit,
    onRenameRequest: () -> Unit,
    onDeleteRequest: () -> Unit,
    onLoadThumbnail: (DeviceFileInfo, Int) -> Bitmap?,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (selectionMode) onToggleSelection() else onOpen(file)
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selectionMode) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = { onToggleSelection() },
                    modifier = Modifier.semantics {
                        contentDescription = stringResource(R.string.select_item, file.displayName)
                    },
                )
                Spacer(Modifier.width(4.dp))
            }
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
                    stringResource(file.type.labelRes) + " · " + formatDeviceBytes(file.byteSize),
                    style = MaterialTheme.typography.bodySmall,
                )
                formatDeviceModifiedAt(file.modifiedAt)?.let { modified ->
                    Text(
                        stringResource(R.string.modified_at, modified),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                if (!file.relativePath.isNullOrBlank()) {
                    Text(
                        file.relativePath,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                }
            }
            if (!selectionMode) {
                DeviceFileActions(file, onShare, onCopy, onRenameRequest, onDeleteRequest)
            }
        }
    }
}

@Composable
private fun DeviceFileGridCard(
    file: DeviceFileInfo,
    selectionMode: Boolean,
    selected: Boolean,
    onToggleSelection: () -> Unit,
    onOpen: (DeviceFileInfo) -> Unit,
    onShare: (DeviceFileInfo) -> Unit,
    onCopy: (DeviceFileInfo) -> Unit,
    onRenameRequest: () -> Unit,
    onDeleteRequest: () -> Unit,
    onLoadThumbnail: (DeviceFileInfo, Int) -> Bitmap?,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (selectionMode) onToggleSelection() else onOpen(file)
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            if (selectionMode) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = { onToggleSelection() },
                    modifier = Modifier.semantics {
                        contentDescription = stringResource(R.string.select_item, file.displayName)
                    },
                )
            }
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
            formatDeviceModifiedAt(file.modifiedAt)?.let { modified ->
                Text(
                    stringResource(R.string.modified_at, modified),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            }
            if (!selectionMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    DeviceFileActions(file, onShare, onCopy, onRenameRequest, onDeleteRequest)
                }
            }
        }
    }
}

@Composable
private fun DeviceFileActions(
    file: DeviceFileInfo,
    onShare: (DeviceFileInfo) -> Unit,
    onCopy: (DeviceFileInfo) -> Unit,
    onRenameRequest: () -> Unit,
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
        onClick = onRenameRequest,
        modifier = Modifier.size(40.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Edit,
            contentDescription = "Renombrar ${file.displayName}",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
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
                    contentDescription = stringResource(file.type.labelRes),
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

private fun formatDeviceModifiedAt(modifiedAt: Long): String? =
    modifiedAt
        .takeIf { it > 0L }
        ?.let { timestamp ->
            DateFormat
                .getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                .format(Date(timestamp))
        }

private fun formatDeviceBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.1f MB", mb)
    return String.format("%.2f GB", mb / 1024.0)
}
