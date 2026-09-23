# SEC-001 — Estado de protecciones del repositorio

**Fecha:** 2026-09-22

## Verificado

- Repositorio público.
- Ruleset `Protect main` activo para la rama principal.
- GitHub Secret Scanning se ejecuta automáticamente y gratis en repositorios públicos.
- GitHub ofrece CodeQL/code scanning gratis para repositorios públicos.
- Rulesets/branch protections están disponibles para repositorios públicos con GitHub Free.

## Limitación de la integración actual

El conector de GitHub disponible puede leer el listado de rulesets, pero no dispone de acción administrativa para crear/editar rulesets ni cambiar branch protection.

El endpoint de branch protection devuelve acceso denegado para la integración administrada.

La protección de `main` fue activada manualmente por el dueño. La integración sigue sin permiso administrativo para modificarla.

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
