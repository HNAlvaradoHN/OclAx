# TASKS

## Prioridad actual

### PRODUCT-006 — Selección múltiple segura en Mi dispositivo
**Estado:** IMPLEMENTED_PENDING_VALIDATION  
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
- selección múltiple dentro de la aplicación funciona.

Pendiente:
- compartir varios archivos reales;
- compartir varias apps instaladas;
- confirmar que Atrás cancela selección.

---



### PRODUCT-005 — Selección múltiple segura + iconos de apps en Android Files
**Estado:** IMPLEMENTED_PENDING_VALIDATION  
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

Pendiente:
- validar Fijar/Desfijar en lote.

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
**Estado:** IMPLEMENTED_PENDING_VALIDATION  
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

Pendiente:
- seleccionar una app de APK único y confirmar retorno a la app receptora;
- confirmar lectura del APK seleccionado por el caller.

---

### PICKER-002 — Mostrar aplicaciones instaladas al adjuntar desde otra app
**Estado:** IMPLEMENTED_PENDING_VALIDATION  
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

Pendiente:
- validar **Elegir con OclAx → Mi dispositivo → Aplicaciones** desde una app externa;
- seleccionar una app de APK único;
- probar una app con splits y confirmar el comportamiento según selección múltiple.

---

### LOCAL-001 — Retirar LAN y simplificar OclAx
**Estado:** IMPLEMENTED_PENDING_VALIDATION  
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

Pendiente:
- auditoría final de referencias/residuos;
- validar APK/Manifest generado.

---

### DATA-002 — Limpieza inteligente de capturas recientes
**Estado:** IMPLEMENTED_PENDING_VALIDATION  
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

Pendiente:
- prueba física de expiración completa con captura recién creada;
- prueba física con captura existente >60 s;
- prueba de Fijados;
- comprobar comportamiento del OEM si el Share de screenshot no entrega URI MediaStore.

---

### FILES-001 — Renombrado real y selector combinado
**Estado:** IMPLEMENTED_PENDING_VALIDATION  
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

Pendiente:
- renombrado físico de imagen/documento;
- renombrado de una copia OclAx con original enlazado;
- validar Files/DocumentsProvider con inventario real;
- confirmar que Android Files sigue devolviendo el URI correcto a la app llamadora.

---

### SEC-001 — Protecciones del repositorio y release
**Estado:** IN_PROGRESS  
**Prioridad:** media

Completado:
- ruleset de main;
- PR obligatorio;
- force-push/borrado de rama principal protegidos;
- Secret Scanning/Push Protection revisados;
- Dependabot;
- CI Android con permisos mínimos;
- firma estable de APK de pruebas almacenada solo en Secrets.

Pendiente antes de release estable:
- CodeQL/code scanning si sigue disponible sin costo;
- revisión final de permisos/manifest;
- revisión de dependencias y licencia;
- build release y checklist de publicación.

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
