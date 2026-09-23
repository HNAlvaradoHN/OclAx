# DECISIONS

## DEC-001 — Nombre y significado

**Decisión:** usar **OclAx**.

**Significado:** Open Content, Local Access & eXchange.

**Concepto:** contenido local disponible para guardar temporalmente, organizar y mover rápidamente entre aplicaciones.

---

## DEC-002 — Repositorio público

**Decisión:** el proyecto se desarrolla públicamente en GitHub.

**Consecuencia:** ningún secreto, dato personal, documento privado, credencial o material sensible puede entrar al repositorio.

---

## DEC-003 — Local-first

**Decisión:** el producto prioriza funcionamiento local y rapidez, evitando depender de red para el flujo principal.

**Motivo:** el objetivo es una herramienta temporal rápida, no almacenamiento remoto permanente.

---

## DEC-004 — Almacenamiento temporal

**Decisión:** OclAx no se diseña como archivo permanente.

**Objetivo:** guardar → usar/insertar → limpiar.

La política exacta de retención será configurable o automática, pero evitar acumulación innecesaria es requisito del producto.

---

## DEC-005 — Separación de responsabilidades

**Decisión:** UI, personalización, lógica, datos y plataforma permanecen desacoplados en lo razonable.

**Motivo:** permitir cambios de diseño sin romper el resto del proyecto.

---

## DEC-006 — Handshake de sesión

**Decisión:** cada chat nuevo debe sincronizarse y obtener un número persistente antes de trabajar.

Formato:

`Ing. OclAx📲 #[NÚMERO]`

Sin handshake válido, la sesión no está autorizada para modificar el proyecto.

---

## DEC-007 — Revisores separados de AGENTS.md

**Decisión:** AGENTS.md define cuándo revisar y `docs/REVIEW_ROLES.md` contiene los checklists y responsabilidades detalladas.

**Motivo:** mantener la constitución legible y permitir evolucionar revisores sin inflar el protocolo principal.

---

## DEC-008 — DESIGN.md como fuente de verdad visual

**Decisión:** cuando exista UI, `docs/DESIGN.md` conserva las decisiones visuales y de accesibilidad.

**Motivo:** separar cambios de diseño de lógica, datos e infraestructura.

**Consecuencia:** herramientas visuales externas son auxiliares; las decisiones duraderas vuelven a GitHub.

---

## DEC-009 — Registro de herramientas externas

**Decisión:** `docs/TOOLS.md` registra herramientas externas relevantes, su estado, límites, privacidad y condiciones de uso.

**Motivo:** evitar agregar herramientas por moda o convertirlas accidentalmente en una fuente de verdad paralela.

---

## DEC-010 — Flujo principal de inserción

**Decisión:** OclAx se diseña como una bandeja temporal accesible desde el selector de archivos de Android.

**Flujo objetivo del usuario:**

`guardar rápido en OclAx → abrir otra app → + / Adjuntar → Archivos → OclAx → Recientes → insertar`

La raíz de OclAx debe priorizar **Recientes** y mostrar contenido mixto en un solo lugar: imágenes/capturas, texto materializado como archivo cuando haga falta, APK, PDF, documentos, video, código y otros archivos.

**Regla de producto:** insertar debe requerir el mínimo de pasos posible. OclAx no debe obligar al usuario a navegar carpetas internas ni organizar manualmente antes de usar un elemento.

**Limitación Android:** OclAx no puede capturar universalmente todo lo copiado en segundo plano. Por eso el ingreso al estante debe usar acciones explícitas y rápidas (Compartir a OclAx, importar, guardar selección/texto, o mecanismos equivalentes compatibles con Android). IME/Accessibility/Shizuku no forman parte del MVP salvo decisión posterior.

---

## DEC-011 — Compartir texto/imagen a OclAx publica también al portapapeles

**Decisión:** cuando OclAx reciba mediante Compartir un texto o una imagen compatible, debe:

1. importar el contenido a la bandeja temporal;
2. colocarlo inmediatamente en el portapapeles del sistema;
3. confirmar de forma breve que quedó listo.

### Texto
Publicar mediante ClipboardManager como texto plano.

### Imagen
Primero copiar los bytes a almacenamiento controlado por OclAx y después publicar al portapapeles una URI propia con permiso de lectura. No depender de la URI temporal del emisor.

### Otros archivos
PDF, APK, documentos, ZIP, video y demás se guardan en OclAx, pero no se promete pegado universal por portapapeles. Su salida principal es:

`+ / Adjuntar → Archivos → OclAx → Recientes → insertar`

**Objetivo:** ofrecer dos rutas rápidas sin duplicar la lógica de ingreso:
- texto/imagen → Compartir a OclAx → queda en Recientes + listo para Pegar;
- cualquier archivo → Compartir a OclAx → queda en Recientes + listo para seleccionar desde Archivos.

---

## DEC-012 — OclAx no será un gestor de archivos completo

**Decisión:** OclAx no intentará reemplazar al gestor de archivos del teléfono.

Su función principal es ser una **fuente ordenada para insertar contenido rápidamente desde otra aplicación**.

**Flujo prioritario:**

`chat/app → + / Adjuntar → Archivos → OclAx → buscar o filtrar → tocar → insertar`

Dentro de OclAx, el usuario debe poder encontrar contenido sin navegar un árbol de carpetas desordenado.

La vista principal del proveedor prioriza:
- Recientes;
- búsqueda;
- Fijados;
- categorías por tipo cuando ayuden: Imágenes, Documentos, APK, PDF, Video, Audio, Texto/Código y Otros.

