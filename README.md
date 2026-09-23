# OclAx

**OclAx = Open Content, Local Access & eXchange**

**Concepto:** una bandeja local y temporal para guardar, ordenar y volver a insertar contenido rápidamente entre aplicaciones Android.

**Idea de marca:** _Tu contenido, listo donde lo necesitás._

## Flujo principal

```text
Compartir texto / imagen / archivo → OclAx → Recientes

Texto / imagen:
OclAx → queda guardado + listo en el portapapeles

Cualquier archivo:
otra app → + / Adjuntar → Archivos → OclAx → Recientes → insertar
```

## Estado actual

Primera prueba vertical Android implementada:

- recepción mediante Android Share Sheet;
- almacenamiento privado y local;
- texto e imagen publicados al portapapeles;
- archivos expuestos mediante `DocumentsProvider`;
- búsqueda básica;
- CI con tests, lint y APK debug.

La compatibilidad con aplicaciones reales todavía requiere validación física en dispositivo.

## Desarrollo

Requisitos: JDK 17, Android SDK 36, Build Tools 36.0.0 y Gradle 9.6.0.

```bash
gradle :app:testDebugUnitTest
gradle :app:lintDebug
gradle :app:assembleDebug
```

Antes de modificar el proyecto, leer `AGENTS.md`.
