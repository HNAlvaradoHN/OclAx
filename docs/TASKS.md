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

### GOV-003 — Sincronización exhaustiva y numeración real por chat
**Estado:** DONE

Incluye:
- protocol_version 4 fusionado en main;
- lectura exhaustiva de todos los archivos legibles versionados antes del primer handshake;
- inventario recursivo como prueba de completitud;
- un número por hilo visible de chat y contador canónico `last_confirmed_chat`;
- prohibición explícita de incrementar por herramientas, reintentos, resincronizaciones, ramas, PR o CI;
- registro administrativo corregido a protocol 4 / chat 2;
- resincronización completa desde main;
- instrucciones del Project de ChatGPT alineadas para delegar las reglas mutables de sincronización, numeración y handshake a AGENTS.md;
- CI de main verde después del cambio de gobernanza;
- registro del fallo sistemático en ERR-012.


### GOV-004 — Autonomía controlada y revisores automáticos
**Estado:** DONE

Implementado en la rama de gobernanza:
- LOCKED_READ_ONLY separado del estado de sincronización;
- autorización por objetivo, sin exigir listas manuales de archivos;
- tests, lint, build, documentación, memoria y handoff incluidos en una tarea autorizada;
- flujo Git completo permitido dentro del objetivo y límites seguros;
- hallazgos fuera de alcance no bloquean salvo riesgo crítico;
- revisores/agentes aplicables activados automáticamente cuando aportan valor;
- instrucciones compactas de ChatGPT Project versionadas en `docs/CHATGPT_PROJECT_INSTRUCTIONS.txt` y mantenidas bajo 8000 caracteres;
- protocol_version sube a 5.

Validado:
- PR #49 fusionado en `main`;
- CI de `main` run 183 verde;
- resincronización exhaustiva completada bajo protocol v5;
- registro persistente e identidad técnica alineados a protocol 5.


### GOV-005 — Alinear paquete maestro, AGENTS y Project Instructions
**Estado:** DONE

Objetivo:
- eliminar contradicciones entre el paquete maestro V3 y la autonomía ya adoptada en OclAx;
- exigir bootstrap únicamente con `BOOTSTRAP AUTORIZADO`;
- mantener autorización por objetivo sin exigir listas manuales de archivos;
- mantener pruebas, revisión, Git, memoria y handoff dentro de una tarea autorizada;
- versionar el paquete maestro V4;
- hacer obligatorios los revisores aplicables solo cuando la matriz realmente corresponda y aporten valor;
- mantener las instrucciones compactas del Project bajo 8000 caracteres;
- subir `protocol_version` a 6.

Validado:
- PR #51 fusionado en `main`;
- CI de `main` run 188 verde;
- resincronización exhaustiva completada bajo protocol v6;
- registro persistente alineado a protocol 6 sin incrementar el chat actual;
- instrucciones compactas verificadas bajo el límite de 8000 caracteres.

## PENDING

### SEC-001 — Configurar protecciones del repositorio público
**Estado:** IN_PROGRESS  
**Prioridad:** alta

Completado:
- ruleset activo en main;
- PR obligatorio;
- borrado y force-push bloqueados;
- Secret Scanning/Push Protection revisados para repo público;
- Dependabot configurado;
- CI mínimo Android instalado.

Pendiente:
- CodeQL/code scanning antes de release estable;
- mantener solo opciones sin costo salvo autorización.

### APP-001 — Inicializar base Android
**Estado:** IMPLEMENTED_PENDING_VALIDATION  
**Dependencia:** SEC-001

Implementado:
- proyecto Android mínimo;
- separación entre UI, almacenamiento, compartir y provider;
- Share Sheet para contenido mixto;
- portapapeles para texto e imagen;
- DocumentsProvider para Recientes;
- tests/lint/build en CI;
- APK debug como artefacto.

Validado:
- CI verde en PR y main;
- APK debug generado;
- DocumentsProvider visible en el selector de archivos del sistema en dispositivo real;
- flujo Compartir → OclAx → Pegar validado en Qwen;
- inserción desde OclAx validada en Qwen para los tipos de archivo probados;
- APK disponible en OclAx pero rechazado por Qwen por política de la app receptora.

Pendiente:
- validación física completa entre más aplicaciones.

