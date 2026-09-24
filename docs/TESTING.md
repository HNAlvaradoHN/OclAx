# TESTING

## Estado actual

Existe una primera base Android con CI.

## CI de la prueba vertical

En cada Pull Request y en `main`:

```bash
gradle --no-daemon :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

El workflow publica un APK debug solo si las validaciones anteriores terminan correctamente.

## Pruebas unitarias actuales

`SafeNamesTest` valida:
- sanitización de nombres externos;
- prevención básica de nombres/rutas hostiles;
- extensiones admitidas;
- nombres internos de payload.

`RetentionPolicyTest` valida:
- expiración de elementos no fijados al superar la retención;
- exclusión de Fijados;
- opción `nunca`;
- fallback seguro a 24 horas ante un valor no admitido.

## Validación física

La automatización NO demuestra compatibilidad entre aplicaciones.

VERIFICADO en teléfono real:
- OclAx aparece en el selector de archivos del sistema como una fuente disponible mediante DocumentsProvider;
- el flujo Compartir → OclAx → Pegar funciona en Qwen;
- los tipos de archivo probados pueden seleccionarse e insertarse en Qwen desde OclAx;
- APK queda disponible en OclAx, pero Qwen no lo acepta como adjunto.

Interpretación:
- el rechazo de APK es una restricción de la app receptora, no un fallo de DocumentsProvider;
- OclAx no debe prometer que una app externa aceptará todos los MIME types que Android permite exponer.

Pendiente de probar:
- búsqueda dentro de la raíz OclAx;
- comportamiento con archivo grande y poco almacenamiento;
- WhatsApp, Telegram y navegador;
- expiración física de copias OclAx y supervivencia del archivo original;
- Fijados y cambio de retención desde UI;
- borrado manual con confirmación y comprobación de que el original externo sobrevive;
- compartir desde una tarjeta hacia otra aplicación;
- selector compacto de categorías alineado a la izquierda: abrir, seleccionar, cierre automático y cierre al tocar fuera;
- naranja vivo y contraste en modo claro/oscuro;
- tarjetas y acciones compactas en distintos tamaños de pantalla;
- Copiar texto e imagen desde una tarjeta y pegar en aplicaciones compatibles;
- verificar que PDF, APK, documentos, video, audio y otros no muestren Copiar;
- comprobar que la categoría Aplicaciones muestra apps lanzables con nombre e icono real, ordenadas alfabéticamente;
- comprobar que APK y Aplicaciones permanecen separadas;
- comprobar búsqueda por nombre de aplicación.

Hasta completar esa validación, APP-001 permanece como:
`IMPLEMENTED_PENDING_VALIDATION`.


## Firma estable de pruebas

VERIFICADO en CI:
- los cuatro GitHub Actions Secrets se reconocen y se muestran enmascarados;
- el keystore se reconstruye en el almacenamiento temporal del runner;
- el build de main con firma persistente terminó verde;
- se publicó el artefacto APK firmado.

Pendiente en dispositivo:
- desinstalar una última vez la instalación antigua firmada con una clave efímera;
- instalar el primer APK con firma persistente;
- generar una build posterior y confirmar que Android la instala encima sin conflicto y conserva los datos internos.


## Bloque compacto + Copiar

VERIFICADO en CI:
- tests unitarios verdes;
- lint verde;
- assembleDebug verde;
- test de elegibilidad confirma que Copiar solo está disponible para Texto/Código e Imágenes;
- build de main generado con la firma persistente de pruebas.

Pendiente en dispositivo:
- instalar esta build encima de la instalación estable anterior sin desinstalar;
- comprobar conservación de copias/preferencias internas;
- probar cierre del menú al seleccionar y al tocar fuera;
- copiar texto e imagen y pegar en aplicaciones compatibles.


## Validación física — actualización estable y apps

VERIFICADO por el dueño:
- la APK nueva se instaló encima de la anterior sin conflicto;
- los datos internos se conservaron.

FALLO observado:
- la primera implementación de Aplicaciones no mostró las apps esperadas;
- Archivos → OclAx continuó mostrando una raíz plana de recientes.

Corrección a validar:
- Apps instaladas mediante LauncherApps;
- etiquetas Apps instaladas / APK guardados;
- carpetas virtuales por categoría dentro de DocumentsProvider manteniendo recientes directos.


## Abrir contenido desde OclAx

Pendiente de validación física:
- tocar PDF y abrirlo con visor compatible;
- tocar Word/OOXML y abrirlo con editor/visor instalado;
- tocar imagen y abrirla con galería/visor;
- tocar video/audio y abrirlo con reproductor;
- tocar APK y llegar al instalador/manejador del sistema;
- comprobar que Android respeta una app predeterminada o muestra resolución cuando corresponda;
- comprobar mensaje seguro cuando no existe manejador compatible;
- confirmar que ninguna apertura concede escritura ni modifica la copia OclAx.


## Mi dispositivo

Pendiente de validación física:
- entrar a Mi dispositivo y comprobar que Apps lista aplicaciones instaladas reales, incluidas las que antes no eran visibles;
- seleccionar Imágenes/Documentos/PDF/APK/Texto-Código/Video/Audio/Otros y comprobar solicitud de acceso amplio;
- conceder acceso desde Ajustes y volver a OclAx;
- comprobar que las categorías muestran contenido real del almacenamiento;
- buscar por nombre/ruta;
- abrir y compartir archivos reales;
- copiar texto e imágenes;
- revocar acceso amplio y confirmar que OclAx conserva su bandeja y que Mi dispositivo vuelve a pedir permiso;
- confirmar que **Eliminar original** aparece solo como acción explícita y que cancelar conserva el archivo;
- observar tiempo de carga y fluidez con un dispositivo con muchos archivos.

## Miniaturas, compartir apps y borrar originales

VERIFICADO en teléfono real:
- Mi dispositivo muestra miniaturas reales para imágenes en cuadrícula.

FALLOS observados:
- PDF no mostraba la portada/primera página;
- borrar un original fallaba con `All requested items must be Media items`.

Corrección a validar:
- PDF debe mostrar una miniatura generada desde su primera página tanto en Mi dispositivo como en Archivos → OclAx;
- borrar una imagen debe funcionar o pedir confirmación Android válida sin el error de URI genérica;
- borrar un PDF/documento debe usar el acceso amplio concedido y no intentar presentarlo como elemento multimedia.

Pendiente de validación física:
- Video debe mostrar fotograma/miniatura cuando sea compatible;
- Mi dispositivo debe mostrar miniaturas en lista y cuadrícula;
- cambiar vista en una categoría no debe modificar las demás y debe sobrevivir al reinicio;
- compartir una app de APK único debe enviar un APK instalable sin datos privados del usuario;
- compartir una app con splits debe enviar base + todos los splits;
- recepción/instalación debe seguir siendo una decisión explícita del receptor;
- cancelar cualquier confirmación de borrado debe conservar el archivo;
- tras eliminar, el elemento debe desaparecer del índice sin afectar copias OclAx;
- autolimpieza debe seguir sin tocar originales.

## Miniaturas dentro de la bandeja OclAx

**Validación física confirmada — 2026-09-23:** las miniaturas de imagen en Recientes/OclAx se muestran correctamente en dispositivo real.

Pendiente de validación física:
- una imagen recibida/pegada debe mostrar su miniatura real en la tarjeta de OclAx;
- un video recibido debe mostrar un fotograma cuando pueda decodificarse;
- un PDF recibido debe mostrar la primera página cuando pueda renderizarse;
- texto, APK, audio, documentos y otros deben conservar un icono reconocible;
- si falla la generación de miniatura, la tarjeta debe seguir siendo utilizable con icono de fallback;
- desplazar una lista con muchas miniaturas no debe bloquear perceptiblemente la UI.

## Spike de transferencia OclAx ↔ OclAx

VERIFICADO previamente en CI aislada:
- Syncthing core v2.1.5 se compiló para Android arm64/API 26 con NDK r30;
- el artefacto fue ELF Android y su SHA-256 coincidió con el archivo de verificación;
- el job usó permisos `contents: read` y no recibió secretos de firma de OclAx.

VERIFICADO en main run 125:
- job nativo construyó correctamente arm64-v8a, armeabi-v7a, x86_64 y x86;
- checksums se verificaron antes de empaquetar;
- tests unitarios, lint y assembleDebug terminaron verdes;
- el APK contiene los cuatro `libsyncthingnative.so`;
- la firma estable de pruebas se preparó correctamente en main y el APK se publicó como artefacto.

Implementado para la siguiente validación física:
- CI principal construye y empaqueta `libsyncthingnative.so` para arm64-v8a, armeabi-v7a, x86_64 y x86 desde el tag+commit pinneado;
- test unitario verifica argumentos seguros de arranque/generación;
- test unitario endurece un config inseguro y confirma loopback/no-discovery/no-relay/no-NAT/no-reporting; también comprueba rechazo de DOCTYPE/entidades externas;
- debug muestra **Enviar a dispositivo · prueba técnica** con Probar motor/Detener;
- antes de `serve` se endurece el config y el proceso arranca pausado; después el servicio vuelve a verificar opciones privadas por REST;
- el probe espera health, obtiene Device ID, verifica autenticación, dirección GUI loopback e intenta comprobar interfaces IPv4 no-loopback;
- detener usa shutdown REST autenticado y fallback acotado.

Antes de conectar UX de envío, todavía debe demostrar físicamente:
- el runtime Syncthing core pinneado arranca en Android y entrega un Device ID;
- REST/GUI responde únicamente por loopback y rechaza acceso desde otra máquina de la LAN;
- la API exige la key privada generada localmente;
- auto-upgrade, usage reporting y crash reporting permanecen desactivados;
- start/stop/restart no corrompe configuración/base de datos;
- dos dispositivos emparejados transfieren un archivo pequeño por LAN;
- un archivo grande puede mostrar progreso sin cargarse completo en memoria;
- cancelación/reintento dejan estados consistentes;
- sin ruta directa, relay funciona como fallback manteniendo contenido cifrado extremo a extremo;
- contenido recibido aterriza en área privada OclAx y no se abre/instala automáticamente;
- CI de construcción nativa no recibe secretos de firma de la app.

## Base de emparejamiento

Automático:
- normalización acepta Device ID compacto o con guiones;
- formato/longitud/caracteres inválidos se rechazan;
- la UI no puede agregar un par sin nombre ni ID válido.

Fallo físico observado antes del emparejamiento:
- en dos teléfonos, **Probar motor** agotó el tiempo antes de devolver Device ID;
- PR #47 cambió el entorno Android a proceso ya supervisado (`STMONITORED=1`) + temp SQLite privado y pasó CI;
- la build firmada posterior siguió mostrando exactamente el mismo timeout en dispositivo real;
- la build diagnóstica posterior identificó la etapa exacta: **preparando la configuración privada**;
- el mensaje mostró la feature `http://apache.org/xml/features/disallow-doctype-decl`, confirmando que el parser XML de Android abortaba antes de iniciar Syncthing.

