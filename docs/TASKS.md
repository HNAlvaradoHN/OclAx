# TASKS

## Prioridad actual

### BRAND-001 — Icono propio de OclAx
**Estado:** DONE  
**Prioridad:** alta

Objetivo:
- sustituir el icono Android genérico por una identidad propia visible en launcher, recientes, información de app y superficies que consumen el icono del paquete.

Implementado en `feat/app-launcher-icon`:
- icono adaptativo negro/naranja con marca O+X;
- variante redonda;
- referencias `android:icon` y `android:roundIcon` en el Manifest;
- sin dependencias ni permisos nuevos.

Validado:
- Android CI runs 356/357 verdes;
- CodeQL runs 10/11 verdes;
- PR #91 fusionado a main;
- APK instalado físicamente por el dueño;
- icono validado visualmente en el dispositivo.

---


### PRODUCT-006 — Selección múltiple segura en Mi dispositivo
**Estado:** DONE  
**Prioridad:** alta

Evidencia física run 334:
- selección/compartir/borrado múltiple de copias OclAx funcionan;
- el dueño reporta que **Seleccionar** no aparece en **Mi dispositivo**.

Implementado en `fix/device-bulk-selection`:
- botón **Seleccionar** dentro de categorías de Mi dispositivo;
- archivos reales: selección individual, Todos/Ninguno y compartir lote;
- apps instaladas: selección individual, Todos/Ninguno y compartir varias;
- máximo 100 archivos o 20 apps por selección;
- apps instaladas conservan base + splits;
- Atrás cancela selección;
- búsqueda queda deshabilitada durante selección.

Límite deliberado:
- no borrar, mover ni renombrar originales en lote;
- esas operaciones permanecen individuales por riesgo de pérdida de datos y reglas de nombre/destino aún no definidas.

Validado:
- PR #88 y main run 336 verdes;
- botón **Seleccionar** visible en **Mi dispositivo → Apps**;
- selección múltiple dentro de la aplicación funciona;
- compartir varios archivos reales funciona;
- compartir varias apps instaladas funciona;
- Atrás cancela selección antes de navegar.

---



### PRODUCT-005 — Selección múltiple segura + iconos de apps en Android Files
**Estado:** DONE  
**Prioridad:** alta

Autorizado por el dueño después de validar run 321.

Implementado en `feat/bulk-actions-app-icons`:
- modo **Seleccionar** en listas OclAx;
- selección individual o todos los visibles;
- compartir varios elementos con ACTION_SEND_MULTIPLE;
- fijar/desfijar varios;
- eliminar varias copias OclAx con confirmación;
- el borrado masivo no toca originales;
- la búsqueda OclAx se deshabilita mientras hay selección para evitar operar sobre elementos ocultos;
- DocumentsProvider vuelve a ofrecer thumbnail para aplicaciones instaladas;
- el icono se obtiene solo para el package solicitado y se cachea;
- para apps con splits se ofrece thumbnail también en la carpeta virtual, sujeto a cómo Android Files decida representarlo.

Fuera de este alcance:
- renombrado masivo: necesita una regla de nombres;
- mover originales: necesita selección de destino y modifica archivos reales;
- borrado masivo de **Mi dispositivo**: se evita por riesgo de pérdida de datos.

Validado físicamente con run 334:
- selección múltiple OclAx;
- compartir varios;
- eliminar varias copias;
- icono real visible en APK componentes de una app split;
- Android Files mantiene icono genérico de carpeta para apps split en el nivel superior.

Validado adicionalmente:
- Fijar/Desfijar varios elementos funciona.

Cierre visual aceptado:
- Android Files puede mostrar icono genérico para carpetas de apps split;
- los APK internos sí muestran el icono real;
- no se cambia el formato de la app instalada solo para forzar la miniatura.

---

### PICKER-004 — Corregir carga de Aplicaciones en Android Files
**Estado:** DONE  
**Prioridad:** alta

Validado físicamente con run 321:
- Android Files carga **Aplicaciones** sin error;
- se muestran las aplicaciones instaladas;
- una app con splits abre su carpeta;
- se muestran `base.apk` y su split.

CI:
- PR #86 verde;
- main run 321 verde.

---

### PICKER-003 — Aplicaciones instaladas dentro de Android Files/SAF
**Estado:** DONE  
**Prioridad:** alta

