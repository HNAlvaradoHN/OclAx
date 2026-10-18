# DESIGN — Memoria visual de OclAx

## Identidad

- **Producto:** OclAx.
- **Significado:** Open Content, Local Access & eXchange.
- **Idea:** contenido local temporal, organizado y listo para usar entre aplicaciones.
- **Frase:** “Tu contenido, listo donde lo necesitás.”

La función LAN fue retirada. La interfaz no debe reservar espacio ni lenguaje para motores, peers o conexiones.

## Paleta

Identidad aprobada:
- modo oscuro: negro/casi negro + naranja;
- modo claro: superficie clara + naranja;
- seguir tema del sistema;
- color nunca sustituye icono/texto.

Paleta semántica vigente por categoría:
- Todo/Recientes: naranja;
- Fijados: dorado;
- Imágenes: verde;
- Documentos: azul;
- PDF: rojo;
- APK/Apps: verde lima;
- Texto/Código: morado;
- Video: magenta;
- Audio: cian;
- Otros: fallback del tema.

## Estructura principal

```text
OclAx
Tu contenido, listo donde lo necesitás.

[ OclAx ] [ Mi dispositivo ]

OclAx:
[ Buscar ]
Autolimpieza [ 1 h / 24 h / ... ]
[ Todo ] [ Fijados ]
[ Imágenes ] [ Documentos ]
...

Mi dispositivo:
[ Buscar ]
[ Aplicaciones ] [ Imágenes ]
[ Documentos ] [ PDF ]
...
```

No existe el bloque **Enviar a dispositivo · prueba técnica**.

## Tarjetas OclAx

Cada elemento puede mostrar:
- miniatura/icono;
- nombre;
- tipo + tamaño;
- fecha/hora de ingreso;
- estado de captura reciente cuando corresponda.

Acciones compactas:
- Renombrar;
- Compartir;
- Copiar solo para texto/imagen;
- Fijar/Desfijar;
- Eliminar copia.

### Estado de captura reciente

Si `deleteSourceOnExpiry=true`, mostrar texto visible, no solo color:

- no fijada: **Captura reciente · original se limpiará al vencer**;
- fijada: **Captura reciente · original protegido mientras esté fijada**.

Motivo: borrar un original es una consecuencia importante y debe ser visible antes del vencimiento.

No mostrar ese mensaje para imágenes normales o capturas antiguas.

## Renombrar

### OclAx
Diálogo:
- título **Renombrar**;
- campo prellenado con nombre completo;
- explicar si OclAx conoce un original enlazado;
- botón Renombrar / Cancelar.

Si el origen puede modificarse, el mismo nombre se aplica al original.

### Mi dispositivo
Diálogo:
- título **Renombrar archivo original**;
- texto claro: “El cambio se aplicará al archivo real del dispositivo”;
- nombre completo prellenado.

La extensión forma parte del nombre visible para no cambiarla silenciosamente.

## Eliminar

### OclAx
Eliminar manualmente una copia continúa significando **Eliminar de OclAx**. No se usa esta acción como atajo para borrar el original.

La eliminación automática del original solo existe por la regla especial de screenshot reciente.

### Mi dispositivo
Eliminar indica explícitamente **Eliminar original** y mantiene confirmación.

## Selectores externos

### Elegir con OclAx
Es la experiencia visual propia:
- cabecera OclAx;
- OclAx / Mi dispositivo;
- búsqueda;
- categorías;
- lista/cuadrícula;
- miniaturas;
- Cancelar;
- selección compatible con MIME;
- en **Mi dispositivo**, categoría **Aplicaciones** cuando el caller acepta APK;
- **Aplicaciones** y **APK** son categorías distintas: una representa software instalado y la otra archivos APK existentes.

Debe sentirse coherente con la aplicación principal.

### Archivos → OclAx
La UI la dibuja Android.

