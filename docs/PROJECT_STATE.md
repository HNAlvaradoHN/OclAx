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
- con la build main run 207, **Mi dispositivo** muestra fecha y hora de modificación físicamente en el teléfono.

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

## En desarrollo

- miniaturas de imágenes dentro de la bandeja OclAx ya validadas físicamente; siguen pendientes video/PDF y otras validaciones de Mi dispositivo;
- TRANSFER-001 continúa con Syncthing core v2.1.5 detrás de una capa propia;
- runtime Android está fusionado para arm64-v8a, armeabi-v7a, x86_64 y x86 con foreground service on-demand, REST loopback y API key privada;
- el arranque seguro mantiene discovery global/local, relay y NAT desactivados en modo aislado; telemetría y crash reporting desactivados;
- conexión LAN-only está implementada con peer pausado por defecto, discovery local temporal, listener TCP restringido a redes privadas y retorno fail-closed a modo aislado;
- el parser XML incompatible en Android fue corregido y validado físicamente en un teléfono;
- el panel técnico, búsqueda, filtros y tarjetas comparten un único scroll vertical; PR #55/main run 196 y validación física con main run 207 confirman desplazamiento completo;
- el diagnóstico LAN mejorado ya está fusionado y validado automáticamente; falta validación física con dos teléfonos;
- TRANSFER-003 sigue **IMPLEMENTED_PENDING_VALIDATION** hasta validar el segundo teléfono y luego la conexión LAN;
- todavía no existe transferencia de archivos: el canal privado + progreso permanece bloqueado hasta validar LAN con dos teléfonos.

## Bloqueos

- **TRANSFER-001/003:** falta confirmar el motor en el segundo teléfono y completar la prueba LAN física con ambos dispositivos en la misma Wi-Fi;
- siguen pendientes validaciones físicas del borrado corregido, PDF/apps y persistencia de vistas en Mi dispositivo;
- Internet directo, relay y canal real de archivos no deben implementarse antes de validar físicamente LAN según la autorización vigente.

## Siguiente paso exacto

1. Cuando esté disponible el segundo teléfono, instalar exactamente la build main run 207 o una posterior equivalente y confirmar Device ID + loopback.
2. Compartir/agregar mutuamente los Device ID y validar **Mis dispositivos** + **Permitir sin aceptar**.
3. En la misma Wi-Fi, tocar **Probar LAN** en ambos dentro de la misma ventana. Si no conecta, registrar el diagnóstico exacto: `no apareció en discovery local`, `apareció pero no conectó` o `quedó pausado`.
4. Confirmar **Desconectar LAN** y retorno a modo aislado.
5. Solo después de validar LAN, diseñar/implementar el canal privado de archivos + progreso; Internet/relay continúa fuera de alcance hasta esa validación.
6. Mantener en paralelo las validaciones físicas pendientes de PDF/borrado/apps y persistencia de vistas en Mi dispositivo.