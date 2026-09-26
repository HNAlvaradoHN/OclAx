# ERRORS

## Abiertos

### ERR-018 — Un peer llega a “Conectado por LAN” antes de que la sesión quede estable en ambos extremos
**Estado:** CORRECCIÓN_IMPLEMENTADA_CI_VERDE_PENDIENTE_FÍSICA

**Síntoma físico — 2026-09-25:**
- con main run 296, el móvil llegó a **Motor activo · conexión LAN verificada / Conectado por LAN**;
- la tablet siguió buscando y terminó indicando que encontró un candidato Syncthing por LAN directa pero no logró verificar el peer;
- después, el móvil detectó la pérdida y volvió automáticamente a aislamiento, por lo que la corrección de ERR-017 sí actuó;
- el dueño confirmó que la tablet tiene guardado exactamente el Device ID actual del móvil, descartando esa discrepancia de emparejamiento en ese sentido.

**Corrección de interpretación:**
- el mensaje anterior “no verificó el dispositivo emparejado” era más específico que la evidencia disponible;
- el código solo sabía que el sondeo TCP/22000 había encontrado al menos un candidato y que `/rest/system/connections` no sostuvo una sesión `connected=true` + `isLocal=true`;
- por tanto, ese texto no demostraba por sí solo un Device ID incorrecto ni un fallo TLS concreto.

**Debilidad verificada en código:**
- `connectLan()` consideraba suficiente la primera muestra `connected=true` + `isLocal=true`;
- inmediatamente después apagaba Local Discovery y liberaba el MulticastLock sin comprobar que la sesión sobreviviera a esa transición de configuración;
- si la conexión se renegociaba o caía en ese punto, la UI podía anunciar éxito brevemente y el otro dispositivo seguir sin una sesión estable;
- cuando la primera conexión venía de discovery, OclAx tampoco fijaba temporalmente la dirección IPv4 privada del peer ya autenticado antes de apagar discovery.

**Causa completa del fallo físico:** **NO VERIFICADA.**
La debilidad anterior explica el éxito prematuro y deja una ventana real de inestabilidad, pero la prueba física siguiente debe confirmar si es además la causa suficiente del fallo asimétrico observado.

**Corrección implementada en `fix/lan-post-connect-stability`:**
- exige varias muestras consecutivas de `connected=true` + `isLocal=true` antes de avanzar;
- toma la IPv4 privada del peer desde una conexión ya autenticada por Syncthing y la conserva temporalmente como ruta `tcp4://…:22000` junto a `dynamic`;
- revalida la sesión después de fijar esa ruta y otra vez después de apagar Local Discovery;
- solo entonces devuelve éxito a la UI;
- cualquier fallo conserva el cleanup fail-closed existente;
- el diagnóstico de candidato directo deja de afirmar una causa de identidad que el runtime no había demostrado.

**Validación requerida:**
- **VERIFICADO:** PR #77 CI run 299 verde en runtime nativo, tests, lint, build, verificación del APK y artefacto;
- misma build en móvil y tablet;
- ambos deben llegar a **Conectado por LAN** y permanecer así al menos 10 segundos;
- repetir pérdida deliberada de un extremo y confirmar que ERR-017 sigue aislando al otro;
- solo después continuar TRANSFER-004.

### ERR-017 — Un extremo conserva “Conectado por LAN” después de que el peer falle
**Estado:** CORRECCIÓN_IMPLEMENTADA_CI_VERDE_PENDIENTE_FÍSICA

**Síntoma físico — 2026-09-25:**
- en el móvil, OclAx mostró **Motor activo · conexión LAN verificada / Conectado por LAN** hacia la tablet;
- simultáneamente, la tablet terminó con **LAN no conectó · motor volvió a modo aislado** y diagnóstico de candidato LAN directo encontrado pero Device ID emparejado no verificado;
- por tanto no existía evidencia de un enlace vivo simultáneo en ambos extremos, aunque el móvil seguía presentándolo como conectado.

**Causa verificada en código:**
- `MainActivity` marcaba `activeLanDeviceId` al completar una conexión y solo lo limpiaba por acciones/fallos locales;
- no existía vigilancia posterior de `/rest/system/connections` para detectar que el peer remoto había perdido la sesión o se había aislado;
- el estado visible podía quedar obsoleto y habilitar controles de transferencia sobre una conexión ya inexistente.

