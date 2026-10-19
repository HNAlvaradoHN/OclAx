# PROJECT_STATE

## Estado

**Fase:** cierre técnico y preparación de candidata de release Android local.  
**Aplicación:** funcional en main; vertical local validada físicamente en sus flujos principales. Hardening/release en progreso.  
**Repositorio:** público.  
**Protocol version:** 6.  
**Autorización actual:** completar revisión final de seguridad/calidad/release y preparar candidata técnica sin firmar ni publicar.

## Dirección vigente

OclAx es una herramienta **local-first** para:
- recibir contenido desde Compartir;
- conservar copias temporales y Fijados;
- buscar/organizar por categorías;
- abrir, copiar y volver a compartir;
- explorar archivos reales y aplicaciones desde **Mi dispositivo**;
- elegir contenido desde otras apps mediante el picker propio o el DocumentsProvider;
- limpiar automáticamente contenido temporal;
- evitar acumulación de capturas de pantalla recién creadas.

**Retirado por decisión del dueño:** envío OclAx↔OclAx, emparejamiento, LAN, Syncthing y toda la infraestructura asociada. DEC-032 manda sobre las decisiones LAN históricas.

## Qué funciona y se conserva

- recepción mediante Android Share Sheet;
- almacenamiento privado bajo OclAx;
- texto/imagen al portapapeles;
- Fijados y retención configurable;
- apertura/compartir de contenido;
- **Mi dispositivo** con aplicaciones y archivos reales mediante acceso concedido por el usuario;
- miniaturas de imagen/video/PDF;
- lista/cuadrícula por categoría;
- compartir APK de aplicaciones instaladas sin datos privados;
- eliminación explícita de originales;
- picker propio `ACTION_GET_CONTENT`;
- DocumentsProvider de solo lectura;
- navegación Atrás jerárquica;
- firma estable del APK de pruebas;
- CI Android con tests, lint y assembleDebug.

## Implementado en la renovación actual

### Retiro real de LAN
- eliminado panel **Enviar a dispositivo** y estados relacionados;
- eliminado emparejamiento y transferencias;
- eliminado runtime/REST/configuración Syncthing;
- eliminados foreground service y permisos de red/multicast/data-sync;
- eliminados binarios nativos, tests del motor y pipeline de compilación/caché nativa;
- eliminado `network_security_config` que existía para el REST loopback;
- CI vuelve a ser Android puro.

### Capturas recientes
- una imagen solo queda elegible para borrar también el original si:
  - proviene de MediaStore;
  - nombre/ruta indican captura de pantalla;
  - existe `DATE_ADDED`;
  - fue compartida dentro de los primeros **60 segundos**;
- capturas más antiguas, imágenes normales o fuentes no verificables conservan el original;
- antes del borrado se revalida la misma URI con tamaño y fecha de alta;
- Fijados nunca expiran;
- la UI muestra cuando una copia está vinculada a la limpieza del original;
- se programa la próxima expiración con AlarmManager sin solicitar alarma exacta, y se mantiene limpieza al volver a abrir/consultar OclAx.

### Renombrado
- elementos OclAx tienen acción **Renombrar**;
- si conservan un original MediaStore enlazado, el mismo nombre se aplica al original;
- **Mi dispositivo** puede renombrar el archivo original explícitamente;
- si Android/el proveedor no permite cambiar el origen, OclAx no reporta falsamente que lo hizo.

### Selector del sistema
- el DocumentsProvider expone copias OclAx **y archivos reales del dispositivo** cuando el acceso amplio está concedido;
- la raíz muestra archivos reales además de categorías y una entrada **Mi dispositivo**;
- imágenes/video/PDF conservan miniaturas;
- el aspecto exacto de **Archivos** sigue perteneciendo a Android;
- para la experiencia visual propia de OclAx se conserva `OclAxPickerActivity` con **OclAx / Mi dispositivo**.

## Seguridad/privacidad vigente

