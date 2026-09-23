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

### ERR-003 — OOXML se clasificaba como texto por contener `xml`
**Estado:** RESUELTO

**Síntoma:** Android CI del PR #20 falló en `ContentTypeTest` al clasificar un documento Word `.docx` como `TEXT` en vez de `DOCUMENT`.

**Causa:** la regla genérica de texto (`mime.contains("xml")`) se evaluaba antes que la regla específica de documentos. El MIME OOXML de Word contiene `openxmlformats`, por lo que coincidía prematuramente con texto.

**Solución:** evaluar MIME de documentos antes de las reglas genéricas JSON/XML.

**Prevención:** ordenar clasificadores desde los tipos más específicos hacia los más generales y mantener pruebas con MIME reales representativos.


### ERR-004 — APK debug de CI no puede actualizar instalación anterior
**Estado:** RESUELTO_PENDIENTE_VALIDACION_FISICA

**Síntoma:** Android muestra “No se instaló la app debido a un conflicto con un paquete” al intentar instalar un APK debug nuevo sobre una instalación anterior de OclAx.

**Causa verificada:** el `applicationId` permanece igual (`io.github.hnalvaradohn.oclax`), pero el workflow genera APK debug en runners efímeros de GitHub Actions sin una clave de firma estable configurada. Cada runner puede crear un debug keystore distinto, por lo que Android rechaza la actualización por firma diferente.

**Impacto:** desinstalar la versión anterior permite instalar la nueva, pero borra las copias privadas y preferencias de OclAx. No afecta archivos originales externos del dispositivo.

**Corrección aplicada:** firma de pruebas persistente almacenada únicamente mediante GitHub Actions Secrets. El workflow reconstruye el keystore en el runner, usa un versionCode monotónico y generó correctamente un APK firmado desde main.

**Validación pendiente:** instalar una vez el APK con la nueva firma y luego comprobar que una build posterior se instala encima sin conflicto. La instalación anterior a esta migración no comparte la nueva firma y debe desinstalarse una última vez.