Evidencia física:
- run 317 muestra **OclAx → Mi dispositivo** dentro de Android Files;
- se ven archivos/categorías reales, pero no **Aplicaciones**.

Causa verificada:
- esa ruta usa `OclAxDocumentsProvider`, no el picker propio;
- DocumentsProvider no exponía PackageManager/InstalledAppsRepository.

Implementado en `fix/documents-provider-installed-apps`:
- categoría **Aplicaciones**;
- app de APK único como documento virtual de solo lectura;
- icono real como miniatura;
- app con splits como carpeta que contiene base + splits;
- búsqueda por nombre/package;
- IDs virtuales validados;
- no se requiere MANAGE_EXTERNAL_STORAGE para Aplicaciones.

Validado físicamente:
- **Aplicaciones** aparece y carga en Android Files;
- una app split abre su carpeta y muestra base + split.

Validado físicamente:
- app de APK único seleccionable;
- retorno correcto a la app receptora;
- el caller puede leer el APK seleccionado;
- apps split siguen exponiendo base + splits sin datos privados.

---

### PICKER-002 — Mostrar aplicaciones instaladas al adjuntar desde otra app
**Estado:** DONE  
**Prioridad:** alta

Evidencia física:
- run 314 muestra el resto del contenido de **Mi dispositivo** al elegir archivo;
- las aplicaciones instaladas no aparecían.

Causa verificada:
- el picker propio solo cargaba archivos del dispositivo y copias OclAx; no cargaba InstalledAppsRepository.

Implementado en fix/picker-installed-apps:
- categoría **Aplicaciones** separada de **APK**;
- icono, nombre y package de aplicaciones instaladas;
- filtro según MIME pedido por la aplicación receptora;
- selección prepara APK mediante InstalledAppExporter;
- instalaciones con splits se entregan completas solo cuando el caller permite múltiples archivos;
- Aplicaciones no depende de MANAGE_EXTERNAL_STORAGE;
- tests de compatibilidad MIME.

Validado físicamente:
- **Elegir con OclAx → Mi dispositivo → Aplicaciones** funciona desde una app externa;
- app de APK único retorna correctamente;
- apps con splits respetan selección simple/múltiple y entregan el paquete de instalación correspondiente.

---

### LOCAL-001 — Retirar LAN y simplificar OclAx
**Estado:** DONE  
**Prioridad:** alta

Objetivo:
- eliminar completamente envío entre dispositivos, emparejamiento, LAN y Syncthing;
- conservar únicamente el producto local;
- limpiar código, permisos, servicios, workflows, tests, recursos y documentación huérfanos.

Implementado en `feat/local-content-refresh`:
- eliminado panel técnico y lógica de dispositivos;
- eliminado runtime Syncthing y canal de archivos;
- eliminados permisos de red/multicast/foreground data-sync;
- eliminado pipeline nativo de CI y empaquetado JNI;
- eliminado `network_security_config` asociado al REST local;
- eliminados tests exclusivos de transferencia.

Validado:
- árbol activo sin clases/recursos/configuración de transferencia;
- Manifest fuente sin permisos de red ni servicios;
- APK run 336 sin permisos de red detectables ni runtime Syncthing;
- únicos binarios nativos: `libandroidx.graphics.path.so` de AndroidX;
- Gradle sin JNI/Syncthing;
- workflow sin pipeline nativo/descargas de runtime.

Las referencias históricas conservadas están solo en documentación y no forman parte del runtime.

---

### DATA-002 — Limpieza inteligente de capturas recientes
**Estado:** DONE  
**Prioridad:** alta

Regla aprobada:
- solo capturas de pantalla verificables compartidas dentro de **60 segundos** de su alta en MediaStore pueden borrar también el original al vencer;
- capturas >60 s, imágenes normales o fuentes no verificables borran únicamente la copia OclAx;
- Fijados no expiran.

Implementado:
- metadata privada de origen;
- clasificación defensiva por imagen + MediaStore + nombre/ruta de screenshot + `DATE_ADDED`;
- revalidación de URI/tamaño/fecha antes de eliminar original;
- programación de próxima expiración mediante AlarmManager sin alarma exacta;
- UI informa si la captura está enlazada a la limpieza del original;
- pruebas unitarias de ventana de 60 s y casos negativos.

