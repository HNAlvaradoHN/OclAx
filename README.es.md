# OclAx

**Español** · [English](README.md)

**OclAx = Open Content, Local Access & eXchange**

OclAx es una bandeja Android local para recibir, organizar, reutilizar y elegir contenido rápidamente entre aplicaciones.

**Tu contenido, listo donde lo necesitás.**

## Qué hace OclAx

Compartí texto, imágenes o archivos hacia OclAx y la aplicación conserva una copia privada local para que puedas encontrarla, abrirla, copiarla o volver a compartirla rápidamente.

```text
Compartir texto / imagen / archivo
        ↓
       OclAx
        ↓
copia privada temporal + categorías + búsqueda
        ↓
copiar / abrir / compartir / elegir desde otra app
```

OclAx también incluye **Mi dispositivo**, donde podés explorar archivos reales y aplicaciones instaladas después de conceder el acceso requerido por Android.

## Funciones principales

- integración con Android Share Sheet;
- almacenamiento privado local y temporal;
- autolimpieza configurable y Fijados;
- búsqueda y categorías visuales;
- selección múltiple para compartir, fijar/desfijar y eliminar copias OclAx;
- selección múltiple en Mi dispositivo para compartir archivos o aplicaciones instaladas;
- copia automática al portapapeles de texto e imágenes compartidos;
- miniaturas de imagen, video y PDF;
- renombrado;
- integración DocumentsProvider de solo lectura;
- selector propio **Elegir con OclAx** para otras aplicaciones;
- exportación segura de paquetes APK instalados sin datos privados de las aplicaciones;
- limpieza inteligente de capturas recientes: si una captura verificable se comparte dentro de los primeros 60 segundos, OclAx puede borrar también el original cuando venza la copia OclAx;
- interfaz en español e inglés según el idioma de Android.

## Privacidad y red

OclAx está diseñada para funcionar localmente.

- No declara permiso `INTERNET`.
- No usa telemetría.
- No incluye transferencia LAN, emparejamiento ni runtime Syncthing.
- El contenido compartido permanece local en el dispositivo.
- La exportación de aplicaciones usa solamente sus APK de instalación, nunca sus datos privados.

## Permisos

Algunas funciones de **Mi dispositivo** necesitan acceso amplio de Android:

- `MANAGE_EXTERNAL_STORAGE`: explorar y administrar archivos reales;
- `QUERY_ALL_PACKAGES`: listar aplicaciones instaladas;
- `REQUEST_INSTALL_PACKAGES`: flujo explícito de instalación de APK;
- `RECEIVE_BOOT_COMPLETED`: reprogramar la autolimpieza local después de reiniciar.

La bandeja privada de OclAx continúa funcionando aunque no se conceda el acceso amplio a archivos.

## Idiomas

OclAx actualmente soporta:

- español;
- inglés.

La interfaz sigue el idioma del sistema/aplicación de Android.

## Instalación

La distribución pública está prevista mediante **GitHub Releases**.

Cuando exista una versión de producción firmada:

1. abrí la versión más reciente en GitHub Releases;
2. descargá el APK de OclAx;
3. autorizá la instalación desde el navegador/administrador de archivos cuando Android lo solicite;
4. instalá o actualizá OclAx.

Instalá APK únicamente desde este repositorio oficial.

## Desarrollo

Requisitos:

- JDK 17;
- Android SDK 36;
- Gradle 9.6.0.

```bash
gradle :app:testDebugUnitTest
gradle :app:lintDebug
gradle :app:assembleDebug
gradle :app:assembleRelease
```

Antes de modificar el proyecto, leer `AGENTS.md`.

## Estado de release

La candidata Android local ya superó los gates técnicos y de QA físico actuales. La firma de producción y la primera publicación en GitHub Releases siguen siendo pasos separados.