- OclAx no declara permiso `INTERNET`;
- no queda servicio de red en segundo plano;
- originales normales nunca forman parte de la autolimpieza;
- la única eliminación automática externa es la excepción de captura reciente de DEC-033;
- source URI/metadatos se guardan solo en metadata privada de la copia OclAx y no se registran;
- el DocumentsProvider permanece de solo lectura hacia apps externas;
- `MANAGE_EXTERNAL_STORAGE` y `QUERY_ALL_PACKAGES` siguen siendo decisiones explícitas de **Mi dispositivo**.

## Gobernanza

- AGENTS.md ya contiene las reglas solicitadas por el dueño:
  - actualización obligatoria de memoria después de cambios significativos;
  - reemplazo real: eliminar código/recursos/dependencias sustituidos, no esconderlos;
  - limpieza antes de cierre y handoff completo.
- Por tanto **no se añade una regla duplicada ni se incrementa protocol_version**.
- Registro administrativo verificado en este chat: `last_confirmed_chat: 5`.

## Evidencia física nueva — 2026-10-17

VERIFICADO con run 314:
- la UI principal ya no muestra LAN, Motor ni Mis dispositivos;
- una screenshot recién compartida quedó marcada para limpiar también el original al vencer;
- una screenshot reciente fijada quedó marcada como protegida mientras siga fijada;
- el resto del contenido real del dispositivo aparece en el flujo de selección;
- HALLAZGO: las aplicaciones instaladas no aparecían al elegir contenido desde otra app.

Causa verificada:
- OclAxPickerActivity cargaba ItemStore y DeviceContentRepository, pero no InstalledAppsRepository;
- PickerCategory no tenía categoría Aplicaciones.

Corrección implementada en fix/picker-installed-apps:
- categoría Aplicaciones separada de APK;
- listado de apps instaladas en Mi dispositivo;
- filtro por MIME del caller;
- exportación mediante InstalledAppExporter;
- base + splits solo cuando el caller permite múltiples archivos.

## Evidencia física adicional — run 317

VERIFICADO por captura del dispositivo:
- la pantalla mostrada es Android Files con breadcrumb **OclAx → Mi dispositivo**;
- aparecen categorías/archivos reales del dispositivo;
- **Aplicaciones** no aparece.

Corrección de diagnóstico:
- PR #84 corrigió `OclAxPickerActivity` (`ACTION_GET_CONTENT`);
- la captura corresponde a `OclAxDocumentsProvider` (`ACTION_OPEN_DOCUMENT`/SAF);
- por tanto PR #84 no podía modificar esa pantalla concreta.

Causa raíz:
- `OclAxDocumentsProvider` solo exponía categorías de archivos MediaStore y archivos reales;
- no consultaba `InstalledAppsRepository` ni creaba documentos virtuales para paquetes instalados.

Implementado en `fix/documents-provider-installed-apps`:
- categoría **Aplicaciones** en Mi dispositivo;
- APK único como documento virtual seleccionable con icono;
- apps con splits como carpeta base + splits;
- búsqueda por nombre/package;
- acceso de solo lectura a código APK instalado;
- IDs virtuales validados para no aceptar rutas arbitrarias.

## Evidencia física adicional — run 319

VERIFICADO:
- la categoría **Aplicaciones** ya aparece dentro de Android Files;
- al entrar, Android muestra **No se puede cargar el contenido en este momento**.

Diagnóstico de código:
- el provider usaba la ruta pesada de UI y cargaba iconos para cada paquete;
- además resolvía base/splits con una segunda consulta por app;
- los APK virtuales anunciaban thumbnails y podían disparar trabajo repetido.

Estado de causa: **HIPÓTESIS TÉCNICA**, sin logcat físico.

Corrección implementada en `fix/files-applications-load`:
- metadata ligera sin Drawable;
- listado SAF obtiene source APK paths en la misma enumeración;
- lookup puntual por package;
- sin thumbnails para APK virtuales;
- pantalla principal y picker propio conservan sus iconos.

## Evidencia física adicional — run 321

VERIFICADO por capturas del dispositivo:
- **Aplicaciones** carga correctamente dentro de Android Files;
- la lista de aplicaciones instaladas es visible;
- una aplicación con splits abre una carpeta virtual;
- dentro aparecen `base.apk` y un split;
- ERR-021 no se reproduce.

