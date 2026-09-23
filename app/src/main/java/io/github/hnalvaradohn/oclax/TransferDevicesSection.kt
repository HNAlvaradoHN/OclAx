package io.github.hnalvaradohn.oclax

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.hnalvaradohn.oclax.data.PairedDevice

@Composable
internal fun TransferDevicesSection(
    status: String,
    busy: Boolean,
    ownDeviceId: String?,
    devices: List<PairedDevice>,
    onProbe: () -> Unit,
    onStop: () -> Unit,
    onShareOwnId: () -> Unit,
    onAddDevice: (name: String, deviceId: String) -> String?,
    onSetAllowWithoutAccept: (deviceId: String, allowed: Boolean) -> Unit,
    onRemoveDevice: (deviceId: String) -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                "Enviar a dispositivo · prueba técnica",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                status,
                style = MaterialTheme.typography.bodySmall,
            )

            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onProbe,
                    enabled = !busy,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                ) {
                    Text("Probar motor")
                }
                TextButton(
                    onClick = onStop,
                    enabled = !busy,
                ) {
                    Text("Detener")
                }
            }

            if (!ownDeviceId.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Este dispositivo",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        ownDeviceId,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    IconButton(onClick = onShareOwnId) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Compartir ID de este dispositivo",
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Mis dispositivos",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                )
                TextButton(
                    onClick = { showAddDialog = true },
                    enabled = ownDeviceId != null && !busy,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                    )
                    Text("Agregar")
                }
            }

            if (devices.isEmpty()) {
                Text(
                    if (ownDeviceId == null) {
                        "Primero probá el motor para obtener el ID de este teléfono."
                    } else {
                        "Todavía no hay dispositivos agregados."
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                devices.forEach { device ->
                    PairedDeviceRow(
                        device = device,
                        onSetAllowWithoutAccept = onSetAllowWithoutAccept,
                        onRemoveDevice = onRemoveDevice,
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddPairedDeviceDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, deviceId ->
                val error = onAddDevice(name, deviceId)
                if (error == null) {
                    showAddDialog = false
                }
                error
            },
        )
    }
}

@Composable
private fun PairedDeviceRow(
    device: PairedDevice,
    onSetAllowWithoutAccept: (deviceId: String, allowed: Boolean) -> Unit,
    onRemoveDevice: (deviceId: String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    device.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    device.deviceId,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = { onRemoveDevice(device.deviceId) }) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Quitar ${device.name}",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Permitir sin aceptar",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "Preferencia guardada para la futura recepción.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = device.allowWithoutAccept,
                onCheckedChange = { allowed ->
                    onSetAllowWithoutAccept(device.deviceId, allowed)
                },
            )
        }
    }
}

@Composable
private fun AddPairedDeviceDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, deviceId: String) -> String?,
) {
    var name by remember { mutableStateOf("") }
    var deviceId by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar dispositivo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Pegá el ID que muestra OclAx en el otro teléfono. No se intercambian archivos todavía.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Nombre") },
                    placeholder = { Text("Mi tablet") },
                )
                OutlinedTextField(
                    value = deviceId,
                    onValueChange = {
                        deviceId = it
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("ID del dispositivo") },
                    placeholder = { Text("AAAAAAA-BBBBBBB-…") },
                    minLines = 2,
                    maxLines = 3,
                )
                error?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    error = onAdd(name, deviceId)
                },
            ) {
                Text("Agregar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
    )
}
