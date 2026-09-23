# TASKS

## DONE

### BOOT-001 — Instalar gobernanza del proyecto
**Estado:** DONE

Incluye:
- AGENTS.md;
- identidad OclAx 📲;
- memoria oficial;
- política de seguridad y privacidad;
- protocolo de sesión y handoff.

### GOV-002 — Evolucionar gobernanza a protocolo V2
**Estado:** DONE

Incluye:
- excepción formal de bootstrap;
- REVIEW_ROLES separado de AGENTS.md;
- DESIGN.md como memoria visual;
- TOOLS.md para herramientas externas;
- rol Diseño / UX / Accesibilidad;
- protocol_version actualizado a 2.

## PENDING

### SEC-001 — Configurar protecciones del repositorio público
**Estado:** IN_PROGRESS  
**Prioridad:** alta

Completado:
- ruleset activo en main;
- PR obligatorio;
- borrado y force-push bloqueados;
- Secret Scanning/Push Protection revisados para repo público;
- Dependabot configurado;
- CI mínimo Android instalado.

Pendiente:
- CodeQL/code scanning antes de release estable;
- mantener solo opciones sin costo salvo autorización.

### APP-001 — Inicializar base Android
**Estado:** IMPLEMENTED_PENDING_VALIDATION  
**Dependencia:** SEC-001

Implementado:
- proyecto Android mínimo;
- separación entre UI, almacenamiento, compartir y provider;
- Share Sheet para contenido mixto;
- portapapeles para texto e imagen;
- DocumentsProvider para Recientes;
- tests/lint/build en CI;
- APK debug como artefacto.

Validado:
- CI verde en PR;
- CI verde en main;
- APK debug generado;
- DocumentsProvider visible en el selector de archivos del sistema en dispositivo real.

Pendiente:
- validación física completa de compartir, pegar y seleccionar contenido entre aplicaciones.

### PRODUCT-001 — Formalizar alcance MVP
**Estado:** DONE

Alcance central confirmado:
- OclAx funciona como bandeja temporal universal;
- entrada rápida y explícita de contenido;
- texto/imagen compartidos a OclAx quedan en Recientes y se publican al portapapeles;
- cualquier archivo queda disponible mediante `+ / Archivos → OclAx → Recientes → insertar`;
- contenido mixto;
- DocumentsProvider como pieza central;
- cero red;
- autolimpieza y fijado;
- sin IME/Accessibility/Shizuku en el MVP.

La compatibilidad real se validará posteriormente en dispositivo.

### PRODUCT-002 — Diseñar selector ordenado de inserción
**Estado:** PENDING

Objetivo:
- búsqueda rápida;
- Recientes;
- Fijados;
- filtros por tipo;
- miniaturas/iconos útiles;
- mínimo de toques desde `+ / Archivos → OclAx`.

No convertir esta tarea en un gestor de archivos completo.
