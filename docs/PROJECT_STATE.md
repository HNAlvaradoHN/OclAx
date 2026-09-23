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

- rama de trabajo con selector compacto de categorías a la izquierda;
- Copiar limitado a Texto/Código e Imágenes;
- tarjetas y acciones más compactas;
- naranja más vivo en oscuro y claro;
- acciones por tarjeta: Compartir, Copiar cuando aplique, Fijar y Eliminar con confirmación;
- firma persistente de pruebas mediante GitHub Actions Secrets;
- versionCode monotónico en CI;
- APK firmado estable generado correctamente en main.

## En desarrollo

- iconografía/miniaturas finales;
- evaluar una organización opcional por categorías dentro de DocumentsProvider sin sacrificar el acceso directo a Recientes.

## Bloqueos

Ninguno conocido.

## Siguiente paso exacto

1. Instalar físicamente el primer APK con firma estable; esta transición requiere desinstalar la versión antigua una última vez.
2. Verificar que una build posterior pueda instalarse encima sin conflicto de paquete.
3. Validar físicamente selector compacto, Copiar texto/imagen, Compartir, Eliminar con confirmación, retención y Fijados.
4. Continuar con iconografía/miniaturas y categorías opcionales en DocumentsProvider sin sacrificar Recientes.
