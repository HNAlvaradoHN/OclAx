# PROJECT_STATE

## Estado

**Fase:** primera prueba vertical Android.  
**Aplicación:** base funcional validada por CI; validación física en progreso.  
**Repositorio:** público.  
**Protocol version:** 6.

## Qué ya está definido

- OclAx debe poder reconstruir el estado de trabajo desde GitHub al iniciar cualquier chat nuevo; las decisiones duraderas se registran aquí/TASKS/DECISIONS/SECURITY/ARCHITECTURE antes del handoff.
- Nombre: OclAx.
- Significado: Open Content, Local Access & eXchange.
- Identidad del agente: OclAx 📲.
- Antes del primer handshake de un chat nuevo se exige inventario y lectura exhaustiva de todo el repositorio versionado legible; el handshake aparece en todas las respuestas READY y el número permanece fijo por hilo visible de chat.
- Repositorio como fuente técnica de verdad.
- Candado `LOCKED_READ_ONLY`: un chat puede quedar READY tras sincronizar, pero solo modifica con una tarea autorizada; la autorización es por objetivo y el agente asume pruebas, revisión, memoria y handoff sin microgestión del usuario.
- Revisores/agentes aplicables de `docs/REVIEW_ROLES.md` son automáticos en tareas significativas cuando reduzcan errores o aceleren validación útil.
- Prioridad máxima a seguridad, privacidad e integridad.
- Aplicación Android local-first orientada a contenido temporal.
- OclAx mantiene una bandeja temporal, pero el alcance aprobado ahora incluye una superficie **Mi dispositivo** para explorar contenido real del teléfono con permisos amplios cuando el usuario los conceda.
- Autolimpieza limitada exclusivamente a copias privadas de OclAx.
- Retención predeterminada de 24 horas; Fijados nunca expiran mientras sigan fijados.

## Gobernanza vigente

- protocol v6 fusionado en `main` mediante PR #51;
- registro administrativo alineado a protocol 6 y conserva `last_confirmed_chat: 3`;
- `docs/MASTER_RULES_CHATGPT_GITHUB_V4.txt`, AGENTS.md, REVIEW_ROLES y las instrucciones compactas del Project quedan alineados;
- bootstrap requiere la frase exacta `BOOTSTRAP AUTORIZADO`;
- autorización por objetivo mantiene autonomía técnica completa dentro de límites seguros;
- revisores/agentes son obligatorios de forma condicional cuando la matriz aplica y aporta valor; roles irrelevantes no se ejecutan;
- instrucciones compactas permanecen bajo el límite de 8000 caracteres.

## Qué funciona

- recepción por Share Sheet;
- almacenamiento local privado;
- publicación de texto/imagen al portapapeles;
- DocumentsProvider de solo lectura visible en Archivos;
- búsqueda y filtros por tipo;
- Fijados y retención configurable;
- tema Material 3: oscuro negro/naranja y claro con acento naranja;
- tests unitarios, lint/build/CI y APK debug;
- firma estable de APK de pruebas;
- caché de runtimes Syncthing nativos en CI con verificación SHA-256 obligatoria antes de empaquetar.

## Validación física confirmada

- OclAx aparece como fuente en el selector de archivos del sistema;
- en Qwen funciona Compartir → OclAx → Pegar;
- los tipos de archivo probados pueden seleccionarse e insertarse desde OclAx;
- Qwen rechaza APK como adjunto aunque OclAx lo expone correctamente: limitación de la app receptora;
- una build posterior se instaló encima de la versión con firma estable sin conflicto;
- la actualización conservó los datos internos;
- miniaturas de imágenes en las tarjetas de Recientes/OclAx confirmadas físicamente en dispositivo;
- en un teléfono real **Probar motor** devuelve Device ID + `loopback verificado` después del fix XML;
- con la build main run 207, el scroll completo de OclAx quedó confirmado físicamente;
- con la build main run 207, **Mi dispositivo** muestra fecha y hora de modificación físicamente en el teléfono;
- portada de PDF, apertura de PDF/Word/imagen/video/audio/APK, borrado/cancelación de imagen y PDF/documento, Apps instaladas y persistencia Lista/Cuadrícula fueron confirmados físicamente.
- con main run 229, el selector propio **Elegir con OclAx** fue revalidado físicamente y el dueño confirmó que la presentación corregida ya aparece bien;
- con la build que incluye PR #66, dos dispositivos emparejados llegaron físicamente a **Conectado por LAN**;
- en ambos dispositivos, **Desconectar LAN** devolvió el estado a **Motor aislado / Desconectado · motor aislado**. TRANSFER-003 queda físicamente validado;
- la causa de que Local Discovery broadcast no cruzara en la prueba anterior sigue **NO VERIFICADA**; la validación actual demuestra conectividad OclAx LAN, no cuál ruta concreta produjo el enlace.

## Implementado recientemente