**Corrección fusionada en PR #75:**
- el runtime expone una comprobación de conexión LAN que sigue exigiendo `connected=true` + `isLocal=true` para el Device ID emparejado;
- mientras una sesión LAN aparece activa, OclAx verifica periódicamente el enlace;
- dos comprobaciones perdidas consecutivas disparan una revalidación final para evitar falsos positivos transitorios;
- si la pérdida se confirma, OclAx ejecuta el mismo cleanup fail-closed de **Desconectar LAN**, limpia el estado visible y vuelve a modo aislado; si la limpieza falla, el runtime se detiene por seguridad.

**No resuelto todavía:**
- la causa de por qué la tablet no verificó al móvil en ese intento sigue **NO VERIFICADA**;
- primero debe eliminarse el estado fantasma y repetir la prueba controlada para distinguir un fallo real de establecimiento de una UI obsoleta.

**Validación automática:**
- PR #75 fusionado después de CI verde;
- main run 296 verde en runtime nativo, tests, lint, build, verificación del APK y publicación del artefacto.

**Validación física requerida:**
- misma build de main run 296 en móvil y tablet;
- si un extremo falla/abandona la sesión, el otro debe dejar automáticamente **Conectado por LAN** y quedar aislado;
- después, ambos deben mantener simultáneamente una sesión local verificada antes de reanudar TRANSFER-004.

### ERR-016 — Ambos peers LAN sanos pero Local Discovery no cruza
**Estado:** MITIGADO_VALIDADO — NO BLOQUEA

**Síntoma físico — 2026-09-24:**
- con main run 234 en ambos dispositivos, cada uno reporta discovery IPv4 local y listener LAN activos;
- ninguno ve al otro por Syncthing Local Discovery;
- al fallar, ambos vuelven al modo aislado.

**Causa:** **NO VERIFICADA.**
- el filtrado/aislamiento de broadcast de la Wi‑Fi es compatible con la evidencia;
- todavía no se atribuye el fallo al router, ROM ni a Syncthing sin una prueba adicional.

**Mitigación implementada:**
- mantener Local Discovery como primera ruta;
- si no conecta, usar un sondeo directo acotado del segmento Wi‑Fi/Ethernet inmediato, exclusivamente TCP/22000;
- máximo 254 hosts, sin Internet/celular ni exposición de IP;
- entregar candidatos temporales al Syncthing del peer; Syncthing sigue verificando Device ID;
- restaurar `dynamic` y aislamiento al terminar; fail-closed si la limpieza falla.

**Validación física — 2026-09-24:**
- los dos dispositivos emparejados alcanzaron **Conectado por LAN** con la implementación que incluye el fallback;
- cada dispositivo volvió a **Motor aislado / Desconectado · motor aislado** al ejecutar su desconexión local;
- por tanto, el problema histórico de Local Discovery ya no bloquea la conectividad LAN de OclAx;
- la causa raíz de por qué broadcast Local Discovery no cruzó sigue **NO VERIFICADA** y se conserva como dato histórico, no como bloqueo activo.


### ERR-015 — Picker propio funcional pero visualmente desordenado
**Estado:** RESUELTO

**Síntoma físico — 2026-09-24:**
- **Elegir con OclAx** aparece y abre correctamente desde una aplicación externa;
- **Mi dispositivo** muestra archivos, pero mezcla APK, imágenes, texto y otros tipos en una lista plana;
- la pantalla no se percibe consistente con la organización ya validada de la aplicación principal.

**Causa verificada en código:**
- el primer picker solo tenía origen + búsqueda;
- no reutilizaba categorías ni miniaturas de la navegación principal;
- sus botones de origen eran más altos y las tarjetas usaban una presentación propia simplificada.

**Corrección implementada:**
- selector de origen alineado al tamaño de la app;
- categorías por tipo, con Fijados solo para OclAx;
- categoría inicial derivada del MIME solicitado cuando es específico;
- miniaturas reales para Imagen/Video/PDF mediante `ThumbnailLoader`;
- fecha/hora y ruta en Mi dispositivo;
- tarjetas compactas alineadas a la densidad visual de OclAx;
- tests unitarios para matching de categorías y selección inicial.

**Validación:**
- PR #63 terminó con CI verde y fue fusionado;
- main run 229 terminó verde;
- el dueño confirmó físicamente que la presentación corregida **ya aparece bien**.

**Pendientes que no reabren ERR-015:**
- variantes funcionales de selección múltiple/cancelar/permiso negado permanecen en PICKER-001.


### ERR-013 — Runtime Syncthing no responde a tiempo en Android
**Estado:** CORREGIDO_VALIDADO_1_DISPOSITIVO

**Síntoma:**
- en dos teléfonos distintos, `Probar motor` termina con `El motor no respondió a tiempo`;
- después de PR #47 y una nueva build firmada desde `main`, el mismo síntoma continuó en dispositivo real;
- el fallo ocurre antes de obtener Device ID y antes de cualquier prueba LAN.

