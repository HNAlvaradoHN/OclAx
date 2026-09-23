# ERRORS

## Abiertos

Ninguno registrado actualmente.

## Resueltos

### ERR-001 — Ciclo de bootstrap sin AGENTS.md
**Estado:** RESUELTO

**Síntoma:** un repositorio nuevo no podía cumplir la regla "leer AGENTS.md antes de modificar" porque AGENTS.md todavía no existía.

**Causa:** faltaba una excepción explícita para la primera inicialización de un repositorio vacío.

**Solución:** permitir un bootstrap limitado exclusivamente a instalar gobernanza, identidad y memoria oficial. Durante bootstrap no se desarrolla código de producto.

**Prevención:** protocol_version 2 incorpora explícitamente la excepción limitada de bootstrap en AGENTS.md y en el paquete maestro.


### ERR-002 — CI no encontraba/aceptaba Android SDK 37
**Estado:** RESUELTO

**Síntoma:** la primera CI Android fallaba antes de compilar al intentar instalar explícitamente SDK 37; al retirar ese paso, Compose actual exigía compileSdk 37.

**Causa:** se eligió inicialmente un baseline más nuevo de lo necesario para la prueba vertical y un Compose BOM cuya versión requería API 37.

**Solución:** usar API 36, ya disponible y suficiente para el MVP, junto con Compose BOM 2026.04.01 (Compose 1.11), y dejar que el runner use su SDK estable.

**Prevención:** elegir la versión mínima actual que satisfaga producto y dependencias; subir compileSdk por necesidad verificada, no por novedad.

### ERR-003 — Clasificador no reconocía documentos OOXML
**Estado:** RESUELTO

**Síntoma:** Android CI del PR #20 falló en `ContentTypeTest` al clasificar un documento Word `.docx` como `OTHER` en vez de `DOCUMENT`.

**Causa:** el detector buscaba la subcadena `officedocument`, pero el MIME estándar de OOXML contiene `openxmlformats-officedocument` y no esa secuencia contigua.

**Solución:** reconocer explícitamente `openxmlformats-officedocument` dentro de los MIME de documentos.

**Prevención:** mantener pruebas unitarias con MIME reales de formatos representativos antes de reutilizar la clasificación en UI o DocumentsProvider.
