# PROJECT_STATE

## Estado

**Fase:** primera prueba vertical Android.  
**Aplicación:** base funcional validada por CI; validación física en progreso.  
**Repositorio:** público.  
**Protocol version:** 2.

## Qué ya está definido

- OclAx debe poder reconstruir el estado de trabajo desde GitHub al iniciar cualquier chat nuevo; las decisiones duraderas se registran aquí/TASKS/DECISIONS/SECURITY/ARCHITECTURE antes del handoff.
- Nombre: OclAx.
- Significado: Open Content, Local Access & eXchange.
- Identidad del agente: OclAx 📲.
- Repositorio como fuente técnica de verdad.
- Prioridad máxima a seguridad, privacidad e integridad.
- Aplicación Android local-first orientada a contenido temporal.
- OclAx mantiene una bandeja temporal, pero el alcance aprobado ahora incluye una superficie **Mi dispositivo** para explorar contenido real del teléfono con permisos amplios cuando el usuario los conceda.
- Autolimpieza limitada exclusivamente a copias privadas de OclAx.
- Retención predeterminada de 24 horas; Fijados nunca expiran mientras sigan fijados.

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
- la actualización conservó los datos internos.

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
- visibilidad de apps sin permiso amplio `QUERY_ALL_PACKAGES`.

## En desarrollo

- corrección física de Apps instaladas mediante LauncherApps;
- categorías virtuales en DocumentsProvider, conservando acceso directo a Recientes;
- miniaturas reales para contenido visual cuando aporten valor;
- diseño e implementación de Mi dispositivo con acceso amplio al contenido real;
- prototipo OclAx ↔ OclAx usando Syncthing como motor candidato.

## Bloqueos

Ninguno conocido.

## Siguiente paso exacto

1. Implementar **Mi dispositivo** como superficie separada para contenido real: aplicaciones, imágenes, videos, audio, PDF, documentos, APK y otros.
2. Solicitar únicamente los permisos amplios necesarios para esa superficie y degradar funcionalidad si el usuario los niega/revoca.
3. Mantener intacto el límite: autolimpieza y borrado interno solo afectan copias OclAx.
4. Prototipar Syncthing como motor de **Enviar a dispositivo** sin exponer sincronización de carpetas al usuario.
5. Implementar confianza por dispositivo: Permitir sin aceptar para dispositivos elegidos; preguntar por defecto para otros.
6. Continuar miniaturas/iconos y validación física.