Esto cierra PICKER-004. PICKER-003 todavía necesita validar selección/retorno de APK a una app receptora.

## Trabajo actual — PRODUCT-005

IMPLEMENTADO_PENDIENTE_VALIDACIÓN en `feat/bulk-actions-app-icons`:
- modo de selección múltiple para elementos OclAx;
- seleccionar todos los elementos visibles;
- compartir varios elementos;
- fijar/desfijar varios;
- borrar varias copias OclAx con confirmación y sin tocar originales;
- política unitaria para MIME compartido y objetivo de Fijados;
- iconos de aplicaciones solicitados de forma puntual por package;
- cache de thumbnails antes de repetir lookup;
- soporte de thumbnail para documentos APK virtuales y carpetas virtuales de apps split.

Límite de plataforma:
- Android Files decide si usa y cómo presenta thumbnails en carpetas. OclAx puede ofrecer el icono, no controlar su renderizado exacto.

## Evidencia física adicional — run 334

VERIFICADO por el dueño:
- selección múltiple OclAx funciona;
- compartir selección funciona;
- eliminar varias copias OclAx funciona;
- Android Files sigue cargando **Aplicaciones** sin ERR-021;
- dentro de una app split, los APK muestran el icono real de la aplicación;
- en el nivel superior, Android Files sigue dibujando las apps split como carpetas genéricas.

Conclusión visual:
- OclAx entrega thumbnail real por package;
- Android Files usa ese thumbnail en documentos APK;
- el sistema no usa el thumbnail ofrecido para representar las carpetas virtuales de apps split en este dispositivo.

Feedback bloqueante:
- el modo **Seleccionar** estaba solo en la superficie OclAx y faltaba en **Mi dispositivo**.

Corrección en `fix/device-bulk-selection`:
- selección múltiple por categoría en **Mi dispositivo**;
- archivos reales: hasta 100 seleccionados, compartir por ACTION_SEND_MULTIPLE;
- aplicaciones instaladas: hasta 20 apps, exportación segura de base + splits y compartir en conjunto;
- Atrás cancela selección primero;
- búsqueda se bloquea durante selección;
- no se añade borrado/renombrado masivo de originales.

## Evidencia física adicional — run 336

VERIFICADO por el dueño:
- **Mi dispositivo → Apps** muestra el botón **Seleccionar**;
- la selección múltiple dentro de la aplicación funciona;
- compartir varios archivos reales desde **Mi dispositivo** funciona;
- compartir varias aplicaciones instaladas desde **Mi dispositivo** funciona;
- Atrás cancela primero el modo selección;
- Fijar/Desfijar varios elementos OclAx funciona;
- Android Files sigue mostrando icono genérico para carpetas virtuales de apps split;
- al entrar a una app split, los APK internos muestran el icono real.

Decisión de cierre visual:
- se acepta el icono genérico de carpeta en Android Files como limitación de presentación del sistema;
- OclAx no cambia el formato de apps split solo para forzar una miniatura.

## Evidencia física adicional — DATA-002 en run 336

VERIFICADO por el dueño:
- una captura antigua ya existente en galería se comparte a OclAx sin quedar marcada para borrar el original;
- una captura creada y compartida inmediatamente queda marcada como **Captura reciente · original se limpiará al vencer**;
- el flujo OEM de compartir screenshot entrega metadata suficiente para que OclAx aplique la regla MediaStore + DATE_ADDED + ventana de 60 s;
- la captura antigua permanece en la galería.

Esto valida físicamente la **clasificación** de captura reciente vs. captura antigua en este dispositivo.

VERIFICADO posteriormente por el dueño:
- las pruebas de vencimiento solicitadas se comportan correctamente en el dispositivo;
- la captura reciente cumple su limpieza al vencer;
- la captura antigua conserva su original;
- la protección de Fijados durante el vencimiento funciona.

Con esto DATA-002 queda físicamente cerrada para el alcance aprobado.

## Evidencia física adicional — FILES-001 y pickers

