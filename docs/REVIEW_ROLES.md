# REVIEW_ROLES — Revisores y auditorías de OclAx

AGENTS.md es la constitución y tiene prioridad. Este archivo define el detalle operativo de los revisores.

## Principios

Los roles son perspectivas independientes de revisión. No requieren contratar múltiples servicios de IA.

Un revisor:
- busca problemas reales;
- no modifica por iniciativa propia salvo delegación explícita;
- no sustituye tests, lint, build ni CI;
- no ignora AGENTS.md;
- no revela secretos ni datos privados;
- no usa servicios pagos sin autorización;
- no inventa defectos para justificar su existencia.

## Formato obligatorio de hallazgo

**Severidad:** BLOQUEANTE / NO BLOQUEANTE / INFORMATIVO  
**Área:** archivo, componente, flujo o decisión  
**Evidencia:** hecho observable, prueba, diff o comportamiento  
**Riesgo:** qué puede salir mal  
**Recomendación:** cambio mínimo razonable  
**Validación:** cómo comprobar la corrección

### BLOQUEANTE
- seguridad o privacidad;
- pérdida/corrupción de datos;
- build/tests críticos;
- contrato roto;
- dependencia base;
- flujo principal inutilizable.

### NO BLOQUEANTE
- mejora real que no impide continuar;
- limpieza;
- mantenibilidad;
- UX secundaria;
- optimización no crítica.

### INFORMATIVO
Observación útil sin acción inmediata necesaria.

## Coordinador

Responsabilidades:
- seleccionar revisores aplicables;
- entregar contexto mínimo;
- evitar revisiones redundantes;
- consolidar hallazgos;
- resolver contradicciones con evidencia;
- registrar acciones útiles;
- mantener el orden del proyecto.

## Seguridad

Revisar:
- secretos;
- permisos;
- autenticación/autorización;
- exposición de componentes;
- validación de entradas;
- ejecución de archivos;
- path traversal e inyecciones;
- intents/deep links/WebView cuando aplique;
- almacenamiento inseguro;
- datos sensibles en logs;
- GitHub Actions;
- supply chain;
- dependencias vulnerables;
- abuso razonablemente previsible.

Preguntas:
- ¿qué controla un atacante?
- ¿qué puede leer, escribir o ejecutar?
- ¿qué privilegio es innecesario?
- ¿qué dato sensible podría filtrarse?

## Privacidad

Revisar:
- PII;
- metadatos;
- telemetría;
- logs;
- capturas;
- fixtures;
- rutas locales;
- retención;
- backups;
- exportaciones;
- permisos;
- datos de terceros.

Preguntas:
- ¿guardamos más de lo necesario?
- ¿podemos evitar recolectarlo?
- ¿cuándo se elimina?
- ¿puede llegar al repo, CI o reportes?

## Arquitectura

Revisar:
- límites UI/theme/domain/data/platform;
- dependencias;
- contratos;
- responsabilidades;
- acoplamiento;
- duplicación;
- sobre-modularización;
- lógica de negocio en UI;
- deuda estructural;
- reemplazos incompletos.

Preguntas:
- ¿el cambio está en la capa correcta?
- ¿un cambio visual toca lógica sin necesidad?
- ¿se creó abstracción sin beneficio?
- ¿hay dos fuentes de verdad?

## Plataforma / Android

Revisar:
- lifecycle;
- permisos;
- exported components;
- intents;
- ContentProvider/DocumentsProvider/FileProvider;
- URI grants;
- almacenamiento;
- background work;
- compatibilidad API;
- configuración release;
- batería/memoria;
- comportamiento real del sistema.

## Calidad / Limpieza

Revisar:
- código muerto;
- archivos viejos;
- imports;
- recursos huérfanos;
- duplicados;
- nombres;
- complejidad accidental;
- TODO/FIXME abandonados;
- flags permanentes;
- hacks;
- dependencias innecesarias;
- reemplazos que dejaron lo viejo oculto.

## Rendimiento

Revisar solo cuando exista riesgo o evidencia:
- memoria;
- CPU;
- I/O;
- red;
- almacenamiento;
- inicio;
- trabajo de fondo;
- listas/archivos grandes;
- cachés;
- fugas;
- operaciones repetidas.

Todo hallazgo de rendimiento debe indicar evidencia o escenario medible.

## QA / Testing

Revisar:
- camino feliz;
- errores;
- límites;
- datos inválidos;
- permisos negados;
- falta de espacio;
- archivos corruptos;
- concurrencia;
- reinicio de proceso;
- persistencia;
- limpieza;
- regresiones;
- recuperación.

Distinguir: probado / no probado / no aplicable.

## Release

Antes de release candidata comprobar:
- build release;
- tests;
- lint/análisis;
- permisos;
- debugging;
- secretos;
- firma;
- artefactos;
- dependencias;
- documentación;
- migraciones;
- privacidad;
- rollback.

No aprobar release con CI crítico fallando.

## Diseño / UX / Accesibilidad

Aplicar cuando exista UI relevante.

Revisar:
- consistencia con DESIGN.md;
- jerarquía visual;
- navegación;
- estados;
- feedback;
- contraste;
- foco;
- tamaños táctiles;
- lectores de pantalla;
- textos;
- componentes;
- comportamiento adaptativo;
- que cambios cosméticos no contaminen lógica.

Gustos subjetivos menores no son BLOQUEANTES salvo que afecten accesibilidad o usabilidad real.

## Matriz de activación

Cambio visual:
- Diseño/UX;
- Calidad si hay reemplazo/limpieza.

Datos/almacenamiento:
- Arquitectura;
- Seguridad;
- Privacidad;
- QA.

Android/plataforma:
- Plataforma;
- Seguridad;
- QA.

Nueva dependencia:
- Seguridad;
- Calidad;
- Plataforma cuando aplique.

Workflow/CI:
- Seguridad;
- Calidad;
- QA del pipeline.

Refactor:
- Arquitectura;
- Calidad;
- QA.

Optimización:
- Rendimiento;
- QA;
- Arquitectura si cambia diseño interno.

Release:
- Release;
- Seguridad;
- Privacidad;
- QA;
- demás roles aplicables.

## Cierre

El coordinador consolida duplicados y registra solo acciones útiles.

Si un BLOQUEANTE es válido, se corrige antes de continuar.
Si es NO BLOQUEANTE, se registra y agrupa.
Si dos revisores discrepan, gana la evidencia; seguridad y privacidad tienen prioridad.
