# FEASIBILITY — Validación técnica y panorama de alternativas

**Fecha de revisión:** 2026-09-22  
**Estado:** investigación técnica; no sustituye pruebas reales en dispositivo.

## Conclusión

VERIFICADO: el núcleo de OclAx es técnicamente viable en Android moderno.

La propuesta no debe definirse como “un clipboard manager que captura todo automáticamente”, porque Android 10+ impide a una app normal leer el portapapeles en segundo plano salvo que tenga foco o sea el IME activo.

La propuesta correcta es:

> una bandeja local y temporal de contenido mixto que recibe contenido de forma explícita, lo organiza, lo expone al selector de archivos de Android y permite volver a compartir/copiarlo rápidamente.

## Flujo viable

### Entrada a OclAx

Vías robustas:
- Android Share Sheet: ACTION_SEND / ACTION_SEND_MULTIPLE;
- importar mediante selector;
- agregar texto manualmente;
- leer el portapapeles mientras OclAx tiene foco;
- ACTION_PROCESS_TEXT para texto seleccionado, si se decide incorporar;
- otras integraciones explícitas que no requieran vigilancia oculta.

No prometer:
- captura universal automática de todo lo copiado en segundo plano.

### Almacenamiento interno

Viable:
- archivos en almacenamiento privado de la app;
- metadatos separados;
- miniaturas/previsualizaciones;
- texto, imágenes, PDF, APK, video, código y archivos arbitrarios;
- TTL / límite por tamaño o cantidad;
- fijar elementos para evitar su limpieza.

La limpieza puede combinar:
- limpieza perezosa al abrir/usar la app;
- trabajo programado cuando sea necesario.

### Salida desde OclAx

Viable:
- Share Sheet;
- copiar texto al portapapeles;
- copiar imágenes/URI al portapapeles cuando corresponda;
- DocumentsProvider para aparecer como ubicación en el selector de archivos del sistema;
- URI grants de lectura al receptor;
- opcionalmente IME/commitContent en una fase posterior.

## DocumentsProvider

Android permite a proveedores locales o en nube participar en Storage Access Framework mediante DocumentsProvider.

Esto permite que OclAx aparezca como una raíz/ubicación dentro del selector de archivos del sistema para las apps que usen SAF.

Límite importante:
- una app que use un selector propio en vez del selector del sistema puede no mostrar OclAx;
- por tanto, “aparece en todas las apps” NO es una promesa válida.

Los elementos temporales pueden eliminarse. Si un receptor conserva una URI persistente y OclAx elimina el elemento por TTL, esa referencia dejará de resolver. Para evitar sorpresas, los elementos fijados no deberían expirar automáticamente.

## Pegado directo

### Texto
Muy compatible mediante ClipboardManager o IME.

### Imágenes / contenido rico
InputConnection.commitContent permite que un IME entregue contenido como imágenes a un editor, pero el receptor debe anunciar/aceptar el MIME correspondiente.

Por tanto:
- no existe un “pegar cualquier archivo en cualquier app” universal;
- el camino confiable para archivos es Share Sheet o selector de archivos;
- rich paste puede añadirse como mejora, no como base del MVP.

## Comparables encontrados

### Clipboard Hero
Repositorio público MIT, Kotlin + Jetpack Compose.

Demuestra un patrón muy relevante:
- recibe imágenes por Share Sheet;
- copia los bytes a almacenamiento propio;
- expone la imagen mediante FileProvider;
- conserva historial corto;
- ofrece autoeliminación;
- funciona localmente sin red.

Es una referencia técnica útil para la filosofía de OclAx, especialmente almacenamiento temporal, URI propias y limpieza.

No reemplaza OclAx porque está enfocado en imágenes y portapapeles, no en una bandeja mixta accesible como proveedor de documentos.

### PasteFromClipboard
Repositorio público MIT.

Demuestra el concepto de presentarse como origen de contenido cuando otra app solicita ACTION_GET_CONTENT y devolver una URI procedente del portapapeles.

Es una prueba conceptual interesante, pero el propio proyecto indica limitaciones y su enfoque de permisos de portapapeles debe revisarse frente a las restricciones modernas de Android. No debe copiarse ciegamente.