Corrección automática validada:
- las features XML específicas del parser se aplican solo si están soportadas;
- OclAx rechaza `DOCTYPE` y `ENTITY` antes del parseo y bloquea resolución externa mediante `EntityResolver`;
- test unitario nuevo simula una feature no soportada y confirma que la preparación no aborta por esa razón;
- se conservan las pruebas de endurecimiento de red/telemetría y rechazo XXE/DOCTYPE;
- PR #53 y main run 192 terminaron verdes en runtime nativo, tests, lint, build multi-ABI y verificación del APK;
- la APK firmada de main run 192 quedó publicada para la siguiente prueba física.

Validación física confirmada — 2026-09-23:
- en un teléfono con la build de main run 192, **Probar motor** muestra `Listo`, Device ID y `loopback verificado`;
- la corrección de compatibilidad XML supera físicamente la etapa que antes fallaba;
- en esa misma pantalla se detectó que el panel técnico largo no permitía desplazar verticalmente todo OclAx; PR #55 + main run 196 quedaron verdes y falta confirmar físicamente el scroll corregido.

Pendiente de validación física:
- confirmar el mismo arranque correcto en el segundo teléfono;
- **Compartir ID** abre Sharesheet sin incluir API keys, archivos ni datos personales;
- en otro teléfono puede pegarse ese ID y guardarlo con un nombre;
- el propio ID no puede agregarse;
- un duplicado no puede agregarse dos veces;
- **Permitir sin aceptar** empieza apagado y persiste al reiniciar;
- quitar un par no toca contenido OclAx ni originales del dispositivo;
- agregar/quitar pares no debe generar tráfico de sincronización mientras el motor siga en modo aislado.