**Evidencia verificada:**
- el fallo es reproducible en más de un dispositivo;
- builds, tests, lint, runtime multi-ABI y empaquetado pasan CI;
- `STMONITORED=1` + `SQLITE_TMPDIR` pasaron CI pero no eliminaron el timeout físicamente;
- la UI anterior ocultaba si el fallo ocurría al preparar configuración, arrancar el binario, abrir REST o endurecer opciones privadas.

**Causa verificada — 2026-09-23:**
- la build diagnóstica de main llegó a la etapa **preparando la configuración privada** y falló antes de arrancar Syncthing;
- Android rechazó la llamada obligatoria a `DocumentBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)`;
- esa feature específica de Xerces no está disponible de forma uniforme en los parsers XML de Android, por lo que la protección defensiva abortaba el flujo antes de que Syncthing pudiera iniciar;
- la hipótesis anterior de monitor externo/re-exec sigue descartada como solución suficiente.

**Diagnóstico implementado en PR #48:**
- registrar localmente la etapa exacta de arranque;
- conservar código de salida del proceso cuando exista;
- mostrar una única línea de log posterior al inicio del intento;
- sanitizar rutas privadas, Device ID, IP y valores largos antes de mostrarla;
- no añadir permisos, telemetría ni envío de logs;
- tests unitarios cubren el formateo y la sanitización;
- CI del PR terminó verde en tests, lint, build multi-ABI y verificación de runtimes.

**Corrección implementada:**
- las flags XML dependientes del parser pasan a ser defensa en profundidad y no abortan si Android no las soporta;
- antes de parsear, OclAx rechaza explícitamente declaraciones `DOCTYPE` y `ENTITY`;
- el `DocumentBuilder` instala además un `EntityResolver` que rechaza cualquier resolución externa;
- `snapshot()` reutiliza la misma ruta segura;
- se añade regresión unitaria para una feature XML no soportada y se conservan las pruebas de rechazo de XXE/DOCTYPE.

**Validación automática:**
- PR #53 terminó verde en runtime nativo, tests, lint, build multi-ABI y verificación del APK;
- PR #53 fue fusionado en `main`;
- main run 192 terminó verde y publicó una APK firmada estable con el fix.

**Validación física — 2026-09-23:**
- un teléfono con la APK de main run 192 supera la preparación privada;
- **Probar motor** devuelve Device ID y `loopback verificado`;
- falta confirmar el segundo teléfono antes de mover ERR-013 a resuelto.

**Siguiente validación física:**
- instalar la build siguiente en el segundo teléfono;
- confirmar Device ID + loopback;
- solo después continuar con emparejamiento y LAN.


## Resueltos

### ERR-008 — PDF sin portada y borrado de originales rechazado
**Estado:** RESUELTO

**Síntomas observados en teléfono real:**
- las miniaturas de imágenes funcionan, pero los PDF siguen mostrando icono genérico;
- al intentar eliminar un original Android muestra: `All requested items must be Media items`.

**Causa verificada en código:**
- `ThumbnailLoader`, `DeviceFileVisual` y `DocumentsProvider` habilitaban miniaturas solo para Imagen/Video, excluyendo PDF;
- Android 11+ recibía una URI de `MediaStore.Files` en `MediaStore.createDeleteRequest`; esa API exige URIs de colecciones de medios y rechaza la URI genérica `Files`.

**Corrección aplicada:**
- PDF renderiza su primera página con `PdfRenderer` como miniatura, tanto en Mi dispositivo como en el DocumentsProvider;
- con acceso amplio, Android 11+ intenta primero borrar mediante `ContentResolver.delete`;
- si Android exige confirmación para Imagen/Video/Audio, OclAx convierte el elemento a la URI específica de su colección MediaStore antes de crear la solicitud del sistema;
- documentos/PDF no se presentan falsamente como elementos multimedia solo para forzar la confirmación.

**Validación física — 2026-09-24:** el usuario confirmó que PDF muestra portada y que borrar/cancelar funciona correctamente para imagen y PDF/documento en el teléfono probado.

**Prevención:** no asumir que una URI de `MediaStore.Files` es válida para APIs restringidas a elementos multimedia; conservar pruebas físicas por tipo de contenido.


### ERR-014 — El panel técnico de transferencia impide hacer scroll completo en OclAx
**Estado:** RESUELTO

**Síntoma:**
- con el motor ya operativo y un dispositivo emparejado visible, el panel técnico ocupa suficiente altura para que búsqueda, filtros y contenido queden por debajo;
- la pantalla OclAx no permite desplazar verticalmente ese conjunto completo.