VERIFICADO por el dueño:
- renombrar desde OclAx funciona y el original enlazado refleja el cambio cuando corresponde;
- renombrar desde **Mi dispositivo** funciona;
- abrir/compartir después del renombrado sigue funcionando;
- **Elegir con OclAx → Mi dispositivo → Aplicaciones** funciona desde una app externa;
- una app de APK único puede seleccionarse y volver al caller;
- una app con splits se comporta correctamente según selección simple/múltiple;
- Android Files/SAF devuelve correctamente el contenido seleccionado a la app receptora.

Con esto se cierran físicamente **FILES-001**, **PICKER-002** y **PICKER-003** para el alcance aprobado.

## Auditoría final — LOCAL-001

VERIFICADO sobre el árbol activo de `main` y el APK run 336:
- no existen rutas/archivos activos con nombres de Syncthing, transferencia, peer/pairing, JNI propio ni `network_security_config`;
- búsquedas de código no encuentran sockets, ServerSocket, DatagramSocket, HttpURLConnection, OkHttp, Retrofit, Ktor, `java.net`, Wi-Fi/P2P/NSD ni callbacks de red;
- el Manifest fuente no declara `INTERNET`, `ACCESS_NETWORK_STATE`, `CHANGE_WIFI_MULTICAST_STATE` ni permisos de foreground service;
- el Manifest fuente no contiene ningún `<service>`;
- el Manifest generado del APK no contiene cadenas de permisos de red ni clases de servicio OclAx/Syncthing;
- el APK no contiene binarios Syncthing ni otros runtimes de transferencia;
- los únicos `.so` del APK son `libandroidx.graphics.path.so` de AndroidX para cuatro ABI;
- Gradle no configura JNI ni depende de Syncthing;
- GitHub Actions solo ejecuta tests/lint/assembleDebug y no descarga/compila runtimes nativos;
- no existe configuración de seguridad de red activa.

Las referencias textuales a LAN/Syncthing que permanecen están limitadas a documentación histórica/decisiones y describen explícitamente que la función fue retirada.

Resultado: **LOCAL-001 queda cerrado** para el alcance aprobado.

## Revisión final de release — rama release/final-review

VERIFICADO:
- permisos, componentes exportados, providers, retención, rename y exportación de APK revisados;
- sin secretos embebidos detectados por búsquedas del árbol activo;
- dependencias runtime AndroidX revisadas sin vulnerabilidades directas conocidas en las versiones usadas;
- licencias directas compatibles con el uso actual: AndroidX/AGP/Kotlin Apache-2.0; JUnit EPL-1.0 solo tests;
- hallazgo BLOQUEANTE: Kotlin tooling 2.4.10 afectado por CVE-2026-53914;
- corrección IMPLEMENTADA: actualización a Kotlin 2.4.21;
- hardening IMPLEMENTADO: máximo 100 URI entrantes por operación de Share Sheet;
- CodeQL Java/Kotlin IMPLEMENTADO;
- assembleRelease y artefacto release unsigned IMPLEMENTADOS en CI;
- checklist creado en docs/RELEASE_CHECKLIST.md.

## Pendientes reales / bloqueos

**IMPLEMENTADO_PENDIENTE_VALIDACIÓN:**
- CI Android de la rama debe quedar verde;
- CodeQL debe completar sin hallazgos bloqueantes;
- validar físicamente modo claro y pantalla pequeña;
- ejecutar regresión rápida con el APK de main después del merge.

**DECISIÓN PENDIENTE PARA PUBLICAR:**
- canal de distribución;
- firma de producción;
- licencia propia del código si el dueño desea declarar reutilización open source;
- si se usa Google Play, cumplimiento de políticas para permisos amplios.

## Siguiente paso exacto

1. Abrir PR de release/final-review y ejecutar Android CI + CodeQL.
2. Corregir cualquier hallazgo bloqueante.
3. Fusionar solo con CI/análisis verdes y comprobar main.
4. Entregar APK debug de main para regresión física y pedir únicamente modo claro/pantalla pequeña + smoke test.
5. Con QA físico cerrado, marcar candidata técnica; firma/publicación quedan como gate separado.