## Conexión LAN entre pares

Precondición de plataforma:
- la Network Security Config debe permitir el REST HTTP únicamente en `127.0.0.1`/`localhost`; no habilitar cleartext global.

Automático:
- la política LAN permite solo `10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16` y `169.254.0.0/16`;
- no acepta `0.0.0.0/0`, `::/0` ni `100.64.0.0/10`;
- el listener de la prueba es TCP IPv4 en el puerto Syncthing esperado;
- tests del diagnóstico LAN verifican mensajes distintos para peer no descubierto, peer descubierto sin conexión y peer inesperadamente pausado;
- el diagnóstico usa `/rest/system/discovery` solo al vencer la búsqueda y no presenta las IPs encontradas al usuario.

Pendiente de validación física con dos teléfonos:
1. instalar la misma build en ambos;
2. ejecutar **Probar motor** en ambos y agregar mutuamente sus Device ID;
3. conectar ambos a la misma Wi‑Fi;
4. tocar **Probar LAN** sobre el otro dispositivo en ambos teléfonos;
5. ambos deben llegar a **Conectado por LAN** y discovery local/MulticastLock deben quedar apagados después de conectar;
6. no debe transferirse ningún archivo en esta prueba;
7. tocar **Desconectar LAN** y confirmar que vuelve a modo aislado;
8. repetir con un peer incorrecto/no presente y confirmar timeout seguro, retorno a modo aislado y mensaje **no apareció en discovery local**;
9. con ambos teléfonos tocando **Probar LAN** pero sin completar enlace, confirmar que si discovery sí ve al peer el mensaje cambia a **apareció en discovery local, pero no se completó la conexión**;
10. simular/forzar fallo al restaurar configuración y comprobar que el runtime se detiene en vez de dejar el listener LAN abierto.

