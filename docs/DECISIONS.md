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


---

## DEC-018 — Apps instaladas y categorías del DocumentsProvider son vistas distintas

**Decisión:** OclAx diferencia explícitamente dos superficies:

1. **App OclAx → Apps instaladas**
   - muestra aplicaciones lanzables del perfil actual;
   - usa LauncherApps;
   - muestra nombre e icono real;
   - no representa esas aplicaciones como archivos adjuntables.

2. **Archivos del sistema → OclAx**
   - muestra exclusivamente copias/archivos controlados por OclAx;
   - mantiene acceso directo a recientes;
   - añade carpetas virtuales por tipo: Fijados, Imágenes, Documentos, PDF, APK, Texto/Código, Video, Audio y Otros.

**Motivo:** una aplicación instalada no es un documento SAF. Exponerla como si fuera un archivo produciría un flujo engañoso. La organización del selector debe mejorar sin romper la semántica de DocumentsProvider.


---

## DEC-019 — Navegación amplia del dispositivo y distribución fuera de Play Store

**Decisión:** OclAx evoluciona de una bandeja temporal pura a una bandeja temporal + navegador local del contenido real del dispositivo.

Objetivo de la vista **Mi dispositivo**:
- aplicaciones instaladas;
- imágenes;
- videos;
- audio;
- PDF;
- documentos;
- APK;
- otros archivos accesibles del almacenamiento compartido.

Permisos:
- OclAx puede solicitar acceso amplio al almacenamiento cuando sea necesario para cumplir este objetivo;
- si Android requiere `MANAGE_EXTERNAL_STORAGE`, se solicita como acceso especial y el usuario puede negarlo o revocarlo;
- si se necesita visibilidad completa de aplicaciones, puede declararse `QUERY_ALL_PACKAGES`;
- no se solicitarán permisos sin relación con una función concreta: “acceso a todo” significa acceso amplio al contenido/aplicaciones que OclAx necesita gestionar, no todos los permisos de Android indiscriminadamente;
- si un usuario niega o revoca permisos, OclAx debe degradar la funcionalidad y mostrar únicamente lo que Android permita.

Distribución:
- no se planea publicar OclAx en Google Play;
- canales previstos: GitHub Releases y, opcionalmente, otras tiendas/distribuidores;
- aun fuera de Play Store, se mantienen mínimos de seguridad, privacidad y consentimiento explícito.

Límite de propiedad:
- la autolimpieza y el botón Eliminar de las tarjetas internas siguen operando únicamente sobre copias privadas de OclAx;
- el contenido real de **Mi dispositivo** no se borra automáticamente;
- cualquier futura eliminación de un original externo debe ser una acción distinta, explícita y confirmada.

**Motivo:** el dueño quiere ver y reutilizar el contenido real del teléfono desde OclAx, y acepta otorgar permisos amplios para conseguirlo.

---

## DEC-020 — Transferencia OclAx ↔ OclAx con experiencia “Enviar a dispositivo”

**Decisión:** Syncthing se adopta como candidato principal de motor técnico para transferencias entre dispositivos OclAx, envuelto por una UX propia de OclAx. La experiencia visible no será “sincronizar carpetas”, sino **seleccionar contenido → elegir dispositivo → enviar**.

Estado:
- candidato aprobado para prototipo;
- investigación técnica actualizada el 2026-09-23: el wrapper oficial `syncthing/syncthing-android` fue discontinuado/archivado y no se adopta como dependencia;
- existe un fork comunitario mantenido (`researchxxl/syncthing-android`) que demuestra un patrón Android vigente con SyncthingNative, pero OclAx lo tratará como referencia técnica y no como autoridad ni dependencia automática;
- el spike ya empaqueta un **Syncthing core estable fijado por versión** detrás de una capa propia de OclAx;
- la versión fijada es Syncthing **v2.1.5**; cualquier actualización deberá revisarse y fijarse explícitamente;
- el runtime se ejecuta on-demand mediante un foreground service `dataSync`; no arranca al boot ni funciona como daemon permanente;
- la GUI/API del motor se fuerza a `127.0.0.1:8384` con API key privada por instalación;
- durante el probe de un solo dispositivo, el runtime arranca pausado, el protocolo de sincronización escucha solo en loopback y discovery global/local, relay y NAT quedan apagados; la red de pares se habilitará recién como parte explícita del emparejamiento/transferencia;
- compilación multi-ABI, configuración previa y controles REST están implementados, pendientes de CI final y validación física Android antes de adoptar el motor como integración final;
- no se autoriza servicio de pago ni infraestructura con costo sin aprobación del dueño.

