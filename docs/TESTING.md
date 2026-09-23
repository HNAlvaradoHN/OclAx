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
- riel derecho completo y usable en modo claro/oscuro y en pantallas de distinta altura.

Hasta completar esa validación, APP-001 permanece como:
`IMPLEMENTED_PENDING_VALIDATION`.
