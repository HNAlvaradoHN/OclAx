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

## PENDING

### SEC-001 — Configurar protecciones del repositorio público
**Estado:** PENDING  
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
**Estado:** PENDING

Definir exactamente qué entra en la primera versión funcional y qué queda fuera para evitar sobreingeniería.