Modelo de confianza:
- cada dispositivo OclAx se empareja explícitamente;
- dispositivos emparejados pueden marcarse como **Mis dispositivos / confiables**;
- por dispositivo existe la opción **Permitir sin aceptar**;
- si está activa, las transferencias entrantes de ese dispositivo se reciben automáticamente;
- si está desactivada, el receptor debe aceptar o rechazar cada transferencia;
- para otros dispositivos emparejados el valor predeterminado es **preguntar antes de recibir**;
- un usuario puede habilitar recepción automática también para un dispositivo externo específico si lo decide;
- un dispositivo desconocido/no emparejado nunca puede enviar automáticamente.

Flujo:
1. seleccionar uno o varios elementos;
2. tocar Enviar;
3. elegir un dispositivo OclAx emparejado;
4. la transferencia comienza;
5. el receptor aplica su política: automática o con confirmación;
6. el contenido recibido aterriza primero en una bandeja privada de OclAx.

Seguridad:
- el proceso/API local del motor debe quedar limitado a loopback; no se expondrá la interfaz de control a la LAN/Internet;
- cualquier API key local se genera por instalación, vive solo en almacenamiento privado y nunca se publica ni se registra;
- el binario/runtime de Syncthing se fija por versión y se construye/obtiene mediante un flujo reproducible separado de secretos de firma;
- se desactivan auto-upgrade y telemetría/usage reporting del motor dentro de OclAx;
- una transferencia recibida nunca ejecuta APK ni abre archivos automáticamente;
- no escribe arbitrariamente sobre archivos originales del dispositivo;
- recepción automática significa “aceptar en la bandeja OclAx”, no ejecutar ni instalar;
- el motor de transferencia y sus APIs locales deben quedar aislados detrás de una capa de plataforma propia;
- credenciales/API keys locales del motor nunca se publican ni se guardan en el repo.

**Motivo:** ofrecer una experiencia tipo envío directo entre dispositivos, manteniendo al usuario en control del nivel de confianza por dispositivo.


---

## DEC-021 — Todo contenido utilizable se puede abrir desde OclAx

**Decisión:** una tarjeta de contenido OclAx no es un archivo muerto. Al tocar la tarjeta, OclAx intenta abrir el elemento mediante Android usando su MIME real y una URI de solo lectura controlada por FileProvider.

Comportamiento:
- PDF → visor PDF predeterminado o selector de aplicaciones si Android necesita preguntar;
- Word/documentos → aplicación compatible instalada;
- imágenes → galería/visor compatible;
- video/audio → reproductor compatible;
- texto/código → aplicación compatible si existe;
- APK → instalador del sistema u otro manejador compatible; la instalación siempre es iniciada por el usuario y Android conserva sus controles de “instalar apps desconocidas”;
- tipos sin manejador → OclAx informa que no hay una aplicación disponible.

Reglas:
- tocar una tarjeta abre; las acciones Compartir, Copiar, Fijar y Eliminar mantienen su comportamiento propio;
- OclAx no ejecuta contenido arbitrario internamente;
- no se otorga escritura al visor externo;
- futuras tarjetas de **Mi dispositivo** deben reutilizar la misma semántica de abrir con Android sin convertir el original en copia salvo que el usuario lo decida.

**Motivo:** el contenido recibido debe poder usarse inmediatamente desde la bandeja y aprovechar las asociaciones/defaults del sistema.

---

## DEC-022 — Jerarquía de transporte OclAx ↔ OclAx

**Decisión:** el prototipo Syncthing debe priorizar conexiones directas y usar relay únicamente como fallback.

Orden conceptual:
1. conexión directa en la misma red/LAN cuando esté disponible;
2. conexión directa entre pares a través de Internet cuando Syncthing pueda establecerla;
3. relay público de Syncthing cuando no sea posible una conexión directa.

