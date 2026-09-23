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

## En desarrollo

- riel vertical derecho para categorías;
- acciones por tarjeta: Compartir, Fijar y Eliminar con confirmación;
- identidad naranja más visible en oscuro y claro;
- iconografía/miniaturas finales;
- evaluar una organización opcional por categorías dentro de DocumentsProvider sin sacrificar el acceso directo a Recientes.

## Bloqueos

- El soporte para firma persistente de pruebas está preparado en código, pero todavía faltan los cuatro GitHub Actions Secrets del dueño. Hasta que se carguen y se genere el primer APK con esa firma, una build nueva puede requerir desinstalar la anterior.

## Siguiente paso exacto

1. Validar por CI el modelo compartido de tipos.
2. Usar ese modelo como base para iconografía y, si no añade navegación innecesaria, categorías opcionales en DocumentsProvider.
3. Mantener Recientes accesible directamente desde Archivos.
4. Validar físicamente retención, Fijados, contraste claro/oscuro y compatibilidad en más aplicaciones.
