# PROJECT_STATE

## Estado

**Fase:** primera prueba vertical Android.  
**Aplicación:** base funcional validada por CI; validación física en progreso.  
**Repositorio:** público.  
**Protocol version:** 2.

## Qué ya está definido

- Nombre: OclAx.
- Significado: Open Content, Local Access & eXchange.
- Identidad del agente: OclAx 📲.
- Repositorio como fuente técnica de verdad.
- Prioridad máxima a seguridad, privacidad e integridad.
- Aplicación Android local-first orientada a contenido temporal.
- OclAx es selector/bandeja ordenada para insertar, no gestor de archivos completo.
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
- Qwen rechaza APK como adjunto aunque OclAx lo expone correctamente: limitación de la app receptora.

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

- miniaturas reales para contenido visual cuando aporten valor;
- evaluar una organización opcional por categorías dentro de DocumentsProvider sin sacrificar el acceso directo a Recientes.

## Bloqueos

Ninguno conocido.

## Siguiente paso exacto

1. Instalar la nueva build sobre la instalación con firma estable, sin desinstalar, para validar actualización real.
2. Confirmar que Android conserva los datos internos al actualizar encima.
3. Validar físicamente selector compacto, Copiar texto/imagen, Compartir, Eliminar con confirmación, retención y Fijados.
4. Validar físicamente el selector de retención compacto, la categoría Aplicaciones y los iconos reales.
5. Confirmar que APK y Aplicaciones se perciben como categorías distintas.
6. Continuar con miniaturas y categorías opcionales en DocumentsProvider sin sacrificar Recientes.
