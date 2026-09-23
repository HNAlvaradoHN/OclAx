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

## Validación física

La automatización NO demuestra compatibilidad entre aplicaciones.

VERIFICADO en teléfono real:
- OclAx aparece en el selector de archivos del sistema como una fuente disponible mediante DocumentsProvider;
- el flujo Compartir → OclAx → Pegar funciona en Qwen.

Pendiente de probar:
- compartir texto → OclAx → pegar;
- compartir imagen → OclAx → pegar;
- compartir PDF/APK/documento → OclAx;
- otra app → + / Archivos → OclAx → Recientes → seleccionar;
- búsqueda dentro de la raíz OclAx;
- comportamiento con archivo grande y poco almacenamiento;
- Qwen, WhatsApp, Telegram y navegador.

Hasta completar esa validación, APP-001 permanece como:
`IMPLEMENTED_PENDING_VALIDATION`.