### PRODUCT-001 — Formalizar alcance MVP
**Estado:** DONE

Alcance central confirmado:
- OclAx funciona como bandeja temporal universal;
- entrada rápida y explícita de contenido;
- texto/imagen compartidos quedan en Recientes y se publican al portapapeles;
- cualquier archivo queda disponible mediante Archivos → OclAx → Recientes;
- cero red;
- autolimpieza y fijado;
- sin IME/Accessibility/Shizuku en el MVP.

### PRODUCT-002 — Diseñar selector ordenado de inserción
**Estado:** IN_PROGRESS

Implementado en main:
- búsqueda;
- filtro Fijados;
- filtros Todo, Imágenes, Documentos, PDF, Apps/APK, Texto/Código, Video, Audio y Otros;
- etiquetas visuales por tipo;
- selector compacto de categorías alineado a la izquierda, reemplazando el riel permanente;
- identidad naranja más viva y saturada;
- tarjetas más compactas;
- acciones compactas Compartir, Fijar y Eliminar;
- Copiar solo para Texto/Código e Imágenes;
- confirmación obligatoria antes de borrar una copia;
- compartir hacia Android Sharesheet.

Validado:
- CI verde en PR y main para el bloque compacto/copiar;
- test unitario confirma que Copiar solo aplica a Texto/Código e Imágenes;
- miniaturas de imágenes dentro de Recientes/OclAx confirmadas físicamente en dispositivo.

Implementado adicionalmente:
- miniaturas reales dentro de la bandeja OclAx para Imagen, Video y PDF reutilizando `ThumbnailLoader`;
- fallback a icono por tipo cuando una miniatura no puede generarse.

Pendiente:
- validación física del menú desplegable y Copiar;
- validar miniaturas internas de Video/PDF;
- comprobar qué organización puede exponerse también dentro de DocumentsProvider sin añadir navegación innecesaria;
- validación física.

### PRODUCT-004 — Navegación visual por tarjetas + búsqueda directa
**Estado:** IMPLEMENTED_PENDING_VALIDATION  
**Prioridad:** alta

Objetivo aprobado — 2026-09-24:
- al entrar a OclAx, **Mi dispositivo** o **Elegir con OclAx**, presentar primero categorías como tarjetas visuales;
- tocar una tarjeta entra a su contenido;
- mantener un buscador visible: si el usuario escribe un nombre desde la vista inicial, mostrar resultados directos sin obligarlo a elegir categoría;
- conservar la identidad negro/naranja y el branding OclAx.

Implementado en rama `feat/visual-category-cards`:
- componente visual compartido de tarjetas en dos columnas;
- tarjetas con icono, nombre, cantidad y tamaño total cuando aplica;
- OclAx: Todo, Fijados, Imágenes, Documentos, PDF, APK guardados, Texto/Código, Video, Audio y Otros;
- Mi dispositivo: Aplicaciones, Imágenes, Documentos, PDF, APK, Texto/Código, Video, Audio y Otros;
- buscador global de Mi dispositivo combina coincidencias de apps y archivos por nombre/paquete/MIME/ruta;
- el picker propio usa las mismas tarjetas, pero respeta primero los MIME permitidos por la app llamadora;
- tocar **Categorías** vuelve al resumen visual; las vistas internas conservan lista/cuadrícula y acciones ya existentes;
- tarjetas del resumen son desplazables en pantallas pequeñas.

Feedback de marca:
- el logo/identidad OclAx no debe perderse al evolucionar la UI;
- **NO VERIFICADO:** el repositorio todavía no contiene un asset gráfico oficial de logo/icono para integrar. No se inventará uno; mientras tanto se conserva la marca textual OclAx y el tema aprobado.

Pendiente:
- CI verde;
- validación física en la pantalla principal y en **Elegir con OclAx**;
- comprobar búsqueda global con coincidencias reales, volver a Categorías, scroll en pantalla pequeña y modo claro/oscuro;
- integrar el asset de logo oficial cuando exista/versione, sin bloquear esta navegación.

### DATA-001 — Retención segura y Fijados
**Estado:** IMPLEMENTED_PENDING_VALIDATION  
**Prioridad:** alta

