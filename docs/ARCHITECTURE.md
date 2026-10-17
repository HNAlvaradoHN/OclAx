# ARCHITECTURE

## Estado

Arquitectura Android local activa. La red/LAN fue retirada por DEC-032.

## Principio

OclAx separa:
- UI / presentación;
- reglas de producto;
- almacenamiento temporal;
- acceso al contenido real del dispositivo;
- integración con Android.

No existe motor de transferencia entre dispositivos.

## Flujo principal de ingreso

```text
Android Share Sheet
      ↓
ShareReceiverActivity
      ↓
ShareIngestor
      ↓
ItemStore
  ↙       ↘
payload    meta.json
privado    origen/retención
      ↓
RetentionScheduler
```

`ItemStore`:
- copia el contenido a almacenamiento privado;
- sanitiza nombre/MIME;
- limita tamaño y comprueba espacio;
- conserva metadata de origen solo cuando existe un `content://`;
- ejecuta la política de Fijados/retención;
- puede eliminar el original únicamente bajo DEC-033;
- permite renombrar metadata OclAx y sincronizar el nombre con un origen MediaStore enlazado.

## Capturas recientes

La decisión de borrar también el original está fuera de UI y se divide en:

```text
URI compartida
   ↓
metadata MediaStore
   ↓
SourceRetentionPolicy
   ↓
deleteSourceOnExpiry = true/false
   ↓
ItemStore.cleanupExpired()
   ↓
revalidar misma URI + tamaño + DATE_ADDED
   ↓
borrar original si sigue siendo el mismo
```

Condiciones de elegibilidad:
- MIME de imagen;
- origen MediaStore;
- nombre/ruta compatible con screenshot;
- `DATE_ADDED` disponible;
- edad al importar ≤60 s.

No se usa coincidencia aproximada para buscar otro archivo si la URI original deja de ser válida.

## Planificación de retención

`RetentionScheduler` usa `AlarmManager.setAndAllowWhileIdle` para programar la próxima expiración.

Reglas:
- no se solicita `SCHEDULE_EXACT_ALARM`;
- el sistema puede diferir el disparo bajo ahorro de energía;
- al arrancar el dispositivo, el receiver vuelve a calcular la próxima expiración;
- abrir/listar OclAx también ejecuta cleanup, por lo que la política no depende solo de la alarma.

## UI principal

```text
MainActivity
 ├─ OclAx
 │   ├─ búsqueda
 │   ├─ categorías
 │   ├─ retención
 │   ├─ abrir/compartir/copiar
 │   ├─ renombrar
 │   ├─ fijar
 │   └─ eliminar copia
 │
 └─ Mi dispositivo
     ├─ InstalledAppsRepository
     └─ DeviceContentRepository
         ├─ MediaStore
         ├─ abrir/compartir/copiar
         ├─ renombrar original
         └─ eliminar original
```

La UI no modifica MediaStore directamente; pasa por `DeviceContentRepository`.

## Selectores externos

### ACTION_GET_CONTENT

`OclAxPickerActivity` ofrece la UI propia de OclAx:
- selector **OclAx / Mi dispositivo**;
- búsqueda/categorías;
- miniaturas;
- filtro por MIME pedido por la app llamadora;
- retorno de URI de solo lectura.

### DocumentsProvider / Archivos

`OclAxDocumentsProvider` expone:
- copias privadas OclAx;
- categorías OclAx;
- una entrada **Mi dispositivo**;
- categorías de archivos reales;
- archivos reales recientes directamente en la raíz cuando hay acceso amplio;
- búsqueda y recientes combinados;
- miniaturas para imagen/video/PDF.

El provider sigue siendo de solo lectura hacia apps externas.

**Límite de plataforma:** la aplicación **Archivos** controla su propia presentación. OclAx controla el árbol de documentos, nombres, MIME, fechas, tamaños y miniaturas; no puede reemplazar esa pantalla por Compose.

## Renombrado

Dos contratos:

1. **OclAx**
   - siempre puede cambiar el nombre visible de su copia;
   - si la copia conserva una URI MediaStore enlazada, intenta renombrar también el original;
   - si el origen no permite escritura, informa la limitación.

2. **Mi dispositivo**
   - renombra el original mediante `ContentResolver.update(DISPLAY_NAME)`;
   - requiere el acceso de almacenamiento correspondiente.

No se cambia silenciosamente la extensión; el diálogo muestra el nombre completo para que el usuario decida.

## Aplicaciones instaladas

`InstalledAppsRepository` inventaría aplicaciones visibles al sistema.
`InstalledAppExporter` copia temporalmente base APK + splits a caché privada para compartir, sin datos de usuario de la app.

## Miniaturas

`ThumbnailLoader` centraliza:
- imágenes;
- video;
- primera página de PDF.

Se reutiliza en:
- OclAx;
- Mi dispositivo;
- picker propio;
- DocumentsProvider.

## Persistencia

- copias OclAx: `filesDir/oclax/items/<uuid>/`;
- metadata: `meta.json` por elemento;
- preferencias de retención/vista: SharedPreferences privadas;
- exportaciones APK: caché temporal;
- miniaturas del provider: caché regenerable.

No existe base de datos ni servicio remoto.

## Permisos relevantes

- `MANAGE_EXTERNAL_STORAGE`: acceso amplio aprobado para **Mi dispositivo**;
- `QUERY_ALL_PACKAGES`: inventario de aplicaciones aprobado;
- `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE`: compatibilidad con Android antiguos;
- `REQUEST_INSTALL_PACKAGES`: apertura/instalación explícita de APK cuando corresponda;
- `RECEIVE_BOOT_COMPLETED`: recalcular la autolimpieza tras reinicio.

**No existen:** `INTERNET`, multicast, network-state ni foreground data-sync.

## Regla de reemplazo

Cuando una función se retire, sus consumidores, recursos, permisos, configuración, tests y CI se eliminan si ya no sirven a otra función. Git conserva el historial; el árbol activo solo conserva lo vigente.