- selector compacto de categorías y tarjetas/acciones compactas;
- Copiar limitado a Texto/Código e Imágenes;
- apertura directa de contenido mediante aplicaciones del sistema;
- superficie **Mi dispositivo** con inventario completo de aplicaciones mediante `PackageManager.getInstalledApplications(0)` + `QUERY_ALL_PACKAGES` y archivos reales mediante MediaStore;
- miniaturas para imagen/video/PDF, lista/cuadrícula recordada por categoría, exportación segura de APK y eliminación explícita de originales;
- fecha y hora de modificación visible en las tarjetas de **Mi dispositivo** mediante PR #58; main run 203 verde;
- base local de **Mis dispositivos**, Device ID compartible y preferencia **Permitir sin aceptar**;
- prueba LAN explícita por peer sin compartir carpetas ni archivos;
- diagnóstico LAN mejorado fusionado: distingue peer no descubierto, descubierto sin conexión y pausado, sin exponer IP;
- PR #57 añadió caché del runtime nativo; main run 200 pobló el caché y runs posteriores restauran el runtime sin recompilar las cuatro ABI, manteniendo verificación SHA-256;
- ERR-005 reconciliado con la implementación vigente mediante PR #59; main run 205 terminó verde.
- selector propio `ACTION_GET_CONTENT` implementado en PR #62 y corregido visualmente en PR #63/main run 229; la apertura y presentación base ya fueron confirmadas físicamente.
- navegación visual por tarjetas de categoría + buscador directo fue fusionada mediante PR #64; main run 232 quedó verde. Falta validación física específica de la navegación nueva.
- paleta semántica por categoría aprobada visualmente y fusionada mediante PR #69: PDF rojo, Apps/APK verde, Documentos azul, Imágenes verde, Texto/Código morado, Video magenta, Fijados dorado, Audio cian y Todo/Recientes naranja; main run 273 quedó verde y la validación visual física sigue pendiente.

## En desarrollo

- miniaturas de imágenes dentro de la bandeja OclAx ya validadas físicamente; siguen pendientes video/PDF y otras validaciones de Mi dispositivo;
- TRANSFER-001 continúa con Syncthing core v2.1.5 detrás de una capa propia;
- runtime Android está fusionado para arm64-v8a, armeabi-v7a, x86_64 y x86 con foreground service on-demand, REST loopback y API key privada;
- el arranque seguro mantiene discovery global/local, relay y NAT desactivados en modo aislado; telemetría y crash reporting desactivados;
- conexión LAN-only está implementada con peer pausado por defecto, discovery local temporal, listener TCP restringido a redes privadas y retorno fail-closed a modo aislado;
- el parser XML incompatible en Android fue corregido y validado físicamente en un teléfono;
- el panel técnico, búsqueda, filtros y tarjetas comparten un único scroll vertical; PR #55/main run 196 y validación física con main run 207 confirman desplazamiento completo;
- TRANSFER-003 queda **DONE** para la conectividad LAN: dos dispositivos conectaron físicamente y ambos restauraron aislamiento al desconectar;
- TRANSFER-004 está **IMPLEMENTED_PENDING_VALIDATION**: PR #68 ya fue fusionado; el primer canal real OclAx↔OclAx sobre LAN verificada incluye carpetas efímeras Syncthing privadas, aceptación/autoaceptación por dispositivo, progreso, importación a ItemStore y cleanup;
- la transferencia real pasó CI de main run 269 en verde y requiere validación física de envío/recepción antes de declararse terminada.
- PICKER-001 permanece **IMPLEMENTED_PENDING_VALIDATION**: apertura/presentación ya están confirmadas; todavía faltan cancelar, MIME/permiso negado y retorno de más variantes. La selección múltiple existente es compatibilidad técnica del intent externo, no una función de producto propuesta por el dueño ni un gate prioritario.
- dos dispositivos físicos ya están disponibles y la build main run 234 fue probada en ambos. En ambos extremos el diagnóstico confirmó **discovery IPv4 local activo + listener LAN activo**, pero ninguno vio al otro por discovery local.
- **CAUSA DEL FALLO DE DISCOVERY: NO VERIFICADA.** La evidencia es compatible con filtrado/aislamiento de broadcast de la Wi‑Fi, pero no lo demuestra por sí sola.
- para no depender exclusivamente de discovery broadcast, el fallback LAN directo y acotado ya está fusionado en PR #66: después de una ventana corta de discovery, OclAx inspecciona solo la red Wi‑Fi/Ethernet local, sondea únicamente TCP/22000 dentro del segmento inmediato (máximo /24) y entrega candidatos privados a Syncthing, que sigue verificando el Device ID; main run 237 quedó verde.

## Bloqueos

- **TRANSFER-004:** PR #68 ya está fusionado y main run 269 quedó verde; no se considera DONE hasta la prueba física de envío, aceptación/rechazo, autoaceptación, progreso, recepción y cleanup;
- el dueño confirmó que Mi dispositivo funciona correctamente en las rutas probadas y que compartir app/APK funciona; siguen pendientes el caso explícito de APK con splits, revocación de permisos y rendimiento con inventarios grandes;
- Internet directo y relay continúan fuera de alcance de este bloque; no se habilitan global discovery, NAT ni relay.

## Siguiente paso exacto

1. Instalar en este dispositivo la próxima build de `main` que incluya la paleta semántica y confirmar visualmente las categorías en modo oscuro/claro.
2. Cuando vuelvan a estar disponibles ambos dispositivos, instalar en ambos la misma build de `main` y conectar por LAN.
3. Enviar un archivo pequeño desde una tarjeta OclAx al dispositivo emparejado.
4. Con **Permitir sin aceptar** apagado, confirmar solicitud visible, **Aceptar**, progreso, aparición en la bandeja OclAx del receptor y confirmación final del emisor.
5. Repetir con **Rechazar** y con **Permitir sin aceptar** activado; verificar que nunca se autoejecuta/instala contenido.
6. Tras terminar, desconectar LAN en ambos y confirmar retorno a modo aislado; Internet/relay queda para una autorización futura.

## Navegación Atrás — PR #71

**IMPLEMENTADO_PENDIENTE_VALIDACIÓN:** PR #71 está fusionado y main run 281 quedó verde. En prueba física con run 281, el gesto Atrás desde PDF volvió correctamente al nivel anterior y la salida desde la raíz fue confirmada como correcta. Quedan por validar físicamente Mi dispositivo, el picker propio y, cuando aplique, el botón Atrás además del gesto.
