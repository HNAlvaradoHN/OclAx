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

## Selector propio desde otras aplicaciones — ACTION_GET_CONTENT

OclAx mantiene dos integraciones complementarias con el selector de Android:

```text
Otra aplicación
   │
   ├─ ACTION_OPEN_DOCUMENT / selector SAF
   │       ↓
   │  DocumentsProvider OclAx
   │
   └─ ACTION_GET_CONTENT
           ↓
      OclAxPickerActivity
           ↓
   ┌───────┴────────┐
   ↓                ↓
ItemStore     DeviceContentRepository
   └───────┬────────┘
           ↓
     content:// URI
           ↓
   aplicación llamadora
```

Reglas:
- `OclAxPickerActivity` es una superficie exportada solo para `ACTION_GET_CONTENT`; cualquier otra acción termina cancelada;
- la UI del picker vive separada de la Activity y reutiliza ItemStore/Mi dispositivo en vez de duplicar almacenamiento;
- la aplicación llamadora solo recibe el contenido que el usuario selecciona explícitamente;
- las copias privadas OclAx salen mediante FileProvider con permiso temporal de lectura; los archivos de Mi dispositivo conservan su URI de contenido;
- se respeta el MIME solicitado por la aplicación llamadora y se soporta selección múltiple cuando `EXTRA_ALLOW_MULTIPLE` está presente;
- el picker no borra, modifica, ejecuta ni instala contenido;
- `ACTION_OPEN_DOCUMENT` sigue usando la UI del sistema y el DocumentsProvider actual; OclAx no intenta reemplazar el selector SAF.

## Transferencia entre dispositivos

El emparejamiento se divide en dos capas para no abrir red antes de tiempo:

```text
UI: Mis dispositivos
        ↓
PairedDeviceStore (privado)
        ↓
nombre + Device ID + confianza
        ↓
[futuro, tras validar runtime]
SyncthingAdapter / config REST
```

Guardar un dispositivo localmente no modifica el motor ni habilita discovery/relay.

La siguiente capa usa ese registro únicamente durante una prueba explícita:

```text
PairedDeviceStore
        ↓
TransferRuntimeController.connectLan
        ↓
SyncthingLanPolicy
        ├─ peer pausado + allowedNetworks privadas
        ├─ global discovery / relay / NAT = off
        └─ local discovery + TCP listener = on temporal
        ↓
ventana corta de discovery local
        ├─ conecta → validar connected + isLocal
        └─ no conecta
              ↓
        LanDirectProbe
        ├─ solo Wi‑Fi/Ethernet
        ├─ segmento inmediato, máximo /24
        └─ solo TCP/22000
              ↓ candidatos privados temporales
        Syncthing device.addresses
              ↓
Syncthing REST loopback
        ↓
/rest/system/connections → connected + isLocal
```

La prueba no crea carpetas Syncthing ni mueve contenido. Una primera muestra `connected=true` + `isLocal=true` no se considera suficiente: el controlador exige estabilidad consecutiva, toma la IPv4 privada del peer desde esa conexión ya autenticada y la conserva temporalmente como ruta directa junto a `dynamic`. Después revalida la sesión, apaga discovery local, libera el MulticastLock y vuelve a revalidar antes de informar éxito a la UI. Al desconectar o fallar, la dirección se restaura a `dynamic` y el motor vuelve a `enforcePrivateOptions()`.

El fallback directo no sustituye a Syncthing como motor ni autentica peers por IP. Solo encuentra candidatos TCP locales cuando los anuncios broadcast no cruzan la Wi‑Fi; Syncthing sigue aceptando la conexión únicamente si el certificado/Device ID corresponde al dispositivo emparejado. Incluso la ruta fijada después de conectar procede de un peer ya autenticado y nunca reemplaza esa identidad. Las direcciones temporales se eliminan al terminar la prueba.

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
- se construye para las ABI Android soportadas en un job CI separado de secretos de firma y se entrega al job Android como artefacto con checksum verificado;
- se empaqueta como librería nativa extraíble y se ejecuta como proceso hijo desde `applicationInfo.nativeLibraryDir`;
- `SyncthingPrivateConfig` genera/endurece la configuración antes de arrancar: GUI/API y listener BEP solo loopback; discovery/relay/NAT apagados durante el probe local;
- `SyncthingRuntimeService` es un foreground service `dataSync` on-demand, no un daemon permanente;
- `SyncthingRestClient` vuelve a imponer/verificar el perfil privado y controla salud, autenticación, Device ID y apagado únicamente contra `127.0.0.1:8384`;
- Android Network Security Config permite HTTP cleartext solo para `127.0.0.1`/`localhost`; el resto de destinos conserva cleartext bloqueado;
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


## Canal real de archivos LAN — TRANSFER-004

El primer canal real conserva Syncthing como motor, pero OclAx controla el contrato visible y el ciclo de vida.

```text
ItemCard (Enviar a dispositivo)
        ↓
FileTransferCoordinator
        ↓
TransferRuntimeController
        ↓
OclAxTransferChannel
        ├─ staging privado filesDir/oclax/transfers/{outgoing|incoming}
        ├─ carpeta Syncthing efímera oclax-<id>
        ├─ payload.bin
        ├─ .oclax-manifest.json
        └─ .oclax-ack.json
        ↓
SyncthingRestClient (REST loopback autenticado)
        ↓
peer ya verificado connected + isLocal
```

Contratos:
- una transferencia solo se inicia contra el único peer LAN activo;
- el ID de carpeta usa namespace OclAx estricto y no se expone como concepto de producto;
- el receptor solo presenta ofertas del peer emparejado activo y con label OclAx válido;
- aceptación manual es el default; **Permitir sin aceptar** solo aplica al peer guardado que ya pasó la confianza por Device ID;
- el contenido entra primero a staging privado y solo después de validar manifest/tamaño/remitente se importa mediante ItemStore;
- ACK se genera únicamente después de una importación exitosa; recién entonces el emisor considera la transferencia completada;
- carpetas/configuración/staging son efímeros y se retiran tras ACK o error;
- polling de ofertas en la pantalla solo dispara consultas; las reglas de transferencia permanecen en `FileTransferCoordinator`/`OclAxTransferChannel`;
- no aparecen carpetas Syncthing, rutas, IP, REST ni conceptos BEP en la UX de producto.