OclAx debe aportar:
- archivos reales además de carpetas;
- categorías;
- **Aplicaciones** dentro de **Mi dispositivo**;
- entrada **Mi dispositivo**;
- nombres/fechas/tamaños;
- miniaturas;
- búsqueda/recientes.

En **Aplicaciones**, una instalación de APK único aparece como archivo APK seleccionable. Si la instalación usa splits, aparece como carpeta con `base.apk` y sus splits para no presentar un paquete incompleto como si fuera un único APK.

No diseñar componentes Compose esperando que aparezcan dentro de la app Files: DocumentsProvider no controla esa presentación.

## Mi dispositivo

Contenido:
- Aplicaciones;
- Imágenes;
- Documentos;
- PDF;
- APK;
- Texto/Código;
- Video;
- Audio;
- Otros.

Archivos:
- miniatura/icono;
- nombre;
- tipo/tamaño;
- fecha de modificación;
- ruta relativa cuando ayuda;
- lista/cuadrícula recordada por categoría.

Acciones:
- Abrir tocando tarjeta;
- Compartir;
- Copiar cuando aplica;
- Renombrar;
- Eliminar original.

Apps:
- icono real;
- nombre;
- indicador sistema/instalada;
- abrir;
- compartir APK.

## Navegación

- Categoría/búsqueda → Atrás vuelve a Categorías;
- Mi dispositivo → Atrás vuelve a OclAx;
- raíz OclAx → confirmación de salida;
- picker propio desenrolla categoría/origen antes de volver a app llamadora.

## Accesibilidad

- targets táctiles suficientes;
- `contentDescription` en acciones de icono;
- estados destructivos también en texto;
- contraste suficiente;
- no depender solo del color;
- nombres truncados visualmente sin cambiar el dato real;
- diálogos con cancelar.

## Pendientes visuales

No bloqueantes:
- logo gráfico oficial cuando exista asset aprobado;
- espaciado fino;
- densidad en pantallas muy pequeñas;
- revisión modo claro;
- animaciones solo si aportan claridad.

No introducir una nueva estética durante la limpieza técnica.

## Selección múltiple OclAx

La selección múltiple es explícita:
- botón **Seleccionar** en una lista/resultados OclAx;
- checkbox visible en cada tarjeta;
- borde primario reforzado para seleccionados;
- contador de seleccionados;
- **Todos/Ninguno**;
- acciones masivas: Compartir, Fijar/Desfijar y Eliminar copias;
- **Cancelar** sale del modo selección;
- Atrás cancela selección antes de navegar;
- la búsqueda queda deshabilitada mientras la selección está activa.

Eliminar en lote debe decir claramente que solo afecta copias OclAx y nunca originales.

No llenar la barra con acciones que no tienen contrato definido. Renombrado/mover masivo quedan fuera hasta tener reglas de producto explícitas.

## Iconos de aplicaciones en Android Files

OclAx ofrece thumbnail con el icono real de la app cuando Android Files lo solicita.

Reglas visuales:
- no sacrificar estabilidad del listado por iconos;
- si el icono no puede cargarse, Files debe poder usar su representación genérica;
- apps split siguen siendo carpetas porque representan varios APK;
- Android Files decide finalmente si muestra thumbnail en carpetas; OclAx no controla esa UI.

## Selección múltiple en Mi dispositivo

Dentro de una categoría de **Mi dispositivo**:
- botón **Seleccionar** junto al título;
- checkbox y borde reforzado en tarjetas;
- contador;
- **Todos/Ninguno**;
- **Compartir** como acción masiva;
- **Cancelar** y Atrás salen primero del modo selección;
- búsqueda deshabilitada mientras la selección está activa.

Límites visuales/funcionales:
- no mostrar Eliminar/Renombrar/Mover como acciones masivas de originales;
- archivos reales admiten hasta 100 seleccionados;
- aplicaciones instaladas admiten hasta 20;
- la app debe seguir dejando claro que compartir aplicaciones significa APK de instalación, no datos privados.