Implementado en main:
- política de retención con 24 h por defecto;
- opciones 1 h, 24 h, 3 días, 7 días y nunca;
- exclusión absoluta de elementos fijados de la autolimpieza;
- borrado limitado a copias privadas bajo `filesDir/oclax/items`;
- persistencia del estado fijado;
- controles UI para cambiar retención y fijar/desfijar;
- CI verde antes de fusión.

Pendiente:
- validación física de expiración y fijados.


### RELEASE-001 — Firma estable de APK de pruebas
**Estado:** DONE  
**Prioridad:** alta

Problema:
- los APK debug generados en runners efímeros pueden quedar firmados con claves distintas;
- Android no permite actualizar una instalación existente si la firma cambia.

Objetivo:
- usar una clave de firma de pruebas estable;
- guardarla exclusivamente en GitHub Actions Secrets;
- nunca committear keystore, contraseña ni clave privada;
- mantener separada cualquier futura clave de release.

Preparado en código:
- Gradle acepta firma estable de prueba solo con los cuatro Secrets presentes;
- GitHub Actions reconstruye el keystore únicamente dentro del runner y solo en pushes a `main`; los PR usan firma debug efímera y no reciben secrets de firma;
- versionCode de CI usa el número monotónico del workflow;
- una configuración parcial de Secrets hace fallar el build.

Validado en CI:
- los cuatro Secrets fueron reconocidos y permanecieron enmascarados;
- el keystore se reconstruyó únicamente dentro del runner;
- CI verde en PR y main;
- el primer APK con firma persistente fue generado como artefacto.

Validación física:
- actualización posterior instalada encima sin conflicto;
- datos internos conservados.


### PRODUCT-003 — Aplicaciones instaladas e iconografía real
**Estado:** IMPLEMENTED_PENDING_VALIDATION  
**Prioridad:** alta

Solicitud confirmada por prueba física:
- mostrar aplicaciones instaladas del dispositivo de forma ordenada;
- usar iconos reales de aplicaciones cuando Android los exponga;
- mejorar iconos/miniaturas de los elementos de OclAx;
- no confundir APK guardados con aplicaciones instaladas.

Implementado:
- categoría Aplicaciones separada de APK;
- listado alfabético de apps lanzables visibles;
- icono real de cada aplicación;
- búsqueda por nombre/paquete;
- iconos Material reconocibles por tipo para imágenes, PDF, APK, documentos, texto/código, video, audio y otros;
- inicialmente se evitó `QUERY_ALL_PACKAGES`; esta restricción fue posteriormente sustituida por DEVICE-001/DEC-019 al aprobarse la superficie amplia Mi dispositivo.

Corrección en curso tras prueba física:
- listado migrado a LauncherApps para mayor fiabilidad;
- etiquetas diferenciadas: Apps instaladas / APK guardados;
- DocumentsProvider organizado por categorías manteniendo acceso directo a recientes.

Validado físicamente — 2026-09-24:
- la lista de Apps instaladas funciona correctamente en el teléfono probado.

Pendiente:
- validar carpetas por categoría dentro de Archivos → OclAx;
- continuar ampliando miniaturas visuales cuando aporte valor;
- decidir más adelante si tocar una app la abre o la convierte en destino directo de compartir.


### DEVICE-001 — Mi dispositivo: contenido real completo
**Estado:** IMPLEMENTED_PENDING_VALIDATION  
**Prioridad:** alta

Implementado:
- superficie separada **Mi dispositivo**;
- selector compacto entre OclAx y Mi dispositivo;
- inventario completo de aplicaciones mediante QUERY_ALL_PACKAGES;
- categorías Apps, Imágenes, Documentos, PDF, APK, Texto/Código, Video, Audio y Otros;
- índice de archivos reales mediante MediaStore.Files con acceso amplio;
- solicitud de MANAGE_EXTERNAL_STORAGE mediante la pantalla especial de Android;
- búsqueda por nombre, MIME y ruta relativa;
- tocar archivo abre; tocar app intenta abrirla;
- Compartir para archivos del dispositivo;
- Copiar para texto e imágenes;
- si se niega acceso amplio, Apps sigue disponible y las categorías de archivos muestran opción para concederlo;
- la primera versión no incluía borrado de originales; DEVICE-002 añadió después **Eliminar original** explícito y confirmado.

