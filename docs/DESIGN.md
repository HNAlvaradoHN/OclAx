# DESIGN — Memoria visual de OclAx

## Estado

Fuente de verdad visual activa. La primera UI Compose ya existe y está evolucionando por bloques verificables.

## Principio

El diseño debe poder evolucionar sin obligar a reescribir lógica, datos o infraestructura.

## Identidad

- Producto: OclAx.
- Significado: Open Content, Local Access & eXchange.
- Concepto: contenido local listo para guardar temporalmente, organizar y mover entre aplicaciones.
- Idea de marca: “Tu contenido, listo donde lo necesitás.”

## Paleta aprobada

Preferencia explícita del dueño:
- modo oscuro: negro/casi negro como base con detalles naranja;
- modo claro: apariencia clara convencional y limpia, conservando naranja como acento de identidad;
- seguir por defecto el tema claro/oscuro del sistema.

Implementación inicial:
- oscuro: fondos `#090909` / `#101010`, superficies elevadas `#1B1B1B`, acento naranja claro `#FFB06A`;
- claro: fondo/superficie claro, acento naranja `#FF7A00`;
- no forzar negro puro en todos los componentes si perjudica jerarquía o contraste.

La paleta puede afinarse tras validación física, manteniendo la identidad negro+naranja en oscuro y claro+naranja en día.

## Pendiente visual

Todavía no se consideran definitivos:
- tipografía;
- iconografía final;
- miniaturas;
- espaciado fino;
- formas;
- animaciones.

No inventar decisiones visuales permanentes y presentarlas como aprobadas.

## Accesibilidad

- contraste suficiente;
- tamaños táctiles adecuados;
- estados claros;
- soporte de lector de pantalla cuando aplique;
- foco/navegación coherentes;
- textos comprensibles;
- no depender solo del color para comunicar tipo o estado.

## Herramientas de diseño

Stitch u otras herramientas pueden usarse para explorar y prototipar.

Reglas:
- no son fuente técnica de verdad;
- no reciben secretos ni datos privados;
- no son requisito para compilar;
- una decisión visual duradera vuelve a este archivo y al código versionado.

## Feedback visual

Cambios no bloqueantes como color, tamaño, espaciado, texto o posición se registran y agrupan para evitar ciclos improductivos.

Los problemas de accesibilidad o de flujo principal sí pueden ser BLOQUEANTES.

## Patrón principal — Selector ordenado

Cuando OclAx se abra desde el selector de archivos de otra app, la prioridad visual es encontrar e insertar rápido.

Orden recomendado:
1. búsqueda visible;
2. Recientes;
3. Fijados;
4. filtros/categorías por tipo;
5. lista o cuadrícula con miniatura/icono, nombre, tipo y fecha.

Categorías actuales de la app:
- Todo;
- Fijados;
- Imágenes;
- Documentos;
- PDF;
- Apps/APK;
- Texto/Código;
- Video;
- Audio;
- Otros.

Evitar:
- árbol de carpetas como pantalla inicial;
- categorías redundantes;
- controles administrativos que distraigan del acto de insertar;
- mezclar sin jerarquía capturas, APK, PDF, documentos y otros archivos.

La interfaz debe favorecer reconocer el contenido visualmente y reducir el número de toques.


## Ajustes aprobados tras prueba física — 2026-09-23

Feedback del dueño después de probar la UI en dispositivo real:

- el naranja debe ser claramente visible como identidad, no solo existir técnicamente en el tema;
- los filtros pasan de barra horizontal a un **riel vertical en el lado derecho** para evitar desplazamiento horizontal y permitir acceso rápido a todas las categorías;
- cada elemento de OclAx se presenta como tarjeta con acciones directas;
- compartir y fijar usan el naranja de identidad;
- eliminar usa color destructivo diferenciado y exige confirmación;
- el tipo de archivo debe tener una señal visual clara dentro de la tarjeta.

Regla UX:
- la barra derecha organiza; no debe quitar protagonismo al contenido;
- si la altura de pantalla no alcanza, el riel puede desplazarse verticalmente, pero nunca volver a depender de desplazamiento horizontal.


## Ajuste aprobado tras segunda prueba física — 2026-09-23

El dueño corrigió y refinó la organización visual:

- el naranja anterior se percibió demasiado claro/durazno; la identidad pasa a un naranja más vivo y saturado;
- el riel derecho implementado previamente queda **sustituido**;
- el selector de categorías queda compacto y anclado al **lado izquierdo**;
- solo se muestra la categoría activa (por defecto, Todo) como control principal;
- al tocarla se abre una lista desplegable con las demás categorías;
- la lista se cierra al seleccionar una categoría o tocar fuera;
- esto devuelve el ancho principal a las tarjetas y evita ocupar una columna permanente;
- tarjetas, badges y acciones se compactan visualmente;
- las acciones visibles son Compartir, Copiar cuando aplique, Fijar/Desfijar y Eliminar;
- Copiar solo aparece para **Texto/Código e Imágenes**.

Accesibilidad:
- los iconos pueden verse más pequeños sin reducir innecesariamente el área táctil;
- cada acción conserva descripción accesible;
- el color no es la única señal de estado.


## Ajuste tras tercera prueba física — 2026-09-23

