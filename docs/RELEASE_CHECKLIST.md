# RELEASE_CHECKLIST — OclAx

## Estado

**Objetivo:** preparar una candidata técnica de release sin firmar ni publicar.  
**Estado:** IMPLEMENTADO_PENDIENTE_CI_Y_QA_FINAL.

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
- límite de 100 URI por operación entrante de Share Sheet para acotar abuso de la actividad exportada;
- test unitario del límite;
- Kotlin tooling actualizado de 2.4.10 a 2.4.21 porque 2.4.10 está afectado por CVE-2026-53914;
- CodeQL añadido con `java-kotlin`.

Pendiente:
- CI verde con el cambio de Kotlin;
- CodeQL sin hallazgos bloqueantes.

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

CodeQL:
- Java/Kotlin;
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

Pendiente:
- modo claro;
- pantalla pequeña / densidad alta;
- regresión rápida del APK generado por main después de esta revisión.

## Firma y publicación

La candidata generada por CI es **release unsigned**.

No se reutiliza automáticamente la firma estable de pruebas como firma de producción.

Para una release pública falta una decisión explícita de distribución:
- APK directo / GitHub Release;
- tienda de aplicaciones;
- otro canal.

Si el canal es Google Play, revisar antes de publicar las políticas aplicables a:
- `MANAGE_EXTERNAL_STORAGE`;
- `QUERY_ALL_PACKAGES`;
- `REQUEST_INSTALL_PACKAGES`.

Crear o usar una clave de producción implica material criptográfico sensible y no se hace automáticamente desde esta revisión.

## Criterio de candidata técnica

Puede marcarse **RELEASE_CANDIDATE_READY** solo cuando:
1. Android CI esté verde;
2. CodeQL no tenga hallazgos bloqueantes;
3. release unsigned se genere correctamente;
4. QA físico pendiente quede validado;
5. memoria oficial quede reconciliada.

La firma y publicación son un gate separado.