Validado físicamente — 2026-09-24:
- aparecen las aplicaciones instaladas esperadas;
- fecha y hora de modificación visibles en las tarjetas de **Mi dispositivo** con main run 207; describe modificación del archivo, no hora de recepción OclAx.

Pendiente:
- validación completa de revocación/reconcesión de permisos;
- comprobar cantidad/categorías con una muestra más amplia;
- miniaturas de video cuando el formato sea compatible;
- validar rendimiento en teléfonos con muchos archivos.

### PICKER-001 — Elegir contenido con UI propia de OclAx desde otras apps
**Estado:** IMPLEMENTED_PENDING_VALIDATION  
**Prioridad:** alta

Objetivo:
- cuando otra aplicación use `ACTION_GET_CONTENT`, permitir elegir **OclAx** como origen y abrir una pantalla propia;
- desde esa pantalla elegir contenido de la bandeja OclAx o de **Mi dispositivo** y devolverlo a la aplicación llamadora.

Implementado en PR #62:
- Activity exportada limitada a `ACTION_GET_CONTENT`;
- selector **OclAx / Mi dispositivo** con búsqueda;
- filtrado por MIME solicitado;
- selección simple y múltiple;
- retorno mediante `content://` y permiso temporal de solo lectura;
- acceso amplio opcional para Mi dispositivo reutilizando la política vigente;
- límites defensivos para MIME externos y cantidad máxima de selección;
- DocumentsProvider/`ACTION_OPEN_DOCUMENT` sin cambios;
- tests unitarios de matching MIME.

Validación física parcial — 2026-09-24:
- **Elegir con OclAx** aparece desde una app compatible y abre correctamente la UI propia;
- **Mi dispositivo** carga archivos reales;
- la primera UI resultó demasiado plana: mezclaba tipos sin el mismo orden visual/categorías/miniaturas de la aplicación principal.

Corrección en curso:
- alinear selector de origen con el control compacto de la app;
- añadir categorías OclAx/Mi dispositivo;
- reutilizar miniaturas para Imagen/Video/PDF;
- mostrar fecha/hora y ruta donde aplique;
- compactar tarjetas para que se sientan como la navegación principal.

Validado físicamente — main run 229:
- **Elegir con OclAx** aparece y la presentación visual corregida fue confirmada como correcta por el dueño.

Pendiente:
- confirmar devolución del archivo en más variantes;
- probar cancelar y permiso de almacenamiento negado;
- la selección múltiple existente queda como compatibilidad técnica de `ACTION_GET_CONTENT` cuando la app llamadora la solicite; **no es una función de producto propuesta por el dueño ni una validación prioritaria**;
- validar la nueva entrada por tarjetas de PRODUCT-004.

### TRANSFER-001 — Envíos OclAx ↔ OclAx
**Estado:** IN_PROGRESS  
**Prioridad:** alta  
**Motor candidato:** Syncthing core v2.x detrás de adaptador propio

Objetivo UX:
- seleccionar contenido;
- elegir un dispositivo emparejado;
- enviar sin exponer al usuario conceptos de carpetas sincronizadas.

Investigación verificada — 2026-09-23:
- el wrapper oficial Android de Syncthing fue discontinuado y archivado; no se incorporará como dependencia;
- Syncthing estable vigente para iniciar el spike: v2.1.5;
- existe un fork Android comunitario mantenido (`researchxxl/syncthing-android`) que demuestra ejecución nativa/foreground service actual, pero se usa solo como referencia;
- Syncthing mantiene REST local, discovery LAN/global y relay; el relay conserva cifrado extremo a extremo entre dispositivos aunque expone metadatos de conexión al relay;
- el runtime debe controlarse exclusivamente por loopback con API key privada.