- El control visual de retención no debe competir con el selector principal.
- La burbuja de tiempo se muestra más baja y corta (por ejemplo, `24 h`) manteniendo un área táctil mayor que su forma visible.
- “Apps/APK” representa APK guardados en OclAx; no debe confundirse visualmente con una futura vista de aplicaciones instaladas del dispositivo.
- La iconografía final debe acercarse al contenido real: miniaturas para imágenes cuando sea viable, iconos reconocibles por tipo y, para una futura vista de aplicaciones instaladas, icono real de cada app cuando Android lo permita.


## Aplicaciones e iconografía — 2026-09-23

- El selector incluye **Aplicaciones** y **APK** como conceptos separados.
- Aplicaciones muestra el icono real y nombre visible de cada app lanzable del dispositivo.
- Las aplicaciones se ordenan alfabéticamente y usan tarjetas compactas.
- Las tarjetas de contenido dejan de depender de siglas como TXT/DOC/APK como señal principal: usan iconos reconocibles por tipo.
- La acción futura al tocar una aplicación no queda implícita en esta pasada; primero se completa y valida la organización visual.


## Corrección tras cuarta prueba física — 2026-09-23

La prueba confirmó que hay dos vistas distintas que no deben confundirse:

- **Apps instaladas** vive dentro de la app OclAx y debe mostrar iconos/nombres reales de aplicaciones lanzables.
- **Archivos → OclAx** es el selector SAF y solo puede representar archivos/copias de OclAx; allí se organizan por carpetas virtuales de tipo, no por aplicaciones instaladas.

Para evitar confusión:
- usar las etiquetas **Apps instaladas** y **APK guardados**;
- mantener recientes visibles directamente en la raíz del proveedor;
- añadir carpetas Fijados, Imágenes, Documentos, PDF, APK, Texto/Código, Video, Audio y Otros;
- no hacer pasar una app instalada por un archivo adjuntable.


## Mi dispositivo y Enviar a dispositivo

La navegación futura debe distinguir visualmente dos orígenes:

- **OclAx**: bandeja temporal, Fijados y contenido recibido/importado;
- **Mi dispositivo**: contenido original del teléfono.

Mi dispositivo prioriza categorías reconocibles:
- Apps;
- Imágenes;
- Videos;
- Audio;
- PDF;
- Documentos;
- APK;
- Otros.

El usuario debe poder reconocer en todo momento si está actuando sobre una copia OclAx o sobre un original del dispositivo.

### Transferencia OclAx ↔ OclAx

Flujo visible:
1. seleccionar contenido;
2. Enviar;
3. elegir dispositivo;
4. ver progreso.

En ajustes de cada dispositivo emparejado:
- **Permitir sin aceptar** — interruptor explícito;
- cuando está apagado, cada recepción pide confirmación;
- para dispositivos nuevos/no confiables, el estado inicial es apagado.

No mostrar al usuario términos internos como carpetas Syncthing, relay, REST API o IDs técnicos salvo una pantalla de diagnóstico avanzada.


## Tarjetas utilizables — abrir al tocar

- Tocar el cuerpo de una tarjeta abre el contenido con el manejador compatible de Android.
- Los botones dentro de la tarjeta siguen siendo acciones independientes: Compartir, Copiar cuando aplique, Fijar y Eliminar.
- La UI debe comunicar de forma natural que la tarjeta es abrible sin añadir un botón grande adicional que vuelva a saturarla.
- Imágenes/video muestran miniaturas reales y PDF muestra su primera página cuando puede renderizarse; tocar sigue significando “abrir”.
- APK debe diferenciarse visualmente como instalable/paquete y nunca sugerir instalación automática.


## Superficies OclAx / Mi dispositivo

La parte superior incluye un selector compacto de dos orígenes:
- **OclAx**: bandeja temporal;
- **Mi dispositivo**: contenido original del teléfono.

Reglas visuales:
- no mezclar en una misma lista copias OclAx y originales;
- Mi dispositivo usa su propio selector compacto de categorías;
- Apps es la categoría inicial de Mi dispositivo para que el usuario pueda comprobar rápidamente la visibilidad instalada;
- archivos reales muestran nombre, tipo, tamaño y ruta relativa cuando exista;
- no mostrar Fijar ni retención sobre originales; **Eliminar original** es una acción separada, destructiva y confirmada;
- Compartir y Copiar aparecen solo donde corresponden;
- tocar el cuerpo abre el elemento.

Si falta el permiso amplio, mostrar una explicación corta y un único botón **Conceder acceso**, sin bloquear la superficie OclAx.

## Vistas por categoría y acciones de Mi dispositivo

- Cada categoría de **Mi dispositivo** puede alternar entre **Lista** y **Cuadrícula** de forma independiente.
- La elección se recuerda por categoría.
- Apps, Imágenes y Video parten de cuadrícula; documentos y demás tipos parten de lista.
- Las imágenes/video muestran miniaturas reales cuando estén disponibles.
- PDF muestra como miniatura la primera página renderizada cuando pueda leerse; si está corrupto, protegido o no puede renderizarse, usa el icono PDF como fallback.
- Apps instaladas muestran acción **Compartir** además de abrir.
- Los archivos originales muestran acción destructiva **Eliminar original** en rojo y con confirmación explícita.
- El texto de confirmación debe dejar claro que no se trata de la autolimpieza de OclAx.
- Dentro del selector de archivos de Android, OclAx ofrece miniaturas y una preferencia inicial; los controles de vista del selector pertenecen al sistema y el usuario conserva la decisión final.