**Causa verificada en código:**
- el encabezado y `TransferDevicesSection` estaban fuera del único `LazyColumn`;
- solo la lista final de elementos OclAx era desplazable;
- cuando el panel técnico crecía, el contenido superior consumía el alto disponible y el usuario no podía desplazar el conjunto.

**Corrección implementada:**
- mantener encabezado y selector de fuente fijos;
- usar una única `LazyColumn` para la superficie OclAx;
- incluir dentro del mismo scroll el panel técnico, búsqueda, filtros/retención y tarjetas;
- conservar `DeviceBrowser` separado con su propio comportamiento existente.

**Validación automática:**
- PR #55 fusionado;
- main run 196 verde en runtime nativo, tests, lint, build y APK.

**Validación física — 2026-09-24:**
- con main run 207, el usuario confirmó que el scroll funciona correctamente en el teléfono;
- la pantalla puede desplazarse después de expandirse el panel técnico.


### ERR-012 — Contador de chat derivó a 11 y el handshake no probaba lectura completa
**Estado:** RESUELTO

**Síntomas:**
- el registro persistente terminó en 11 aunque el dueño confirmó que el hilo actual es el chat 2;
- el protocolo permitía pasar a READY después de una lectura progresiva/parcial;
- existían reglas duplicadas fuera del repositorio que podían conservar una cadencia antigua del handshake y contradecir la fuente técnica de verdad.

**Causa:**
- **CAUSA HISTÓRICA EXACTA DEL 2 → 11: DESCONOCIDA.** El historial accesible del issue no conserva las ediciones de cuerpo necesarias para atribuir cada incremento.
- **VERIFICADO:** la definición de “sesión” no distinguía de forma suficientemente fuerte un chat visible del usuario frente a reintentos, resincronizaciones y actividad técnica.
- **VERIFICADO:** la sección de sincronización priorizaba lectura mínima/progresiva, por lo que el handshake no demostraba que se hubiera leído todo el proyecto.
- **VERIFICADO:** existen reglas fundamentales duplicadas fuera del repositorio; cambiar solo GitHub no actualiza automáticamente instrucciones del Project ya cargadas en ChatGPT, lo que permite deriva.

**Corrección:**
- protocol_version 4 exige inventario recursivo y lectura de todos los archivos legibles versionados antes del primer handshake;
- el número queda definido como uno por hilo visible de chat y nunca cambia por actividad técnica; el campo canónico pasa a llamarse `last_confirmed_chat`;
- ante duda, el contador no se incrementa;
- el dueño corrigió el chat actual a **#2**;
- cualquier instrucción externa duplicada debe mantenerse alineada con AGENTS.md o reducirse a un bootstrap que mande releer AGENTS.md.

**Prevención:**
- GitHub conserva la regla canónica;
- las instrucciones del Project de ChatGPT delegan a AGENTS.md las reglas mutables de sincronización, numeración y handshake;
- comprobar el inventario completo antes de READY;
- el registro administrativo ya usa `last_confirmed_chat` y quedó corregido a chat 2 bajo protocol v4.

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

**Causa histórica:** la primera versión se limitó a aplicaciones lanzables y el DocumentsProvider seguía plano. Esa estrategia quedó sustituida cuando el dueño aprobó que **Mi dispositivo** mostrara el inventario completo de aplicaciones.

**Corrección vigente verificada en código y DEC-023:**
- `InstalledAppsRepository` usa `PackageManager.getInstalledApplications(0)` para inventariar aplicaciones visibles al sistema, excluyendo únicamente OclAx;
- `QUERY_ALL_PACKAGES` permanece declarado para el inventario completo aprobado en DEC-023;
- se separan visualmente `Apps instaladas` y `APK guardados`;
- DocumentsProvider mantiene carpetas virtuales por categoría además del acceso a recientes;
- la referencia anterior a `LauncherApps.getActivityList()` ya no describe la implementación vigente.

**Validación pendiente:** confirmar en el mismo teléfono que Apps instaladas muestra el inventario esperado con iconos/nombres y que Archivos → OclAx presenta carpetas de categorías sin perder acceso directo a recientes.


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

**Solución inicial:** separar el borrado por versión: Android 11+ usaba `MediaStore.createDeleteRequest`; Android 10 usa un método anotado para API 29 que maneja `RecoverableSecurityException`; Android 8/9 usa la ruta legacy.

**Evolución posterior:** la prueba física reveló que Android 11+ rechaza la URI genérica de `MediaStore.Files` en `createDeleteRequest`. La corrección vigente está documentada en ERR-008.