Spike técnico, en orden:
1. **VERIFICADO:** construir/empaquetar Syncthing core v2.1.5 para Android arm64 en CI aislada y **sin acceso a secretos de firma**; se produjo un ELF Android API 26 con verificación SHA-256.
2. **VERIFICADO_EN_CI:** el CI principal construye runtimes pinneados para arm64-v8a, armeabi-v7a, x86_64 y x86 en un job sin secretos, verifica cada SHA-256 y los empaqueta en el APK; Gradle fuerza extracción para ejecutarlos como proceso hijo. PR #39 y main run 125 terminaron verdes.
3. **IMPLEMENTADO_PENDIENTE_VALIDACIÓN_FÍSICA:** antes de iniciar `serve`, OclAx genera la configuración local si hace falta y la endurece: GUI/API `127.0.0.1:8384`, API key privada, listener BEP solo loopback y discovery global/local, relay y NAT desactivados.
4. **IMPLEMENTADO_PENDIENTE_VALIDACIÓN_FÍSICA:** `SyncthingRuntimeService` usa foreground service `dataSync` on-demand y vuelve a aplicar/verificar por REST el modo aislado, `urAccepted=-1` y `crashReportingEnabled=false` antes de marcar el motor activo.
5. **IMPLEMENTADO_PENDIENTE_VALIDACIÓN_FÍSICA:** el probe obtiene `myID`, comprueba que REST exige API key, valida dirección GUI loopback, intenta detectar el mismo runtime por IPv4 no-loopback y la detención usa `/rest/system/shutdown` con fallback acotado.
6. **FALLO FÍSICO REPRODUCIDO:** en dos teléfonos, `Probar motor` agotó el tiempo sin responder antes de obtener Device ID.
7. **PR #47 IMPLEMENTADO Y CI VERDE:** el runtime Android ejecuta Syncthing como proceso interno ya supervisado (`STMONITORED=1`) y usa almacenamiento temporal privado para SQLite.
8. **REPRUEBA FÍSICA FALLIDA:** la build firmada de `main` siguió mostrando `El motor no respondió a tiempo`; por tanto esa hipótesis no resolvió la causa real.
9. **DIAGNÓSTICO IMPLEMENTADO Y FUSIONADO · MAIN CI VERDE:** registrar etapa exacta, código de salida y una línea de log del intento actual sanitizada; sin permisos, telemetría ni subida de logs.
10. **CAUSA VERIFICADA EN DISPOSITIVO:** Android falla al preparar la configuración porque su parser no soporta obligatoriamente la feature Xerces `disallow-doctype-decl`.
11. **FIX VALIDADO EN UN TELÉFONO REAL:** PR #53 fusionado; main run 192 verde; la APK firmada supera la preparación privada y **Probar motor** devuelve Device ID + `loopback verificado`.
12. **FALLO UX CORREGIDO_PENDIENTE_VALIDACIÓN_FÍSICA:** PR #55 fusionado y main run 196 verde; OclAx usa una única lista desplazable para panel, búsqueda, controles y tarjetas.
13. **DIAGNÓSTICO LAN IMPLEMENTADO_PENDIENTE_CI/FÍSICA:** al vencer la búsqueda, consulta `/rest/system/discovery` y diferencia `no visto por discovery`, `visto sin conexión` y `peer pausado`, sin exponer IPs.
14. **VALIDADO EN PRIMER TELÉFONO:** main run 207 confirma scroll completo, Device ID + loopback y fecha/hora en Mi dispositivo.
15. **COMPLETADO:** segundo dispositivo emparejado y motor validado;
16. **COMPLETADO:** conexión LAN verificada físicamente y aislamiento restaurado al desconectar en ambos;
17. **ACTUAL:** implementar y validar el canal privado de archivos LAN + progreso;
18. Internet directo/relay queda fuera de alcance hasta terminar la transferencia LAN y recibir autorización para ese bloque.

Política de recepción:
- **Mis dispositivos / confiables:** opción Permitir sin aceptar;
- otros dispositivos emparejados: preguntar por defecto;
- autoaceptación opcional por dispositivo;
- no emparejados: nunca autoaceptar.

Costo:
- cero servicios pagos por defecto;
- cualquier servidor/relay/infraestructura propia con costo requiere autorización previa.


### TRANSFER-002 — Emparejamiento y confianza por dispositivo
**Estado:** DONE  
**Prioridad:** alta

Implementado:
- lista privada **Mis dispositivos** separada del motor Syncthing;
- cada dispositivo guarda nombre visible, Device ID normalizado y preferencia **Permitir sin aceptar**;
- la preferencia de autoaceptación empieza apagada;
- agregar/quitar/cambiar confianza no abre red ni inicia transferencias;
- el Device ID propio solo aparece después de una prueba correcta del runtime;
- acción explícita **Compartir ID** usa el Sharesheet de Android;
- se rechaza agregar el propio Device ID y IDs con formato inválido;
- los datos se guardan en SharedPreferences privadas de OclAx.

