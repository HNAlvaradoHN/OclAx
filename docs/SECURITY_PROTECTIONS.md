# SEC-001 — Estado de protecciones del repositorio

**Fecha:** 2026-09-22

## Verificado

- Repositorio público.
- No hay rulesets configurados actualmente.
- GitHub Secret Scanning se ejecuta automáticamente y gratis en repositorios públicos.
- GitHub ofrece CodeQL/code scanning gratis para repositorios públicos.
- Rulesets/branch protections están disponibles para repositorios públicos con GitHub Free.

## Limitación de la integración actual

El conector de GitHub disponible puede leer el listado de rulesets, pero no dispone de acción administrativa para crear/editar rulesets ni cambiar branch protection.

El endpoint de branch protection devuelve acceso denegado para la integración administrada.

Por tanto, la configuración de protección de main requiere una acción administrativa manual del dueño en GitHub o una herramienta futura con permiso administrativo explícito.

## Configuración objetivo para main

Cuando el dueño la active:
- requerir Pull Request antes de merge;
- impedir force-push;
- impedir borrado de main;
- requerir conversaciones resueltas cuando aplique;
- requerir checks solo después de que existan checks estables;
- no exigir aprobación de otra persona mientras el proyecto tenga un único mantenedor, salvo decisión posterior.

## Después de crear el proyecto Android

Configurar:
- Dependabot;
- CodeQL;
- build/lint/test CI;
- checks requeridos en main una vez que sus nombres sean estables.

No activar servicios pagos.