Validado físicamente en este dispositivo:
- screenshot antigua (>60 s) compartida sin marca de borrado del original;
- screenshot recién creada y compartida inmediatamente marcada como **Captura reciente · original se limpiará al vencer**;
- el flujo OEM entrega metadata suficiente para aplicar la política MediaStore/DATE_ADDED/60 s.

Validado físicamente:
- vencimiento de captura reciente correcto;
- captura antigua conserva el original al vencer;
- Fijados quedan protegidos durante el vencimiento.

DATA-002 queda cerrada para el alcance aprobado.

---

### FILES-001 — Renombrado real y selector combinado
**Estado:** DONE  
**Prioridad:** alta

Objetivo:
- renombrar desde OclAx y aplicar el cambio al original cuando exista un origen MediaStore enlazado;
- renombrar originales desde **Mi dispositivo**;
- desde **Archivos → OclAx**, acceder también a archivos reales del dispositivo.

Implementado:
- acción Renombrar en tarjetas OclAx;
- acción Renombrar en archivos de **Mi dispositivo**;
- DocumentsProvider expone copias OclAx, categorías, entrada **Mi dispositivo** y archivos reales recientes/directos;
- búsqueda/recientes incluyen archivos reales cuando hay acceso;
- miniaturas de imagen/video/PDF se conservan;
- picker propio `ACTION_GET_CONTENT` se mantiene como UI visual OclAx.

Validado físicamente:
- renombrado de imagen/documento desde **Mi dispositivo**;
- renombrado de copia OclAx con original enlazado;
- apertura y compartir continúan funcionando después del cambio;
- Files/DocumentsProvider funciona con inventario real;
- Android Files devuelve correctamente el contenido a la app llamadora.

---

### SEC-001 — Protecciones del repositorio y release
**Estado:** DONE  
**Prioridad:** alta

Completado:
- ruleset de main;
- PR obligatorio;
- force-push/borrado de rama principal protegidos;
- Secret Scanning/Push Protection revisados;
- Dependabot;
- CI Android con permisos mínimos;
- firma estable de APK de pruebas almacenada solo en Secrets;
- revisión final de permisos/Manifest y componentes exportados;
- revisión final de privacidad, almacenamiento y providers;
- auditoría de secretos por búsqueda del árbol activo;
- hardening del Share Sheet: máximo 100 URI por operación entrante;
- Kotlin tooling actualizado 2.4.10 → 2.4.21 por CVE-2026-53914;
- CodeQL Java/Kotlin añadido;
- CI ampliado para `:app:assembleRelease`;
- artefacto `OclAx-release-unsigned` añadido;
- checklist técnico en `docs/RELEASE_CHECKLIST.md`;
- revisión de licencias directas: AndroidX/AGP/Kotlin Apache-2.0; JUnit EPL-1.0 solo tests.

Validado en PR #90:
- Android CI run 352 verde con Kotlin 2.4.21, tests, lint, debug y assembleRelease;
- artefactos debug y release unsigned generados;
- CodeQL run 6 verde.

Validación final:
- modo claro: VERIFICADO;
- pantalla pequeña/ventana estrecha: VERIFICADO;
- regresión rápida física del APK con PR #91: VERIFICADO;
- icono propio de OclAx: VERIFICADO.

Resultado:
- candidata técnica local: **RELEASE_CANDIDATE_READY**.

Pendiente para publicación estable:
- decidir canal de distribución;
- decidir licencia del código fuente si se desea ofrecer reutilización open source;
- definir firma de producción; no reutilizar automáticamente la firma de pruebas;
- si el canal es Google Play, revisar elegibilidad/políticas de MANAGE_EXTERNAL_STORAGE, QUERY_ALL_PACKAGES y REQUEST_INSTALL_PACKAGES.

---

## Base conservada

### APP-001 — Base Android local
**Estado:** IMPLEMENTED_PENDING_VALIDATION

Conservado:
- Share Sheet;
- ItemStore privado;
- FileProvider;
- DocumentsProvider;
- portapapeles;
- búsqueda/categorías;
- Fijados/retención;
- CI y APK debug.

Pendiente:
- ampliar validación entre aplicaciones reales.

### PRODUCT-004 — Navegación visual y búsqueda
**Estado:** IMPLEMENTED_PENDING_VALIDATION

Conservado:
- tarjetas de categorías;
- búsqueda directa;
- paleta semántica;
- OclAx / Mi dispositivo;
- picker propio.

