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
- CI de `main` run 188 terminó verde en runtime nativo, tests, lint, build multi-ABI, verificación de runtimes y publicación de APK;
- resincronización exhaustiva bajo protocol v6 completada en este chat sin incrementar el número de sesión;
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
- tests unitarios, lint/build/CI y APK debug.

## Validación física confirmada

- OclAx aparece como fuente en el selector de archivos del sistema;
- en Qwen funciona Compartir → OclAx → Pegar;
- los tipos de archivo probados pueden seleccionarse e insertarse desde OclAx;
- Qwen rechaza APK como adjunto aunque OclAx lo expone correctamente: limitación de la app receptora;
- una build posterior se instaló encima de la versión con firma estable sin conflicto;
- la actualización conservó los datos internos;
- miniaturas de imágenes en las tarjetas de Recientes/OclAx confirmadas físicamente en dispositivo.

## Implementado recientemente

- selector compacto de categorías a la izquierda, fusionado en main;
- Copiar limitado a Texto/Código e Imágenes;
- tarjetas e iconos visualmente más compactos, manteniendo áreas táctiles accesibles;
- naranja más vivo en oscuro y claro;
- CI verde en PR y main para este bloque;
- acciones por tarjeta: Compartir, Copiar cuando aplique, Fijar y Eliminar con confirmación;
- firma persistente de pruebas mediante GitHub Actions Secrets;
- versionCode monotónico en CI;
- APK firmado estable generado correctamente en main.

## Implementado recientemente

- control de retención visualmente más compacto tras prueba física;
- categoría Aplicaciones separada de APK;
- aplicaciones lanzables del dispositivo ordenadas alfabéticamente con iconos reales;
- iconos reconocibles por tipo en las tarjetas de OclAx;
- la primera versión de apps evitó `QUERY_ALL_PACKAGES`; esa restricción fue sustituida después al aprobarse Mi dispositivo completo.

## Implementado recientemente

- apertura directa de contenido desde tarjetas mediante aplicaciones del sistema;
- superficie **Mi dispositivo** separada de la bandeja OclAx;
- acceso amplio al almacenamiento compartido mediante permiso especial de Android;
- visibilidad completa de aplicaciones mediante QUERY_ALL_PACKAGES;
- categorías del dispositivo: Apps, Imágenes, Documentos, PDF, APK, Texto/Código, Video, Audio y Otros;
- búsqueda, abrir y compartir desde contenido real;
- Copiar limitado a texto e imágenes también en Mi dispositivo;
- en la primera versión de Mi dispositivo no había borrado de originales; DEVICE-002 añadió después **Eliminar original** explícito y confirmado.

## Implementado recientemente

- miniaturas reales para imágenes/video en Mi dispositivo;
- miniaturas expuestas por DocumentsProvider al selector de Android;
- vista Lista/Cuadrícula recordada por categoría en Mi dispositivo;
- compartir aplicaciones instaladas exportando solo APK base + splits, sin datos privados;
- eliminación explícita de originales con confirmación OclAx y autorización Android cuando corresponde;
- límites de espacio para exportaciones temporales de APK;
- PR #34 fusionado tras tests, lint y build verdes.

## Implementado recientemente

- miniaturas reales también en las tarjetas internas de OclAx para Imagen, Video y PDF;
- carga asíncrona reutilizando ThumbnailLoader y fallback seguro a icono por tipo.

## Implementado recientemente

- base local de **Mis dispositivos** fusionada en main mediante PR #40;
- agregar/quitar dispositivo por Device ID, nombre visible y preferencia **Permitir sin aceptar**;
- Device ID propio compartible de forma explícita desde Android;
- el siguiente bloque ya implementa una prueba LAN explícita por peer, pero todavía no comparte carpetas ni archivos.

## En desarrollo