Validado:
- PR #40 fusionado después de CI verde;
- validación física con dos dispositivos: ambos quedaron mutuamente emparejados de forma suficiente para alcanzar una conexión LAN autenticada por Device ID;
- **Permitir sin aceptar** permanece como preferencia explícita por peer y será revalidada durante TRANSFER-004.

Mejora futura no bloqueante:
- añadir QR si reduce fricción sin dependencia innecesaria.

### TRANSFER-003 — Conexión LAN entre dispositivos emparejados
**Estado:** DONE  
**Prioridad:** alta

Implementado:
- conexión LAN explícita por dispositivo emparejado con Syncthing core;
- Local Discovery como primera ruta y fallback directo acotado al segmento Wi‑Fi/Ethernet inmediato, únicamente TCP/22000;
- éxito solo con Device ID emparejado y `connected=true` + `isLocal=true`;
- global discovery, relay, NAT, usage reporting y crash reporting apagados;
- al desconectar, peer pausado, direcciones temporales restauradas a `dynamic` y motor vuelve a aislamiento; cleanup incompleto detiene runtime.

Validación automática:
- PR #66 fusionado;
- main run 237 verde en runtime nativo, tests, lint, build, verificación de APK y artefacto.

Validación física — 2026-09-24:
- los dos dispositivos emparejados alcanzaron **Conectado por LAN**;
- al pulsar **Desconectar LAN** en cada uno, cada dispositivo volvió correctamente a **Motor aislado / Desconectado · motor aislado**;
- el cierre es local por dispositivo en esta fase: desconectar uno no envía una orden remota para cambiar la UI/configuración del otro;
- **CAUSA DEL FALLO HISTÓRICO DE LOCAL DISCOVERY: NO VERIFICADA.** La prueba valida conectividad LAN OclAx, no demuestra qué ruta concreta produjo el enlace.

### TRANSFER-004 — Canal real de archivos LAN + progreso
**Estado:** IN_PROGRESS  
**Prioridad:** alta  
**PR:** #68

Objetivo:
- enviar una copia almacenada en OclAx al dispositivo emparejado sobre la conexión LAN ya verificada;
- pedir aceptación por defecto y respetar **Permitir sin aceptar** por dispositivo;
- mostrar progreso/estado;
- materializar lo recibido primero en almacenamiento privado OclAx;
- limpiar configuración y staging efímeros al terminar o fallar.

Implementación actual:
- `FileTransferCoordinator` separa orquestación de UI;
- `OclAxTransferChannel` crea una carpeta Syncthing efímera por transferencia dentro de almacenamiento privado;
- protocolo acotado a `payload.bin`, manifest JSON y ACK JSON, con namespace `oclax-<id>`;
- receptor consulta solicitudes periódicamente mientras LAN está activa; puede Aceptar/Rechazar y autoacepta solo si el peer guardado tiene **Permitir sin aceptar**;
- manifest valida transferId, Device ID remitente, nombre/MIME y tamaño; ItemStore vuelve a sanitizar nombre/MIME;
- se rechazan directorios, symlinks, exceso de archivos y carpetas por encima del límite defensivo;
- el remitente no marca éxito hasta recibir ACK después de que ItemStore haya importado la copia;
- timeout escala con tamaño hasta un máximo acotado; UI bloquea detener/desconectar mientras una transferencia está activa;
- no se añaden permisos ni dependencias y no se habilita Internet, global discovery, relay o NAT.

Pendiente:
- CI verde y merge de PR #68;
- prueba física con dos dispositivos: aceptar, rechazar, **Permitir sin aceptar**, progreso, archivo recibido utilizable y cleanup;
- probar un archivo mayor y falta de espacio;
- cancelación/reintento explícitos quedan para refinamiento posterior después del primer vertical estable.

### PLATFORM-001 — Compatibilidad de permisos de red local Android 17
**Estado:** PENDING  
**Prioridad:** media  
**No bloquea:** validación actual con targetSdk 36.

Hallazgo de revisión de Plataforma:
- OclAx apunta actualmente a Android 16 / API 36;
- Android 17 / API 37 introduce `ACCESS_LOCAL_NETWORK` como permiso runtime para acceso LAN al apuntar a API 37;
- antes de subir targetSdk a 37 o publicar una release que lo haga, implementar el flujo de permiso local o un selector de sistema compatible;
- probar rechazo/revocación del permiso y mantener Internet normal independiente del LAN.

