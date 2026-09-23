# ARCHITECTURE

## Estado

Arquitectura Android activa. La implementación real ya incluye bandeja OclAx, DocumentsProvider/FileProvider, Mi dispositivo y las capas de plataforma descritas más abajo.

## Plataforma

Android.

## Principio

Un cambio visual no debe afectar innecesariamente lógica, datos o integraciones del sistema.

## Capas previstas

### UI / Presentación
Pantallas, navegación, componentes y estados visuales.

### Theme / Personalización
Colores, tipografía, formas, tamaños, espaciados y tokens visuales.

### Domain / Lógica
Reglas del producto y casos de uso.

### Data / Persistencia
Metadatos, archivos locales, preferencias y repositorios de datos.

### Platform / Android
Integraciones con APIs del sistema, selector de archivos, portapapeles, trabajos en segundo plano y permisos.

## Reglas

- No lógica de negocio enterrada en UI.
- No dependencias circulares.
- No sobre-modularización.
- Lo visual debe ser reemplazable sin reescribir el núcleo.
- Una implementación sustituida se elimina cuando ya no tenga consumidores.

## Tecnología

Android nativo con Kotlin y Jetpack Compose. Las versiones concretas verificadas del toolchain se registran en DECISIONS y el código Gradle; cualquier dependencia nueva requiere necesidad y validación según AGENTS.md.


## Implementación Android inicial — 2026-09-22

La primera prueba vertical usa una sola aplicación Android nativa.

```text
ShareReceiverActivity
        ↓
   ShareIngestor
        ↓
     ItemStore
      ↙     ↘
FileProvider  DocumentsProvider
clipboard       selector Android

MainActivity (Compose)
        ↓
     ItemStore
```

Reglas vigentes:
- almacenamiento en directorio privado de la app;
- cada elemento vive en un directorio opaco generado por UUID;
- nombre visible y MIME se guardan como metadata, nunca controlan rutas;
- archivos se copian por streaming, no se cargan completos en RAM;
- FileProvider se usa para URI de portapapeles;
- DocumentsProvider se usa como fuente del selector del sistema;
- no existe permiso de Internet;
- no existe acceso total al almacenamiento del teléfono;
- la UI no posee la lógica de importación ni del provider.

El escaneo simple del directorio es deliberado para esta prueba vertical. Si el volumen real lo exige, la indexación podrá evolucionar después sin cambiar los contratos de ShareIngestor/DocumentsProvider.


## Aplicaciones instaladas — bloque visual

La consulta de aplicaciones instaladas pertenece a Plataforma/Android y se encapsula en `InstalledAppsRepository`.

```text
MainActivity (Compose)
        ↓
InstalledAppsRepository
        ↓
PackageManager
        ↓
MAIN + LAUNCHER visibles
```

Reglas:
- la UI no consulta PackageManager directamente;
- no se solicita visibilidad total de paquetes;
- se muestran solo apps lanzables visibles mediante la consulta declarada en manifest;
- la lista no se persiste ni se transmite;
- los archivos APK de OclAx continúan usando el modelo de contenido existente y no se mezclan con apps instaladas.


## Mi dispositivo — acceso amplio

La nueva superficie **Mi dispositivo** queda separada de la bandeja temporal.

```text
UI / Mi dispositivo
        ↓
DeviceContentRepository
   ↙      ↓       ↘
Apps   MediaStore  Storage index
        / SAF      / acceso amplio
```

Principios:
- la UI no accede directamente a APIs de almacenamiento/paquetes;
- una capa de plataforma resuelve permisos y capacidades disponibles;
- contenido externo se referencia como contenido del dispositivo; no se convierte en copia OclAx salvo que el usuario lo importe/envíe;
- la bandeja temporal y su ItemStore conservan la propiedad exclusiva de sus copias;
- denegar/revocar permisos no debe romper la bandeja OclAx.

## Transferencia entre dispositivos

Syncthing core v2.x es el motor candidato, encapsulado detrás de una capa propia. El wrapper Android oficial discontinuado no forma parte de la arquitectura OclAx.

```text
UI: Enviar a dispositivo
        ↓
TransferService / Domain
        ↓
SyncthingAdapter
        ↓ REST loopback + API key privada
SyncthingRuntime (foreground service)
        ↓
Syncthing core nativo pinneado por versión
        ↓
dispositivo OclAx emparejado
```