**Prevención:** encapsular APIs Android introducidas después de minSdk en métodos explícitamente versionados/anotados y validar físicamente los contratos de URI exigidos por cada API.


### ERR-009 — El spike nativo asumía que sdkmanager estaba en PATH
**Estado:** RESUELTO

**Síntoma:** la primera ejecución aislada de `Syncthing Native Spike` falló antes de descargar o ejecutar código de Syncthing con `sdkmanager: command not found`.

**Causa verificada:** el workflow invocaba `sdkmanager` por nombre y asumía que GitHub Actions lo añadía al `PATH`. El runner no cumplió esa suposición.

**Corrección aplicada:** el workflow reutiliza el NDK pinneado si ya está instalado; en caso contrario localiza `sdkmanager` dentro de `ANDROID_HOME/cmdline-tools`, instala la versión exacta del NDK y publica la ruta del compilador mediante `GITHUB_ENV`.

**Validación:** el segundo run completó NDK, obtención del tag pinneado, compilación arm64 y artefacto. El binario producido es ELF Android arm64/API 26 con NDK r30 y su SHA-256 coincide con el archivo de verificación generado por CI.

**Seguridad:** el primer fallo ocurrió antes de obtener código externo; el job corregido mantiene `contents: read`, no recibe secretos de firma ni otros secretos privilegiados.

**Prevención:** no asumir que herramientas del Android SDK están en `PATH`; resolver rutas desde `ANDROID_HOME` y validar ejecutables antes de usarlos.

### ERR-010 — Lint bloqueó la actualización manual de la notificación del servicio
**Estado:** RESUELTO

**Síntoma:** la primera CI completa del runtime construyó correctamente las cuatro ABI nativas, pero `lintDebug` bloqueó Android con `NotificationPermission` al llamar directamente a `NotificationManager.notify` en Android 13+.

**Causa:** el foreground service ya posee su notificación obligatoria, pero la implementación intentaba actualizarla mediante la API general de notificaciones, cuyo contrato de lint exige `POST_NOTIFICATIONS`.

**Corrección aplicada:** OclAx actualiza la misma notificación del servicio volviendo a llamar a `ServiceCompat.startForeground`, en lugar de pedir un permiso adicional que no es necesario para el flujo principal del foreground service.

**Prevención:** no ampliar permisos para silenciar lint cuando existe una API más estrecha que representa correctamente el caso de uso; mantener mínimo privilegio.

### ERR-011 — Import de weight bloqueó compilación del emparejamiento
**Estado:** RESUELTO

**Síntoma:** la primera CI de la base de emparejamiento falló en `TransferDevicesSection.kt` con `Cannot access 'RowColumnParentData?.weight'`.

**Causa:** se importó explícitamente `androidx.compose.foundation.layout.weight`, pero en la versión actual de Compose `Modifier.weight` se resuelve como extensión del scope de Row/Column; ese import apuntó a una API interna.

**Corrección aplicada:** eliminar el import explícito y dejar que `Modifier.weight` se resuelva dentro del `RowScope` correspondiente.

**Validación:** la CI posterior del PR #40 terminó verde en tests, lint y build antes de fusionar la base de emparejamiento.

**Prevención:** para extensiones scoped de Compose como `weight`, preferir el patrón ya usado en el proyecto y no importar símbolos internos solo porque el IDE/autocompletado los sugiera.

### ERR-012 — Atrás desde categorías cerraba OclAx
**Estado:** RESUELTO

**Síntoma:** dentro de una categoría como PDF, el gesto/botón Atrás terminaba la Activity principal en vez de regresar al resumen de categorías.

**Causa verificada:** la UI Compose mantenía sourceMode, búsqueda y filtro como estado local, pero no registraba un BackHandler; Android aplicaba el comportamiento por defecto de la Activity.

**Corrección en PR #71:**
- categorías/búsquedas se desenrollan antes de salir;
- Mi dispositivo vuelve a OclAx principal;
- la raíz de OclAx muestra confirmación y un segundo Atrás cierra;
- el picker propio desenrolla su jerarquía antes de volver a la app llamadora;
- la política principal tiene pruebas unitarias.

**Validación física — 2026-09-25:** con main run 281, el gesto Atrás desde PDF volvió correctamente al nivel anterior, la salida desde raíz funcionó como fue solicitada y el picker propio retrocedió por su jerarquía hasta devolver al usuario a la aplicación llamadora sin cerrar ni romper el flujo.

**Prevención:** cualquier nueva superficie navegable debe declarar explícitamente su jerarquía de Atrás y cubrirla con prueba de política o UI.
