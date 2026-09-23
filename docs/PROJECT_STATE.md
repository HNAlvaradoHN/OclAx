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
- la primera versión de apps evitó `QUERY_ALL_PACKAGES`; esa restricción fue sustituida después al aprobarse Mi dispositivo completo.

## Implementado recientemente

- apertura directa de contenido desde tarjetas mediante aplicaciones del sistema;
- superficie **Mi dispositivo** separada de la bandeja OclAx;
- acceso amplio al almacenamiento compartido mediante permiso especial de Android;
- visibilidad completa de aplicaciones mediante QUERY_ALL_PACKAGES;
- categorías del dispositivo: Apps, Imágenes, Documentos, PDF, APK, Texto/Código, Video, Audio y Otros;
- búsqueda, abrir y compartir desde contenido real;
- Copiar limitado a texto e imágenes también en Mi dispositivo;
- ningún borrado de originales desde Mi dispositivo.

## Implementado recientemente

- miniaturas reales para imágenes/video en Mi dispositivo;
- miniaturas expuestas por DocumentsProvider al selector de Android;
- vista Lista/Cuadrícula recordada por categoría en Mi dispositivo;
- compartir aplicaciones instaladas exportando solo APK base + splits, sin datos privados;
- eliminación explícita de originales con confirmación OclAx y autorización Android cuando corresponde;
- límites de espacio para exportaciones temporales de APK;
- PR #34 fusionado tras tests, lint y build verdes.

## En desarrollo

- corrección tras prueba física: portada real de PDF mediante primera página renderizada;
- corrección del borrado de originales en Android 11+;
- validación física del bloque Mi dispositivo;
- prototipo OclAx ↔ OclAx usando Syncthing como motor candidato después de cerrar esta validación.

## Bloqueos

- la build probada físicamente no podía borrar originales: Android rechazaba la URI genérica de MediaStore.Files;
- la corrección está implementada y debe validarse físicamente antes de avanzar al bloque de transferencia.

## Siguiente paso exacto

1. Instalar la build corregida y confirmar portada de PDF en Mi dispositivo y Archivos → OclAx.
2. Borrar y cancelar borrado de una imagen y de un PDF/documento; confirmar que el original desaparece solo cuando se acepta y que la bandeja OclAx queda intacta.
3. Validar compartir una app de APK único y otra con splits confirmando que no viajan datos privados.
4. Confirmar que Lista/Cuadrícula se recuerda de forma independiente por categoría.
5. Confirmar rendimiento de miniaturas con muchas imágenes/videos/PDF.
6. Después de esa validación, prototipar Syncthing como motor de **Enviar a dispositivo** con directo/LAN/Internet y relay público como fallback.
7. Implementar confianza por dispositivo: Permitir sin aceptar para dispositivos elegidos; preguntar por defecto para otros.
