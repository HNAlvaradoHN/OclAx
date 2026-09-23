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