**Regla UX:** ordenar para insertar, no administrar almacenamiento.

Por tanto, el MVP no necesita:
- mover archivos del teléfono entre carpetas;
- renombrado masivo;
- compresión/descompresión;
- análisis de almacenamiento;
- acceso remoto;
- papelera general del dispositivo;
- funciones propias de un gestor de archivos completo.

Los archivos que ya existen fuera de OclAx pueden incorporarse de forma explícita cuando se necesiten, sin convertir a OclAx en explorador total del almacenamiento.

---

## DEC-013 — Base técnica Android de la prueba vertical

**Decisión:** iniciar OclAx como aplicación Android nativa Kotlin con Jetpack Compose para la UI, almacenamiento privado propio, FileProvider y DocumentsProvider.

**Toolchain verificado al 2026-09-22:**
- Android Gradle Plugin 9.4.0;
- Gradle 9.6.0;
- JDK 17;
- compileSdk/targetSdk 36;
- Compose BOM 2026.09.00.

**Motivo:** usar APIs nativas y mínimas para demostrar el flujo central antes de añadir persistencia compleja, IME, accesibilidad, nube o permisos amplios.

**Consecuencia:** no se añade Room ni WorkManager hasta que exista una necesidad demostrada.

---

## DEC-014 — Retención y límite de propiedad

**Decisión:** la autolimpieza solo puede eliminar copias creadas y controladas por OclAx dentro de su almacenamiento privado. Nunca debe eliminar, mover ni modificar el archivo original del dispositivo o de otra aplicación.

**Retención predeterminada:** 24 horas para elementos no fijados.

**Opciones previstas:** 1 hora, 24 horas, 3 días, 7 días y nunca. Un valor persistido inválido vuelve de forma segura a 24 horas.

**Fijados:** un elemento fijado queda excluido de la autolimpieza hasta que el usuario lo desfije.

**Ejecución inicial:** limpieza oportunista al consultar la bandeja/DocumentProvider, sin WorkManager. Esto mantiene el MVP simple y evita trabajo en segundo plano innecesario; una ejecución periódica solo se añadirá si una necesidad real lo justifica.

**Consecuencia:** borrar una copia OclAx puede hacer que deje de estar disponible desde OclAx, pero no afecta su fuente original externa.


---

## DEC-015 — Acciones directas por elemento y riel de categorías

**Decisión:** la bandeja interna de OclAx permite actuar directamente sobre cada copia temporal.

Acciones:
- Compartir a otra aplicación mediante Android Sharesheet;
- Fijar / Desfijar;
- Eliminar manualmente con confirmación obligatoria.

**Borrado manual:** elimina únicamente la copia privada controlada por OclAx. Nunca elimina el archivo original del dispositivo ni usa la URI de origen para borrar.

**Organización:** los filtros Todo, Fijados, Imágenes, Documentos, PDF, Apps/APK, Texto/Código, Video, Audio y Otros se muestran en un riel vertical a la derecha.

**Motivo:** reducir búsqueda y desplazamiento horizontal, y convertir la bandeja en una herramienta bidireccional: recibir contenido y volver a compartirlo rápidamente.


---

## DEC-016 — Selector compacto a la izquierda y copia limitada

**Decisión:** la organización por categorías deja de usar un riel vertical permanente.

La UI de la app muestra un único control compacto de categoría, alineado a la izquierda. Por defecto muestra **Todo**. Al tocarlo abre un menú desplegable con Fijados, Imágenes, Documentos, PDF, Apps/APK, Texto/Código, Video, Audio y Otros.

El menú:
- se cierra al elegir una categoría;
- se cierra al tocar fuera;
- muestra como control principal la categoría actualmente seleccionada.

Esta decisión **sustituye únicamente la parte de organización visual del riel derecho de DEC-015**. Las acciones directas por elemento de DEC-015 siguen vigentes.

**Copiar:** la acción Copiar solo se ofrece para:
- Texto/Código: publica texto plano al portapapeles, con un límite defensivo de tamaño;
- Imágenes: publica una URI propia de OclAx mediante FileProvider al portapapeles.

PDF, APK, documentos, video, audio y otros archivos no muestran Copiar; mantienen Compartir como salida principal.

**Motivo:** ahorrar pantalla, devolver ancho a las tarjetas y evitar prometer una semántica de portapapeles que otras aplicaciones no manejan de forma consistente para archivos arbitrarios.


---

## DEC-017 — Aplicaciones instaladas visibles sin permiso amplio

**Decisión:** OclAx incorpora una categoría separada **Aplicaciones** para mostrar aplicaciones instaladas que Android expone como lanzables mediante `MAIN + LAUNCHER`.

Reglas:
- **Aplicaciones** y **APK** son categorías distintas;
- APK representa archivos APK guardados en la bandeja OclAx;
- Aplicaciones representa apps instaladas visibles para el launcher;
- se usa el icono real y nombre visible de cada app;
- la lista se ordena alfabéticamente;
- la búsqueda filtra por nombre o paquete;
- esta primera versión de la vista es informativa/visual: no redefine todavía abrir una app ni usarla como destino de transferencia.

Privacidad y plataforma:
- no se solicita `QUERY_ALL_PACKAGES`;
- solo se declara una consulta de visibilidad para apps con actividad de launcher;
- la lista permanece local y no se envía a ningún servicio;
- OclAx sigue sin permiso de Internet.

**Motivo:** completar la parte del flujo visual solicitada por el dueño —aplicaciones del dispositivo ordenadas e identificables— sin ampliar permisos ni mezclar apps instaladas con archivos APK.
