# OclAx

**OclAx = Open Content, Local Access & eXchange**

**Concepto:** una bandeja Android local y temporal para recibir contenido, organizarlo, acceder a archivos reales del dispositivo y volver a usarlo rápidamente entre aplicaciones.

**Idea de marca:** _Tu contenido, listo donde lo necesitás._

## Flujo principal

```text
Compartir texto / imagen / archivo
        ↓
       OclAx
        ↓
copia temporal + categorías + búsqueda
        ↓
copiar / abrir / compartir / elegir desde otra app
```

OclAx también ofrece **Mi dispositivo** para explorar archivos reales y aplicaciones cuando el usuario concede el acceso correspondiente.

## Funciones actuales

- recepción mediante Android Share Sheet;
- almacenamiento privado temporal;
- Fijados y retención configurable;
- búsqueda y categorías visuales;
- selección múltiple de copias OclAx para compartir, fijar/desfijar y eliminar en lote;
- portapapeles para texto/imagen;
- abrir/compartir contenido;
- miniaturas de imagen/video/PDF;
- Mi dispositivo;
- renombrar archivos;
- DocumentsProvider;
- picker propio **Elegir con OclAx**;
- limpieza inteligente de capturas recién creadas: si una screenshot verificable se comparte dentro de 60 s, puede limpiarse también su original cuando venza la retención;
- CI con tests, lint y APK debug.

## Sin red

OclAx no incluye transferencia entre dispositivos, LAN ni Syncthing. Ese bloque fue retirado del producto y del código activo.

## Desarrollo

Requisitos: JDK 17, Android SDK 36 y Gradle 9.6.0.

```bash
gradle :app:testDebugUnitTest
gradle :app:lintDebug
gradle :app:assembleDebug
```

Antes de modificar el proyecto, leer `AGENTS.md`.