## Revisión obligatoria TRANSFER-003 — post-merge

Revisión aplicada según `docs/REVIEW_ROLES.md` después de detectar que el cambio se había fusionado sin dejar la auditoría explícita registrada.

- **Seguridad — INFORMATIVO.** Evidencia: REST HTTP limitado por Network Security Config a loopback; API key privada; discovery local temporal; peers limitados a redes privadas; cleanup fail-closed. Riesgo: exposición LAN accidental si esos controles regresan. Recomendación: conservar tests/controles actuales. Validación: CI + prueba física de conexión/desconexión.
- **Privacidad — INFORMATIVO.** Evidencia: Device ID solo se comparte por acción explícita; local discovery se activa únicamente durante búsqueda y se apaga tras conectar; no se transfieren archivos aún. Riesgo: Device ID visible temporalmente en la LAN. Recomendación: mantener consentimiento explícito y ventana corta. Validación: observar que discovery termina tras conectar.
- **Arquitectura — NO BLOQUEANTE.** Evidencia: la lógica de transporte está en `TransferRuntimeController`/`SyncthingRestClient`, pero `MainActivity` ya acumula estado/orquestación de diagnóstico. Riesgo: el siguiente bloque de envío/progreso puede acoplar UI y dominio. Recomendación: extraer un coordinador/state holder pequeño antes de que TRANSFER-004 crezca; no sobre-modularizar. Validación: UI no debe contener reglas de transporte ni persistencia.
- **Plataforma Android — NO BLOQUEANTE.** Evidencia: targetSdk actual 36; Android 17/API 37 requerirá permiso runtime de red local para apps que apunten a 37. Riesgo: futuras builds perderían LAN si se sube target sin flujo de permiso. Recomendación: mantener PLATFORM-001 antes de target 37. Validación: probar grant/deny/revoke al migrar.
- **QA — INFORMATIVO.** Evidencia: tests unitarios y CI verdes; no existe todavía prueba física de dos teléfonos. Riesgo: diferencias reales de Wi‑Fi/multicast/ROM. Recomendación: no marcar DONE hasta completar el guion de prueba física. Validación: dos teléfonos, conexión local, desconexión y timeout seguro.
- **Rendimiento — INFORMATIVO.** Evidencia: polling REST cada 500 ms solo durante una ventana de hasta 45 s y MulticastLock solo durante discovery. Riesgo: consumo temporal si se repite muchas veces. Recomendación: medir antes de optimizar; considerar eventos solo si la prueba muestra impacto. Validación: observar batería/fluidez en prueba real.
- **Diseño/UX/Accesibilidad — NO BLOQUEANTE.** Evidencia: controles técnicos están limitados a debug y estados tienen texto además de color. Riesgo: exceso de detalles si llegan a UX final. Recomendación: retirar controles técnicos cuando exista Enviar → dispositivo → progreso. Validación: prueba de flujo final.
- **Calidad/Limpieza — INFORMATIVO.** Evidencia: el bloque viejo de diagnóstico fue reemplazado, no quedó duplicado activo. Recomendación: mantener esta regla al reemplazar el panel técnico.
- **Release — NO APLICA aún.** Sigue siendo build de validación; release estable requiere además SEC-001/CodeQL, licencias/atribuciones y validación física.