- miniaturas de imágenes dentro de la bandeja OclAx ya validadas físicamente; siguen pendientes video/PDF y otras validaciones de Mi dispositivo;
- TRANSFER-001 iniciado: spike técnico para Syncthing core v2.x detrás de una capa propia;
- investigación confirmó que el wrapper Android oficial está archivado, por lo que no se adoptará como dependencia;
- **VERIFICADO:** el spike aislado construyó Syncthing core v2.1.5 para Android arm64/API 26 con NDK r30, sin secretos de firma;
- runtime Android fusionado en main para arm64-v8a, armeabi-v7a, x86_64 y x86: build nativo en job sin secretos, checksums antes de empaquetar, foreground service on-demand, REST loopback y API key privada;
- el arranque seguro genera/configura el motor antes de servir: listener de sincronización solo en loopback, discovery global/local, relay y NAT desactivados; telemetría y crash reporting desactivados;
- el probe valida Device ID, autenticación REST, aislamiento loopback y start/stop;
- **VERIFICADO EN CI MAIN:** tests, lint, build multi-ABI, presencia de los cuatro runtimes y APK firmado estable terminaron verdes en el run 125;
- conexión LAN-only fusionada en main mediante PR #42: peer pausado por defecto, discovery local temporal, listener TCP restringido a redes privadas y retorno fail-closed a modo aislado al desconectar;
- **VERIFICADO EN CI MAIN:** run 150 terminó verde con tests, lint, build multi-ABI, verificación de runtimes y APK debug publicado;
- **FALLO FÍSICO REPRODUCIDO EN DOS TELÉFONOS:** `Probar motor` agotó el tiempo antes de obtener Device ID, por lo que la prueba LAN no puede comenzar aún;
- **PR #47 / MAIN CI VERDE, PERO FALLÓ LA REPRUEBA FÍSICA:** ejecutar Syncthing como proceso interno ya supervisado (`STMONITORED=1`) y usar almacenamiento temporal privado para SQLite no eliminó el timeout; esa hipótesis queda descartada como solución suficiente;
- **DIAGNÓSTICO IMPLEMENTADO Y FUSIONADO · MAIN CI VERDE:** OclAx registra localmente la etapa de arranque, código de salida y una única línea de log sanitizada del intento actual para reemplazar el timeout genérico por evidencia accionable; no añade permisos ni telemetría; la build diagnóstica identificó el fallo en la preparación de configuración privada por una feature XML no soportada en Android;
- **CAUSA DE ARRANQUE VERIFICADA Y FIX FUSIONADO:** el parser XML de Android rechazaba la feature Xerces `disallow-doctype-decl` antes de iniciar Syncthing; PR #53 reemplaza esa dependencia por controles portables que conservan bloqueo de DOCTYPE/ENTITY y resolución externa; PR CI y main run 192 quedaron verdes;
- TRANSFER-003 sigue **IMPLEMENTED_PENDING_VALIDATION** hasta que la nueva build obtenga Device ID + loopback y recién después se pruebe LAN;
- todavía no existe transferencia de archivos: el siguiente bloque será canal privado + progreso solo después de validar LAN.

## Bloqueos

- **TRANSFER-001 BLOQUEADO_PENDIENTE_VALIDACION_FISICA:** la causa inmediata del arranque ya está VERIFICADA y el fix pasó CI/merge/main; falta comprobar en dispositivo que supera la preparación privada y obtiene Device ID + loopback;
- siguen pendientes validaciones físicas del borrado corregido y otros puntos de Mi dispositivo antes de declarar esos bloques DONE.

## Siguiente paso exacto

1. Instalar la APK firmada publicada por **main run 192** en **un teléfono primero**.
2. Tocar **Probar motor** una sola vez.
3. Confirmar que supera la etapa de configuración privada y obtiene Device ID + loopback; si aparece otro fallo, registrar el diagnóstico exacto y corregir esa causa.
4. Solo después probar el segundo teléfono, compartir/agregar mutuamente los Device ID y validar **Mis dispositivos** + **Permitir sin aceptar**.
5. En la misma Wi-Fi, tocar **Probar LAN** en ambos y confirmar **Conectado por LAN**; después desconectar y comprobar retorno a modo aislado.
6. Crear el canal privado de transferencia de archivos + progreso solo después de validar esa conexión.
7. Mantener en paralelo las validaciones pendientes de PDF/borrado/apps/vistas/fecha-hora en Mi dispositivo.
