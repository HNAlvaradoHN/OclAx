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
**Estado:** RESUELTO

**Síntoma:** Android muestra “No se instaló la app debido a un conflicto con un paquete” al intentar instalar un APK debug nuevo sobre una instalación anterior de OclAx.

**Causa verificada:** el `applicationId` permanece igual (`io.github.hnalvaradohn.oclax`), pero el workflow genera APK debug en runners efímeros de GitHub Actions sin una clave de firma estable configurada. Cada runner puede crear un debug keystore distinto, por lo que Android rechaza la actualización por firma diferente.

**Impacto:** desinstalar la versión anterior permite instalar la nueva, pero borra las copias privadas y preferencias de OclAx. No afecta archivos originales externos del dispositivo.

**Corrección aplicada:** firma de pruebas persistente almacenada únicamente mediante GitHub Actions Secrets. El workflow reconstruye el keystore en el runner, usa un versionCode monotónico y generó correctamente un APK firmado desde main.

**Validación física:** el dueño instaló una build posterior encima de la instalación firmada establemente y confirmó que Android permitió la actualización y conservó los datos internos.


### ERR-005 — Aplicaciones instaladas no visibles tras primera implementación
**Estado:** RESUELTO_PENDIENTE_VALIDACION_FISICA

**Síntoma:** tras actualizar físicamente, la categoría de apps instaladas no mostró las aplicaciones esperadas y el selector de archivos del sistema siguió mostrando únicamente elementos recientes sin organización por categorías.

**Causa probable verificada en implementación:** la primera versión consultaba actividades lanzables mediante PackageManager y el DocumentsProvider seguía plano. La UI además usaba etiquetas demasiado parecidas entre APK y aplicaciones instaladas.

**Corrección aplicada:**
- usar `LauncherApps.getActivityList()` para obtener actividades lanzables del perfil actual;
- separar visualmente `Apps instaladas` y `APK guardados`;
- añadir carpetas virtuales por categoría en DocumentsProvider manteniendo también los elementos recientes directos;
- eliminar la declaración `<queries>` que dejó de ser necesaria.

**Validación pendiente:** confirmar en el mismo teléfono que Apps instaladas muestra iconos/nombres y que Archivos → OclAx presenta carpetas de categorías sin perder acceso directo a recientes.


### ERR-006 — Primera CI de Mi dispositivo falló al materializar apps y por lint de visibilidad total
**Estado:** RESUELTO

**Síntomas:**
- la primera compilación devolvía `Sequence<InstalledAppInfo>` donde el contrato exigía `List<InstalledAppInfo>`;
- después de corregirlo, lint bloqueó `QUERY_ALL_PACKAGES` con `QueryAllPackagesPermission`.

**Causa:**
- faltaba materializar la secuencia ordenada con `.toList()`;
- lint no puede inferir que la visibilidad completa de paquetes es una decisión explícita y documentada del producto distribuido fuera de Play Store.

**Solución:**
- materializar explícitamente la lista;
- mantener `QUERY_ALL_PACKAGES` por decisión DEC-019/DEC-023 y añadir una supresión localizada `tools:ignore="QueryAllPackagesPermission"` únicamente en esa declaración.

**Prevención:** los permisos amplios deben estar justificados en DECISIONS/SECURITY y cualquier supresión de lint debe ser puntual, visible y asociada a una decisión explícita; no desactivar lint globalmente.

### ERR-007 — Lint bloqueó borrado compatible con Android 10
**Estado:** RESUELTO

**Síntoma:** la CI del PR #34 compiló, ejecutó tests y generó el APK, pero lint bloqueó el cambio porque `RecoverableSecurityException` requiere API 29 mientras OclAx mantiene minSdk 26.

**Causa:** la primera implementación capturaba directamente una excepción introducida en Android 10 dentro de un método accesible para todas las versiones soportadas.

**Solución:** separar el borrado por versión: Android 11+ usa `MediaStore.createDeleteRequest`; Android 10 usa un método anotado para API 29 que maneja `RecoverableSecurityException`; Android 8/9 usa la ruta legacy.

**Prevención:** encapsular APIs Android introducidas después de minSdk en métodos explícitamente versionados/anotados en lugar de confiar solo en ramas internas.
