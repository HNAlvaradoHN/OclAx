# RELEASE_CHECKLIST — OclAx

## Estado

**Objetivo:** preparar una candidata técnica de release sin firmar ni publicar.  
**Estado:** RELEASE_CANDIDATE_READY.

Una release estable no se declara lista solo porque compile. Debe cerrar seguridad, privacidad, dependencias, CodeQL, build release, QA físico y firma/publicación.

## Revisión de seguridad

VERIFICADO:
- aplicación local sin permiso INTERNET ni runtime LAN;
- FileProvider no exportado y limitado a copias OclAx + exportaciones APK temporales;
- DocumentsProvider protegido por `android.permission.MANAGE_DOCUMENTS` y de solo lectura;
- receiver de retención no exportado;
- screenshots solo pueden borrar el original bajo DEC-033 y revalidación fail-closed;
- nombres sanitizados y payload privado con IDs opacos;
- exportación de apps usa únicamente `sourceDir` + `splitSourceDirs`, nunca `dataDir`;
- búsquedas de secretos no encuentran credenciales/llaves embebidas.

Hardening de esta revisión:
- límite de 100 URI por operación entrante de Share Sheet para acotar abuso de la actividad exportada, validado antes de iterar lotes crudos cuando Android expone su tamaño;
- test unitario del límite;
- Kotlin tooling actualizado de 2.4.10 a 2.4.21 porque 2.4.10 está afectado por CVE-2026-53914;
- CodeQL añadido con `java-kotlin`.

VERIFICADO:
- Android CI verde en PR #90 y PR #91;
- CodeQL verde en PR #90 y PR #91;
- no quedaron hallazgos bloqueantes abiertos para la candidata técnica.

## Privacidad

VERIFICADO:
- `allowBackup=false`;
- metadata de origen vive en almacenamiento privado;
- no hay telemetría ni red;
- URIs/nombres privados no se registran en logs;
- sharing/export usa grants temporales de lectura;
- datos privados de apps instaladas no se exportan;
- originales solo se modifican por acciones explícitas o la excepción aprobada de screenshot reciente.

## Arquitectura / Android

VERIFICADO:
- UI delega persistencia a ItemStore y archivos reales a DeviceContentRepository;
- política de screenshot está separada de UI;
- AlarmManager no usa alarma exacta;
- flujo BOOT reprograma limpieza;
- pickers y DocumentsProvider fueron validados físicamente;
- renombrado y borrado de originales fueron validados físicamente;
- LAN/Syncthing no deja runtime activo.

Riesgo informativo:
- DocumentsProvider puede enumerar inventarios grandes; no hay evidencia actual de fallo tras el hardening de Aplicaciones.

## Calidad / limpieza

VERIFICADO:
- sin TODO/FIXME de producto abandonados;
- sin Log/println/printStackTrace de datos;
- sin WebView, ejecución de procesos, sockets o clientes HTTP;
- Dependabot está configurado para Gradle y GitHub Actions;
- main está protegido por ruleset y PR obligatorio.

## Dependencias y licencias

Directas:
- AndroidX Core / Activity / Compose: Apache-2.0;
- Android Gradle Plugin: Apache-2.0;
- Kotlin tooling: Apache-2.0;
- JUnit 4.13.2: EPL-1.0, solo tests.

Estado de vulnerabilidades directas revisadas:
- AndroidX Core 1.18.0: sin vulnerabilidad conocida en la revisión;
- Activity Compose 1.13.0: sin vulnerabilidad conocida en la revisión;
- Compose BOM 2026.04.01: sin vulnerabilidad conocida en la revisión;
- Kotlin 2.4.10: **afectado por CVE-2026-53914**; corregido en esta rama al actualizar a 2.4.21.

Decisión deliberada:
- no actualizar dependencias solo por novedad durante cierre de release si no corrige un riesgo concreto.

Pendiente legal/gobernanza:
- el repositorio no declara una licencia propia. No se inventa una licencia del proyecto sin decisión del dueño.
- antes de presentar el código como open source reutilizable, el dueño debe elegir licencia o mantener explícitamente todos los derechos reservados.

## CI / análisis

Android CI debe ejecutar:
- `:app:testDebugUnitTest`;
- `:app:lintDebug`;
- `:app:assembleDebug`;
- `:app:assembleRelease`.

Artefactos esperados:
- `OclAx-debug`;
- `OclAx-release-unsigned`.

Los artefactos usan `actions/upload-artifact@v7` para evitar el runtime Node.js 20 deprecado.

CodeQL:
- Java/Kotlin con suite `security-extended`;
- PR, push a main y ejecución semanal;
- permisos mínimos: `contents: read` + `security-events: write`.

## QA físico final

Ya verificado:
- flujos principales OclAx/Mi dispositivo;
- selección múltiple;
- apps instaladas y split APK;
- Android Files/SAF;
- retención y screenshots;
- renombrado;
- modo oscuro observado físicamente.

VERIFICADO físicamente:
- modo claro;
- pantalla pequeña mediante ventana estrecha/Formato libre, con scroll y controles utilizables;
- regresión rápida del APK final: compartir, categorías, búsqueda, Mi dispositivo, abrir/compartir y Atrás;
- icono propio OclAx visible correctamente.

## Firma y publicación

La candidata generada por CI es **release unsigned**.

No se reutiliza automáticamente la firma estable de pruebas como firma de producción.

Canal de distribución elegido por el dueño: **GitHub Releases** con APK de producción firmado.

No se requiere revisión de políticas de Google Play para el canal elegido actual. Los permisos sensibles siguen documentados públicamente porque afectan confianza e instalación, aunque no se publique en Play Store.

Antes de la primera GitHub Release:
- completar validación bilingüe inglés/español;
- crear/definir firma de producción protegida;
- generar y verificar el APK firmado;
- publicar notas de versión e instrucciones de instalación;
- no subir keystore, contraseña ni material privado de firma.

Crear o usar una clave de producción implica material criptográfico sensible y no se hace automáticamente desde esta revisión.

## Criterio de candidata técnica

**RELEASE_CANDIDATE_READY — VERIFICADO.**

Criterios cerrados:
1. Android CI verde;
2. CodeQL sin hallazgos bloqueantes;
3. release unsigned generado correctamente;
4. QA físico final validado;
5. memoria oficial reconciliada en este cierre.

La firma y publicación son un gate separado.