El runtime nativo:
- vive en directorios privados de OclAx;
- no expone su GUI/API fuera de loopback;
- desactiva auto-upgrade/usage reporting;
- se construye en CI separada de secretos de firma;
- no filtra conceptos de carpetas Syncthing hacia la UI principal.

OclAx controla:
- emparejamiento;
- lista/nombre de dispositivos;
- política de confianza;
- creación de una transferencia;
- progreso/cancelación/reintento;
- bandeja de recepción.

Syncthing no debe filtrar conceptos de sincronización de carpetas hacia la UX principal.

Recepción:
- todo contenido entrante se materializa primero en almacenamiento privado/controlado por OclAx;
- posteriormente el usuario puede compartirlo, copiarlo cuando aplique o guardarlo externamente;
- autoaceptación nunca implica autoejecución/autoinstalación.


## Abrir contenido

La integración para abrir contenido externo pertenece a Plataforma/Android y queda encapsulada en `ContentOpener`.

```text
ItemCard
   ↓ tap
MainActivity
   ↓
ContentOpener
   ↓
FileProvider URI (read-only)
   ↓
Android ACTION_VIEW
   ↓
visor / galería / reproductor / instalador compatible
```

Reglas:
- ItemStore sigue siendo la autoridad sobre la copia interna;
- ContentOpener no modifica ni interpreta el archivo;
- el receptor recibe permiso temporal de lectura;
- la elección de aplicación predeterminada/resolución pertenece a Android;
- APK usa el mismo principio de apertura explícita; OclAx no instala silenciosamente.

La futura superficie Mi dispositivo reutiliza el mismo caso de uso con URIs externas autorizadas, evitando copias innecesarias.


## Implementación Mi dispositivo — 2026-09-23

```text
MainActivity
   ↓
DeviceContentRepository ──→ MediaStore.Files
   │
   └─ permisos/capacidad → Environment.isExternalStorageManager()

MainActivity
   ↓
InstalledAppsRepository ──→ PackageManager.getInstalledApplications()
```

Detalles:
- el índice del dispositivo se carga fuera del hilo principal mediante un executor único;
- la UI recibe listas ya materializadas y filtra localmente por categoría/búsqueda;
- `QUERY_ALL_PACKAGES` permite inventario completo de aplicaciones;
- `MANAGE_EXTERNAL_STORAGE` habilita el índice amplio de archivos compartidos;
- el acceso al dispositivo permite lectura/uso y borrado explícito de originales únicamente desde el caso de uso documentado más abajo;
- abrir archivos reutiliza ContentOpener con URI MediaStore;
- compartir concede lectura temporal al receptor;
- Copiar se limita a Texto/Código e Imagen igual que en la bandeja.

El ItemStore no participa en Mi dispositivo salvo que una acción futura importe explícitamente un original a la bandeja.

## Miniaturas, exportación y borrado — 2026-09-23

```text
DeviceBrowser
   ├─→ ThumbnailLoader ─→ ContentResolver / media decoder
   ├─→ InstalledAppExporter ─→ sourceDir + splitSourceDirs ─→ cache + FileProvider
   └─→ DeviceContentRepository.requestDelete ─→ delete directo / confirmación MediaStore cuando aplica

DocumentsProvider
   └─→ ThumbnailLoader ─→ thumbnail cache ─→ openDocumentThumbnail

OclAx ItemCard
   └─→ ThumbnailLoader ─→ copia privada ItemStore
```

Responsabilidades:
- `ThumbnailLoader` limita miniaturas a imágenes/video/PDF, renderiza solo la primera página de PDF y mantiene caché de memoria acotada.
- `InstalledAppExporter` prepara copias temporales del código APK; no conoce datos privados de apps.
- `DeviceContentRepository` ejecuta el borrado con la URI MediaStore seleccionada; en Android 11+ usa eliminación directa bajo acceso amplio y, si un medio exige confirmación, genera una solicitud con la URI específica de Imagen/Video/Audio.
- `ViewModePreferences` persiste lista/cuadrícula por categoría sin mezclarlo con reglas de dominio.