Pendiente:
- validación visual final modo claro/oscuro y pantallas pequeñas.

### DEVICE-001 — Mi dispositivo
**Estado:** IMPLEMENTED_PENDING_VALIDATION

Conservado:
- inventario de apps;
- archivos reales por MediaStore;
- acceso amplio explícito;
- abrir/compartir/copiar;
- miniaturas;
- borrar originales con confirmación;
- lista/cuadrícula por categoría.

Nuevo:
- renombrar originales;
- exposición de archivos reales dentro del DocumentsProvider.

Pendiente:
- revocación/reconcesión de acceso;
- rendimiento con inventarios grandes;
- paquete APK con splits.

---

## DONE

### GOV-001 — Gobernanza y memoria oficial
**Estado:** DONE

Incluye:
- AGENTS.md;
- identidad OclAx 📲;
- LOCKED_READ_ONLY / sincronización exhaustiva;
- numeración y handshake persistentes;
- autorización por objetivo;
- memoria/handoff automáticos;
- revisores condicionales;
- reemplazo real y limpieza obligatoria.

### RELEASE-001 — Firma estable de APK de pruebas
**Estado:** DONE

Validado:
- Secrets permanecen fuera del repo;
- builds de main pueden usar firma persistente;
- una actualización física posterior conservó instalación/datos.

### PICKER-001 — Elegir con OclAx
**Estado:** DONE

Validado físicamente:
- apertura desde app externa;
- OclAx / Mi dispositivo;
- categorías;
- cancelar;
- permiso negado;
- devolución de PDF desde OclAx e imagen desde Mi dispositivo.

### NAV-001 — Atrás jerárquico
**Estado:** DONE

Validado físicamente:
- categorías/búsqueda retroceden antes de salir;
- salida desde raíz se confirma;
- picker vuelve correctamente a la aplicación llamadora.

---

## Alcance retirado

El bloque histórico TRANSFER-001/002/003/004 y sus errores LAN dejan de ser tareas activas por **DEC-032**. No se mantienen como deuda ni como funcionalidad futura implícita. Si el dueño quisiera transmisión entre dispositivos otra vez, sería una nueva decisión/tarea y no una reactivación automática del código eliminado.

---

### I18N-001 — Inglés + español y documentación pública
**Estado:** IMPLEMENTED_PENDING_VALIDATION  
**Prioridad:** alta

Autorizado por el dueño para preparar la publicación por GitHub.

Implementado en `feat/bilingual-release-docs`:
- inglés como recursos por defecto;
- español en `values-es`;
- `localeConfig` para inglés/español;
- extracción de copy principal de MainActivity, Mi dispositivo, picker, Share Sheet, provider y errores visibles;
- README principal en inglés y README equivalente en español;
- explicación pública de funciones, privacidad, permisos e instalación prevista mediante GitHub Releases;
- sin permisos, dependencias, red ni telemetría nuevos.

Pendiente:
- Android CI + lint + build;
- CodeQL;
- validación física rápida en inglés y español, incluyendo ventana estrecha por posibles textos ingleses más largos.

### FEEDBACK-001 — Confirmación flotante del portapapeles
**Estado:** PENDING  
**Tipo:** hallazgo no bloqueante / comportamiento de plataforma

VERIFICADO:
- OclAx copia automáticamente texto/imágenes compartidos al portapapeles mediante `ClipboardManager.setPrimaryClip`;
- Android puede mostrar su propia confirmación/preview del portapapeles;
- OclAx además muestra su propio Toast al terminar el ingreso.

Propuesta pendiente de autorización:
- conservar la copia automática;
- evitar confirmación duplicada propia cuando la versión de Android ya muestra confirmación del sistema.
- no prometer que OclAx puede suprimir la UI de portapapeles del sistema/OEM.

### FEEDBACK-002 — Conservar original de captura sin fijar
**Estado:** PENDING  
**Tipo:** mejora de producto no bloqueante

Idea del dueño:
- permitir que una captura reciente siga la autolimpieza normal de su copia OclAx, pero que su original quede protegido permanentemente sin convertir el elemento en Fijado.

Propuesta técnica:
- acción/estado explícito **Conservar original** que desactive `deleteSourceOnExpiry` para ese elemento;
- Fijar conserva su significado actual: mantener la copia OclAx sin vencimiento.
