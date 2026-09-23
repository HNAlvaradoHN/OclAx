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
- selector compacto de categorías alineado a la izquierda: abrir, seleccionar, cierre automático y cierre al tocar fuera;
- naranja vivo y contraste en modo claro/oscuro;
- tarjetas y acciones compactas en distintos tamaños de pantalla;
- Copiar texto e imagen desde una tarjeta y pegar en aplicaciones compatibles;
- verificar que PDF, APK, documentos, video, audio y otros no muestren Copiar;
- comprobar que la categoría Aplicaciones muestra apps lanzables con nombre e icono real, ordenadas alfabéticamente;
- comprobar que APK y Aplicaciones permanecen separadas;
- comprobar búsqueda por nombre de aplicación.

Hasta completar esa validación, APP-001 permanece como:
`IMPLEMENTED_PENDING_VALIDATION`.


## Firma estable de pruebas

VERIFICADO en CI:
- los cuatro GitHub Actions Secrets se reconocen y se muestran enmascarados;
- el keystore se reconstruye en el almacenamiento temporal del runner;
- el build de main con firma persistente terminó verde;
- se publicó el artefacto APK firmado.

Pendiente en dispositivo:
- desinstalar una última vez la instalación antigua firmada con una clave efímera;
- instalar el primer APK con firma persistente;
- generar una build posterior y confirmar que Android la instala encima sin conflicto y conserva los datos internos.


## Bloque compacto + Copiar

VERIFICADO en CI:
- tests unitarios verdes;
- lint verde;
- assembleDebug verde;
- test de elegibilidad confirma que Copiar solo está disponible para Texto/Código e Imágenes;
- build de main generado con la firma persistente de pruebas.

Pendiente en dispositivo:
- instalar esta build encima de la instalación estable anterior sin desinstalar;
- comprobar conservación de copias/preferencias internas;
- probar cierre del menú al seleccionar y al tocar fuera;
- copiar texto e imagen y pegar en aplicaciones compatibles.


## Validación física — actualización estable y apps

VERIFICADO por el dueño:
- la APK nueva se instaló encima de la anterior sin conflicto;
- los datos internos se conservaron.

FALLO observado:
- la primera implementación de Aplicaciones no mostró las apps esperadas;
- Archivos → OclAx continuó mostrando una raíz plana de recientes.

Corrección a validar:
- Apps instaladas mediante LauncherApps;
- etiquetas Apps instaladas / APK guardados;
- carpetas virtuales por categoría dentro de DocumentsProvider manteniendo recientes directos.


## Abrir contenido desde OclAx

Pendiente de validación física:
- tocar PDF y abrirlo con visor compatible;
- tocar Word/OOXML y abrirlo con editor/visor instalado;
- tocar imagen y abrirla con galería/visor;
- tocar video/audio y abrirlo con reproductor;
- tocar APK y llegar al instalador/manejador del sistema;
- comprobar que Android respeta una app predeterminada o muestra resolución cuando corresponda;
- comprobar mensaje seguro cuando no existe manejador compatible;
- confirmar que ninguna apertura concede escritura ni modifica la copia OclAx.


## Mi dispositivo

Pendiente de validación física:
- entrar a Mi dispositivo y comprobar que Apps lista aplicaciones instaladas reales, incluidas las que antes no eran visibles;
- seleccionar Imágenes/Documentos/PDF/APK/Texto-Código/Video/Audio/Otros y comprobar solicitud de acceso amplio;
- conceder acceso desde Ajustes y volver a OclAx;
- comprobar que las categorías muestran contenido real del almacenamiento;
- buscar por nombre/ruta;
- abrir y compartir archivos reales;
- copiar texto e imágenes;
- revocar acceso amplio y confirmar que OclAx conserva su bandeja y que Mi dispositivo vuelve a pedir permiso;
- confirmar que no existe una acción de borrado de originales;
- observar tiempo de carga y fluidez con un dispositivo con muchos archivos.
