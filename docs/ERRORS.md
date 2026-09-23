# ERRORS

## Abiertos

Ninguno registrado actualmente.

## Resueltos

### ERR-001 — Ciclo de bootstrap sin AGENTS.md
**Estado:** RESUELTO

**Síntoma:** un repositorio nuevo no podía cumplir la regla "leer AGENTS.md antes de modificar" porque AGENTS.md todavía no existía.

**Causa:** faltaba una excepción explícita para la primera inicialización de un repositorio vacío.

**Solución:** permitir un bootstrap limitado exclusivamente a instalar gobernanza, identidad y memoria oficial. Durante bootstrap no se desarrolla código de producto.

**Prevención:** los futuros paquetes maestros deben incluir esta excepción de bootstrap.