### Easy Clipboard
Proyecto de clipboard history con IME, ACTION_PROCESS_TEXT, accesibilidad y Shizuku opcional.

Valor para OclAx:
- confirma las restricciones reales del portapapeles moderno;
- muestra que IME/PROCESS_TEXT son caminos posibles.

No es el mismo producto: está centrado en historial/paste de texto y captura global.

Antes de reutilizar código debe confirmarse la licencia real del repositorio; su README dice que MIT “se añadirá”.

### File Shelf
Aplicación de Play Store enfocada en organizar archivos de cualquier tipo recibidos por Share Sheet.

Se acerca al concepto de “bandeja”, pero la información pública revisada no demuestra una raíz DocumentsProvider, historial de portapapeles mixto o filosofía temporal equivalente.

### ClipSync
Aplicación de Play Store que organiza texto, imágenes y archivos, con búsqueda y autolimpieza.

Es más un clipboard manager permanente/organizador; incluye anuncios/compras y no se encontró evidencia pública de que se exponga como ubicación de DocumentsProvider.

## Señales de necesidad observadas

En Reddit aparecen usuarios que buscan:
- historial local sin nube;
- límites por tamaño/retención;
- texto + imágenes;
- mejores formas de mover archivos entre apps;
- alternativas porque los clipboard managers tradicionales chocan con restricciones de Android 10+.

Esto demuestra que el problema existe.

NO demuestra todavía:
- tamaño de mercado;
- disposición a pagar;
- que el conjunto exacto de OclAx tenga demanda masiva.

## Diferenciación encontrada

En esta búsqueda no apareció una solución pública claramente equivalente que reúna a la vez:

1. bandeja local temporal;
2. texto + imágenes + PDFs + APK + videos + código + archivos arbitrarios;
3. recepción por Share Sheet;
4. ubicación propia mediante DocumentsProvider;
5. limpieza automática;
6. interfaz tipo gestor visual;
7. sin nube como requisito.

Esto no prueba que no exista ninguna en todo Internet, pero sí indica que OclAx no es simplemente una copia obvia de una solución dominante encontrada.

## Riesgos reales del producto

1. **Compatibilidad por app:** algunas apps usan pickers propios.
2. **Clipboard Android 10+:** no se puede vigilar globalmente sin IME/foco o mecanismos especiales.
3. **MIME:** el receptor puede filtrar tipos.
4. **Contenido rico:** commitContent depende del receptor.
5. **TTL:** borrar un elemento invalida URIs guardadas.
6. **Archivos grandes:** video/APK necesitan límites de almacenamiento y streaming, no cargar todo a RAM.
7. **Seguridad:** archivos recibidos son datos no confiables; OclAx no debe ejecutarlos ni interpretarlos de forma peligrosa.

## Dirección recomendada del MVP

La prioridad de producto queda definida por este flujo:

`capturar/seleccionar/tener archivo → guardar rápidamente en OclAx → otra app → + / Archivos → OclAx → Recientes → insertar`

OclAx debe comportarse como una **bandeja temporal universal**, no como un gestor de carpetas tradicional.

El MVP debe validar primero el flujo más valioso y menos frágil:

1. recibir texto/imagen/archivo mediante Share;
2. mostrar una bandeja ordenada y local;
3. volver a copiar/compartir;
4. exponer archivos mediante DocumentsProvider;
5. autolimpieza configurable;
6. fijar elementos para impedir expiración;
7. cero red.

Dejar para después:
- teclado/IME propio;
- accesibilidad;
- Shizuku;
- sincronización entre dispositivos;
- nube;
- IA;
- indexación compleja;
- edición avanzada.

## Prueba crítica antes de ampliar

En un teléfono real verificar:
- Qwen;
- WhatsApp;
- Telegram;
- navegador;
- gestor de archivos;
- al menos una app que use ACTION_OPEN_DOCUMENT.

Para cada app comprobar:
- si OclAx aparece en el picker;
- qué MIME acepta;
- si importa una copia o conserva la URI;
- comportamiento después de la limpieza del elemento.
