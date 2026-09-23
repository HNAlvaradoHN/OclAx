# ARCHITECTURE

## Estado

Arquitectura conceptual inicial. No hay código todavía.

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

La selección concreta de librerías y versiones se decidirá y verificará antes de crear la base Android.
No se considera aprobada una dependencia solo por haber sido mencionada en una conversación.


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

Syncthing es el motor candidato, encapsulado detrás de una capa propia:

```text
UI: Enviar a dispositivo
        ↓
TransferService / Domain
        ↓
SyncthingAdapter
        ↓
motor Syncthing local
        ↓
dispositivo OclAx emparejado
```

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
