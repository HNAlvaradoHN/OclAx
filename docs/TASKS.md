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

Objetivo:
- revisar Secret Scanning / Push Protection disponibles;
- revisar Dependabot;
- definir protección de main / PR;
- preparar CI mínimo cuando exista código;
- usar únicamente opciones sin costo salvo autorización.

### APP-001 — Inicializar base Android
**Estado:** PENDING  
**Dependencia:** SEC-001

Objetivo:
- crear proyecto Android mínimo;
- separar UI/theme/domain/data/platform desde el inicio;
- mantener cero secretos;
- establecer build y pruebas básicas.

### PRODUCT-001 — Formalizar alcance MVP
**Estado:** DONE

Alcance central confirmado:
- OclAx funciona como bandeja temporal universal;
- entrada rápida y explícita de contenido;
- salida principal mediante `+ / Archivos → OclAx → Recientes → insertar`;
- contenido mixto;
- DocumentsProvider como pieza central;
- cero red;
- autolimpieza y fijado;
- sin IME/Accessibility/Shizuku en el MVP.

La compatibilidad real se validará posteriormente en dispositivo.
