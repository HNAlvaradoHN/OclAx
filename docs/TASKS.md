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
- test unitario confirma que Copiar solo aplica a Texto/Código e Imágenes.

Implementado adicionalmente:
- miniaturas reales dentro de la bandeja OclAx para Imagen, Video y PDF reutilizando `ThumbnailLoader`;
- fallback a icono por tipo cuando una miniatura no puede generarse.

Pendiente:
- validación física del menú desplegable y Copiar;
- validar miniaturas internas de Imagen/Video/PDF;
- comprobar qué organización puede exponerse también dentro de DocumentsProvider sin añadir navegación innecesaria;
- validación física.

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

Pendiente:
- validar físicamente visibilidad, orden e iconos;
- validar carpetas por categoría dentro de Archivos → OclAx;
- miniaturas reales para contenido visual cuando aporte valor;
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

Pendiente:
- validación física de permisos y contenido real;
- comprobar que aparecen las aplicaciones esperadas;
- comprobar cantidad/categorías de archivos reales;
- miniaturas reales para imágenes/video;
- validar rendimiento en teléfonos con muchos archivos.

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
6. **SIGUIENTE:** instalar la build en teléfono real y validar arranque → Device ID → loopback → detener → arrancar otra vez sin corrupción.
7. emparejar dos instalaciones de prueba y validar transferencia LAN;
8. validar conexión Internet directa y relay público como fallback;
9. recién después conectar progreso/cancelación/reintento y la UX visible **Enviar a dispositivo**.

Política de recepción:
- **Mis dispositivos / confiables:** opción Permitir sin aceptar;
- otros dispositivos emparejados: preguntar por defecto;
- autoaceptación opcional por dispositivo;
- no emparejados: nunca autoaceptar.

Costo:
- cero servicios pagos por defecto;
- cualquier servidor/relay/infraestructura propia con costo requiere autorización previa.


### TRANSFER-002 — Emparejamiento y confianza por dispositivo
**Estado:** IMPLEMENTED_PENDING_VALIDATION  
**Prioridad:** alta

Implementado en la base de emparejamiento:
- lista privada **Mis dispositivos** separada del motor Syncthing;
- cada dispositivo guarda nombre visible, Device ID normalizado y preferencia **Permitir sin aceptar**;
- la preferencia de autoaceptación empieza siempre apagada;
- agregar/quitar/cambiar confianza no abre red ni inicia transferencias;
- el Device ID propio solo aparece después de una prueba correcta del runtime;
- acción explícita **Compartir ID** usa el Sharesheet de Android;
- se rechaza agregar el propio Device ID y IDs con formato inválido;
- los datos se guardan en SharedPreferences privadas de OclAx.

Validado:
- PR #40 fusionado después de CI verde en tests, lint y build.

Pendiente:
- validar en dos teléfonos que cada uno puede compartir/agregar el ID del otro;
- posteriormente añadir QR si mejora el flujo sin dependencia innecesaria.

### TRANSFER-003 — Conexión LAN entre dispositivos emparejados
**Estado:** IMPLEMENTED_PENDING_VALIDATION  
**Prioridad:** alta

Implementado:
- **Probar LAN** por dispositivo emparejado dentro del panel técnico;
- OclAx pide al propio Syncthing validar/canonizar el Device ID antes de configurarlo;
- el peer se crea pausado, sin autoaceptar carpetas y sin funciones de introducer;
- durante la prueba LAN se habilita únicamente un listener TCP IPv4 y discovery local;
- global discovery, relay, NAT traversal, usage reporting y crash reporting permanecen apagados;
- el peer queda limitado a rangos IPv4 privados/link-local; no se permite `0.0.0.0/0`, `::/0` ni CGNAT como red de confianza;
- Android mantiene un MulticastLock solo durante la búsqueda por discovery local y lo libera en cuanto la conexión LAN queda confirmada;
- la conexión solo se considera válida cuando Syncthing informa `connected=true` e `isLocal=true`;
- **Desconectar LAN** pausa el peer, restaura el motor a modo aislado y libera el MulticastLock; si la restauración falla, el runtime se detiene por seguridad;
- no se comparte ninguna carpeta ni archivo todavía.

Pendiente:
- CI de esta rama;
- prueba física con dos teléfonos en la misma Wi‑Fi;
- confirmar que ambos muestran **Conectado por LAN**;
- confirmar que desconectar vuelve al modo aislado;
- solo después crear el canal privado de archivos y progreso.

### OPEN-001 — Abrir contenido desde la bandeja
**Estado:** IMPLEMENTED_PENDING_VALIDATION  
**Prioridad:** alta

Implementado:
- tocar una tarjeta intenta abrir el archivo con Android usando ACTION_VIEW;
- FileProvider entrega una URI de solo lectura;
- PDF, documentos, imágenes, video, audio, texto y otros usan su MIME real;
- APK puede pasar al instalador/manejador del sistema mediante acción iniciada por el usuario;
- se declara REQUEST_INSTALL_PACKAGES para permitir el flujo de instalación de APK cuando Android lo autorice;
- si no existe manejador compatible, se muestra un mensaje en lugar de fallar.

Pendiente:
- validación física con PDF, Word/OOXML, imagen, video, audio y APK;
- verificar comportamiento con aplicación predeterminada y con selector cuando haya varias opciones;
- verificar flujo de “instalar apps desconocidas” al primer intento de abrir un APK.

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

Pendiente:
- validar físicamente portada de PDF;
- validar borrar/cancelar una imagen y un PDF/documento;
- confirmar compartir APK único y paquete con splits sin datos privados;
- confirmar persistencia de Lista/Cuadrícula;
- confirmar experiencia de recepción/instalación para paquetes con splits antes de declararlo DONE.
