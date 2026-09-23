# DESIGN — Memoria visual de OclAx

## Estado

Fuente de verdad visual inicial. La UI todavía no está implementada.

## Principio

El diseño debe poder evolucionar sin obligar a reescribir lógica, datos o infraestructura.

## Identidad

- Producto: OclAx.
- Significado: Open Content, Local Access & eXchange.
- Concepto: contenido local listo para guardar temporalmente, organizar y mover entre aplicaciones.
- Idea de marca: “Tu contenido, listo donde lo necesitás.”

## Reglas iniciales

Todavía no se han aprobado:
- paleta definitiva;
- tipografía definitiva;
- iconografía;
- componentes;
- espaciado;
- formas;
- animaciones.

No inventar decisiones visuales permanentes y presentarlas como aprobadas.

## Accesibilidad

Cuando exista UI:
- contraste suficiente;
- tamaños táctiles adecuados;
- estados claros;
- soporte de lector de pantalla cuando aplique;
- foco/navegación coherentes;
- textos comprensibles.

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

Evitar:
- árbol de carpetas como pantalla inicial;
- categorías redundantes;
- controles administrativos que distraigan del acto de insertar;
- mezclar sin jerarquía capturas, APK, PDF, documentos y otros archivos.

La interfaz debe favorecer reconocer el contenido visualmente y reducir el número de toques.
