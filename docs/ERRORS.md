# ERRORS

## Estado

No hay un error nuevo confirmado en la renovación local. Las funciones nuevas permanecen `IMPLEMENTED_PENDING_VALIDATION` hasta CI y prueba física.

## Funcionalidad retirada

### ERR-016 / ERR-017 / ERR-018 / ERR-019 — bloque LAN
**Estado:** RETIRADO_CON_FUNCIONALIDAD

Estos errores documentaban discovery, pérdida de peer, estabilidad de conexión y lifecycle del runtime Syncthing.

DEC-032 retiró por completo:
- LAN;
- emparejamiento;
- Syncthing;
- transferencias entre dispositivos;
- servicio foreground;
- pipeline nativo.

Por tanto ya no son bloqueos activos ni deuda pendiente del producto. El historial Git conserva las causas y fixes por si alguna vez se investiga una solución distinta, pero no deben reaparecer en PROJECT_STATE/TASKS como trabajo actual.

---

## Resueltos útiles

### ERR-013 — Parser XML Android bloqueaba el runtime nativo
**Estado:** HISTÓRICO_RESUELTO

Lección conservada:
- no asumir que features Xerces existen uniformemente en Android;
- defensas XML críticas deben tener más de una barrera si una feature del parser es opcional.

El código que motivó este error fue retirado junto con Syncthing.

### ERR-008 — PDF sin portada y borrado de originales con URI genérica
**Estado:** RESUELTO

Lecciones:
- PDF necesita ruta explícita de miniatura con PdfRenderer;
- `MediaStore.createDeleteRequest` no acepta arbitrariamente una URI genérica `MediaStore.Files`;
- APIs de borrado/edición deben respetar el tipo real de URI y la versión Android.

Sigue siendo relevante para **Mi dispositivo**.

### ERR-014 — Panel técnico impedía scroll completo
**Estado:** RESUELTO_Y_COMPONENTE_RETIRADO

El panel de transferencia fue retirado. La lección de mantener una jerarquía de scroll coherente sigue aplicando a las superficies de contenido.

### ERR-012 — Atrás desde categorías cerraba OclAx
**Estado:** RESUELTO

Prevención:
- cada superficie navegable declara su jerarquía Atrás;
- política principal cubierta por `BackNavigationPolicyTest`.

### ERR-004 — APK debug no actualizaba instalación anterior
**Estado:** RESUELTO

Causa:
- firmas debug efímeras de runners distintos.

Solución:
- firma estable de pruebas reconstruida solo desde GitHub Actions Secrets en main;
- PR no recibe secretos.

Validación física:
- una build posterior pudo actualizar la instalación conservando datos.

### ERR-005 — Aplicaciones instaladas no visibles
**Estado:** RESUELTO_PENDIENTE_VALIDACIÓN_AMPLIA

La implementación vigente usa inventario aprobado con `QUERY_ALL_PACKAGES` y excluye OclAx. El usuario confirmó funcionamiento en el dispositivo probado; queda cobertura de inventarios/variantes más amplias.

### ERR-007 — Lint/API de borrado Android 10
**Estado:** RESUELTO

Lección:
- encapsular APIs posteriores a minSdk por versión;
- no suprimir errores de plataforma para “hacer compilar”.

### ERR-003 — OOXML clasificado como texto
**Estado:** RESUELTO

Prevención:
- clasificadores MIME específicos antes de reglas genéricas JSON/XML.

### ERR-012-GOV — contador/handshake no representaba un chat real
**Estado:** RESUELTO

Prevención vigente:
- inventario y lectura exhaustiva antes del primer handshake;
- un número por hilo visible;
- herramientas/reintentos/PR/CI no incrementan;
- registro canónico `last_confirmed_chat`.

---

## Riesgos a observar en la renovación

Estos son **riesgos de prueba**, no errores confirmados:

1. un OEM podría compartir una captura mediante un provider no MediaStore; en ese caso la política fail-closed conserva el original;
2. Android podría negar `ContentResolver.update/delete` pese a acceso amplio; debe informarse/conservar original, no buscar atajos;
3. AlarmManager puede diferir la limpieza bajo ahorro de energía;
4. DocumentsProvider puede enumerar un inventario grande con coste de I/O; medir antes de optimizar;
5. la UI de Files no puede ser sustituida por Compose desde un DocumentsProvider.

Si uno se reproduce como fallo del contrato aprobado, crear un ERR nuevo con evidencia y causa; no reabrir errores LAN.
