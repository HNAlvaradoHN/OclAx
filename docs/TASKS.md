# TASKS

## Prioridad actual

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
- CI verde;
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
- CI;
- prueba física con captura recién creada;
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
- CI;
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