### OPEN-001 — Abrir contenido desde la bandeja
**Estado:** DONE  
**Prioridad:** alta

Implementado:
- tocar una tarjeta intenta abrir el archivo con Android usando ACTION_VIEW;
- FileProvider entrega una URI de solo lectura;
- PDF, documentos, imágenes, video, audio, texto y otros usan su MIME real;
- APK puede pasar al instalador/manejador del sistema mediante acción iniciada por el usuario;
- se declara REQUEST_INSTALL_PACKAGES para permitir el flujo de instalación de APK cuando Android lo autorice;
- si no existe manejador compatible, se muestra un mensaje en lugar de fallar.

Validación física — 2026-09-24:
- PDF, Word/OOXML, imagen, video, audio y APK abren correctamente desde OclAx en el teléfono probado;
- el flujo de APK alcanza el manejador/instalador del sistema según corresponda.

Observaciones futuras no bloqueantes:
- seguir verificando variantes de resolución cuando existan varias apps manejadoras o ningún manejador compatible.

### DEVICE-002 — Miniaturas, compartir apps y borrar originales
**Estado:** IMPLEMENTED_PENDING_VALIDATION  
**Prioridad:** alta

Implementación en PR #34:
- miniaturas reales para imágenes/video en Mi dispositivo;
- miniaturas expuestas por DocumentsProvider al selector de Android;
- lista/cuadrícula persistente por categoría;
- compartir apps instaladas usando base APK + splits sin datos privados;
- borrar originales mediante confirmación explícita y autorización MediaStore cuando corresponda.

Validado:
- tests unitarios, lint y assembleDebug verdes en CI del PR;
- prueba física confirma miniaturas reales para imágenes.

Fallo físico detectado:
- PDF no mostraba portada porque la ruta de miniaturas excluía PDF;
- borrar un original fallaba con `All requested items must be Media items` porque Android 11+ recibía una URI genérica de `MediaStore.Files`.

Corrección implementada:
- primera página del PDF renderizada con `PdfRenderer` para Mi dispositivo y DocumentsProvider;
- borrado Android 11+ intenta eliminación directa con el acceso amplio ya concedido y usa confirmación MediaStore solo con URI específica para Imagen/Video/Audio cuando sea necesaria.

Validado físicamente — 2026-09-24:
- portada/primera página de PDF visible;
- borrar y cancelar funcionan para imagen y PDF/documento;
- Lista/Cuadrícula se recuerda correctamente.

Validación física adicional — 2026-09-25:
- el dueño confirma que **Mi dispositivo** funciona correctamente en las rutas probadas;
- compartir una app/APK funciona en el dispositivo probado.

Pendiente:
- distinguir y validar explícitamente el caso de paquete con splits antes de declararlo DONE;
- confirmar experiencia de recepción/instalación para paquetes con splits.

### NAV-001 — Navegación Atrás jerárquica y salida confirmada
**Estado:** DONE  
**Prioridad:** alta  
**PR:** #71 · fusionado; main run 281 verde

Objetivo:
- Atrás desde una categoría/búsqueda vuelve al resumen de categorías en vez de cerrar OclAx;
- Atrás desde **Mi dispositivo** vuelve primero a OclAx principal;
- en la pantalla principal, el primer Atrás pide confirmación de salida;
- después de esa advertencia, un segundo Atrás cierra la app;
- el picker propio también retrocede categoría → origen → app llamadora.

Implementado:
- política de navegación principal separada y testeable;
- BackHandler en OclAx, Mi dispositivo y picker propio;
- confirmación explícita de salida sin alterar almacenamiento, permisos, red ni transferencias.

Validado físicamente:
- run 281: el gesto Atrás desde la vista PDF vuelve al nivel anterior y ya no cierra OclAx;
- la secuencia de salida desde la raíz funciona como fue solicitada;
- **Elegir con OclAx** retrocede categoría → resumen → origen → aplicación llamadora sin cerrar ni romper el flujo;
- la navegación por gestos del dispositivo probado cubre el mecanismo real disponible; no queda un botón físico pendiente en ese equipo.