Aclaración:
- el relay no es almacenamiento en nube ni “sube el archivo para después”; reenvía tráfico entre los dos dispositivos;
- la sesión entre dispositivos permanece cifrada extremo a extremo;
- el relay puede conocer metadatos de conexión como IP/device ID y volumen de tráfico;
- los relays públicos disponibles actualmente no requieren un servicio pago de OclAx, pero no constituyen un SLA ni una garantía de gratuidad eterna;
- OclAx no contratará ni desplegará infraestructura con costo sin autorización explícita.

**Motivo:** conseguir una experiencia tipo “enviar y listo” usando primero la ruta más directa y rápida disponible, sin costo obligatorio de servidor.


---

## DEC-023 — Mi dispositivo es una superficie separada y de solo uso explícito

**Decisión:** la aplicación principal tiene dos superficies visibles:

1. **OclAx**
   - copias temporales controladas por ItemStore;
   - Fijados, retención, Compartir, Copiar y Eliminar;
   - autolimpieza exclusiva de copias OclAx.

2. **Mi dispositivo**
   - inventario del contenido real accesible del teléfono;
   - aplicaciones instaladas;
   - imágenes, documentos, PDF, APK, texto/código, video, audio y otros archivos;
   - abrir, compartir y copiar texto/imagen sin crear una copia OclAx automáticamente.

Permisos:
- `QUERY_ALL_PACKAGES` para inventario completo de aplicaciones;
- `MANAGE_EXTERNAL_STORAGE` en Android 11+ para acceso amplio a almacenamiento compartido;
- `READ_EXTERNAL_STORAGE` solo como compatibilidad en Android antiguos;
- el usuario entra a la pantalla especial de Android para conceder/revocar acceso amplio.

Reglas:
- negar acceso a archivos no rompe OclAx ni el listado de aplicaciones;
- Mi dispositivo no expone botón Eliminar en esta fase;
- tocar una aplicación intenta abrirla si tiene actividad lanzable;
- tocar un archivo intenta abrirlo con Android;
- Compartir y Copiar reutilizan URI de contenido existentes, sin duplicar automáticamente el original.

**Motivo:** permitir acceso real al contenido del dispositivo sin mezclar propiedad, retención ni borrado con la bandeja temporal.

## DEC-024 — Miniaturas, exportación de apps y borrado explícito

**Decisión:** la superficie **Mi dispositivo** evoluciona para reconocer contenido visual, compartir aplicaciones instaladas y eliminar originales únicamente mediante una acción explícita.

### Miniaturas y vista
- Imágenes y video usan miniaturas reales cuando Android puede generarlas.
- PDF usa una miniatura renderizada desde su primera página cuando puede abrirse de forma segura.
- El DocumentsProvider marca imágenes/video/PDF como compatibles con miniatura y responde a las solicitudes del selector del sistema.
- Imágenes y Video prefieren cuadrícula como presentación inicial; el selector del sistema conserva la decisión final del usuario.
- Dentro de Mi dispositivo, lista/cuadrícula se elige y recuerda **por categoría**.

### Compartir una aplicación instalada
- OclAx comparte únicamente los APK que forman la instalación: base + splits cuando existan.
- Se leen exclusivamente `ApplicationInfo.sourceDir` y `splitSourceDirs`.
- Nunca se exporta `dataDir`, preferencias, bases de datos, caché, sesiones, cuentas ni archivos personales de esa app.
- Los APK se copian temporalmente a caché privada OclAx y se exponen en solo lectura mediante FileProvider.
- Para instalaciones con splits se comparte el conjunto completo; la instalación posterior sigue siendo decisión del receptor y puede requerir un instalador compatible.

### Eliminar originales
- **Eliminar del dispositivo** es distinto de **Eliminar de OclAx**.
- OclAx muestra confirmación explícita indicando que se elimina el original.
- En Android 11+ y con acceso amplio ya concedido, OclAx intenta eliminar mediante ContentResolver; si Android exige confirmación para un medio, usa `MediaStore.createDeleteRequest` con la URI específica de Imagen/Video/Audio.
- La autolimpieza de OclAx jamás incluye originales del dispositivo.
