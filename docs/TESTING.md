# TESTING

## CI vigente

Cada PR y cada push a `main` ejecutan:

```bash
gradle --no-daemon :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Si todo pasa, CI publica `OclAx-debug`.

La renovación local elimina por completo el job nativo de Syncthing; ya no existe build/caché/verificación JNI de transferencia.

## Pruebas unitarias vigentes

### SafeNamesTest
Valida:
- sanitización de nombres;
- eliminación de componentes de ruta;
- fallback;
- extensión segura;
- nombre interno de payload.

### RetentionPolicyTest
Valida:
- expiración según retención;
- Fijados no expiran;
- `Nunca`;
- fallback seguro a 24 h.

### SourceRetentionPolicyTest
Valida la nueva excepción de capturas:
- screenshot MediaStore dentro de 60 s → elegible para borrar original al vencer;
- screenshot de más de 60 s → original protegido;
- imagen normal reciente → original protegido;
- screenshot de fuente no MediaStore → original protegido.

### ContentTypeTest
Valida clasificación por MIME y elegibilidad de Copiar.

### BulkActionPolicyTest
Valida:
- MIME específico cuando todos los elementos comparten tipo;
- wildcard `*/*` para lotes mixtos;
- objetivo Fijar/Desfijar según el estado actual de la selección.

### PickerCategoryTest / PickerMimeMatcherTest
Validan categorías, MIME exacto/wildcard, restricciones del caller y que **Aplicaciones** solo aparezca cuando el MIME solicitado acepta APK.

### BackNavigationPolicyTest
Valida jerarquía de Atrás y salida.

### CategoryAccentTest
Valida paleta semántica/fallback.

## Validación física nueva — run 314

VERIFICADO:
- UI principal sin LAN/Motor/Mis dispositivos;
- screenshot reciente reconocida como elegible para limpiar el original;
- screenshot elegible fijada muestra que el original queda protegido;
- el flujo de selección muestra el resto del contenido real del dispositivo.

HALLAZGO confirmado:
- el picker externo no mostraba aplicaciones instaladas; se corrige en PICKER-002.

## Validación física ya confirmada

VERIFICADO históricamente:
- OclAx aparece como fuente de archivos;
- Compartir → OclAx → Pegar funciona;
- DocumentsProvider devuelve contenido a una app receptora compatible;
- miniaturas de imagen, portada PDF y vistas de **Mi dispositivo** funcionan;
- abrir PDF/Word/imagen/video/audio/APK funciona en las rutas probadas;
- eliminar/cancelar original funciona para imagen y PDF/documento;
- Apps instaladas se muestran;
- lista/cuadrícula persiste por categoría;
- picker propio **Elegir con OclAx** funciona con OclAx y Mi dispositivo;
- picker devuelve PDF desde OclAx e imagen desde Mi dispositivo;
- navegación Atrás fue validada;
- APK de pruebas con firma estable puede actualizar la instalación conservando datos.

La validación histórica LAN no es un gate: esa funcionalidad fue retirada por DEC-032.

## Plan obligatorio — renovación local

### LOCAL-001 — retiro de LAN

Validación automática:
1. no existen clases/paquetes de transferencia;
2. no existe servicio Syncthing en Manifest;
3. no existen permisos `INTERNET`, `ACCESS_NETWORK_STATE`, multicast ni foreground data-sync;
4. no existe configuración de red loopback;
5. Gradle no fuerza empaquetado JNI para Syncthing;
6. workflow no descarga/compila/empaqueta runtimes;
7. tests, lint y assembleDebug pasan;
8. APK se genera sin depender de un artefacto nativo.

Validación física:
- pantalla principal no muestra **Enviar a dispositivo**, Motor ni Mis dispositivos;
- flujo OclAx/Mi dispositivo sigue funcionando;
- actualización sobre instalación previa no pierde las copias internas.

### DATA-002 — capturas recientes

Caso A — captura recién creada:
1. crear screenshot;
2. compartirla a OclAx dentro de ≤60 s;
3. confirmar indicador **Captura reciente · original se limpiará al vencer**;
4. comprobar que la copia funciona;
5. dejar vencer la retención;
6. confirmar desaparición de copia OclAx y original de galería.

Caso B — captura existente:
1. conservar screenshot durante >60 s;
2. compartirla después a OclAx;
3. confirmar que NO aparece el indicador de borrado de original;
4. dejar vencer;
5. copia OclAx desaparece;
6. original permanece en galería.

Caso C — imagen normal reciente:
- compartir foto normal aunque tenga <60 s;
- el original debe sobrevivir.

Caso D — Fijados:
- compartir screenshot elegible;
- fijarla antes del vencimiento;
- después del tiempo de retención deben sobrevivir copia y original;
- al desfijar, se aplica la retención normal.

Caso E — reinicio:
- con elemento pendiente, reiniciar el teléfono;
- confirmar que la expiración vuelve a programarse;
- aceptar que Android puede diferir una alarma inexacta bajo ahorro energético.

Caso F — identidad:
- si la URI original deja de existir o sus metadatos ya no coinciden, OclAx no debe buscar/borrar otro archivo.

### FILES-001 — renombrado

OclAx:
1. compartir archivo desde MediaStore;
2. abrir Renombrar;
3. usar un nombre nuevo válido;
4. comprobar nombre en OclAx;
5. comprobar mismo nombre en galería/Files;
6. reiniciar OclAx y confirmar persistencia.

Origen no modificable:
- usar un contenido de provider que no sea MediaStore;
- OclAx puede renombrar la copia, pero debe informar que el origen no permite renombrado.

Mi dispositivo:
1. elegir imagen/documento;
2. Renombrar;
3. comprobar cambio en OclAx/Mi dispositivo y fuera de OclAx;
4. comprobar apertura/compartir después del cambio.

### FILES-001 — DocumentsProvider

Con acceso amplio concedido:
1. desde una app externa elegir **Archivos**;
2. entrar a **OclAx**;
3. confirmar que se ven archivos, no únicamente carpetas;
4. confirmar entrada **Mi dispositivo**;
5. entrar y comprobar categorías + archivos reales;
6. buscar un archivo real por nombre;
7. abrir imagen/PDF y verificar miniatura cuando el Files del sistema la solicite;
8. seleccionar un archivo y confirmar retorno a la app llamadora.

Con acceso revocado:
- OclAx temporal debe seguir disponible;
- **Mi dispositivo** no debe filtrar ni inventar acceso.

Nota de plataforma:
- la apariencia exacta de la app **Archivos** es del sistema Android; la prueba verifica contenido/jerarquía/miniaturas, no que Files copie el diseño Compose de OclAx.
- para UI propia debe validarse **Elegir con OclAx**.

## Revisión obligatoria de la renovación

### Seguridad
**Estado:** PENDIENTE REVISIÓN FINAL  
Comprobar eliminación de red, receiver, límites de borrado y rename.

### Privacidad
**Estado:** PENDIENTE REVISIÓN FINAL  
Comprobar que source URI/metadatos son privados y que una foto normal nunca entra en borrado automático.

### Arquitectura
**Estado:** PENDIENTE REVISIÓN FINAL  
Comprobar separación ItemStore / policy / scheduler / repository / UI y ausencia de restos del motor retirado.

### Plataforma Android
**Estado:** PENDIENTE CI/FÍSICA  
Comprobar ContentResolver update/delete, AlarmManager/BOOT y DocumentsProvider.

### Calidad/Limpieza
**Estado:** PENDIENTE REVISIÓN FINAL  
Buscar imports, recursos, permisos, tests, docs y workflows huérfanos de LAN.

### QA
**Estado:** PENDIENTE CI/FÍSICA  
Ejecutar matriz anterior.

### Rendimiento
**Estado:** INFORMATIVO  
DocumentsProvider puede enumerar muchos archivos; la raíz limita la exposición directa inicial a 100, mientras **Mi dispositivo** conserva inventario completo. Validar un teléfono con inventario grande antes de optimizar.

### Diseño/UX/Accesibilidad
**Estado:** PENDIENTE FÍSICO  
Renombrar usa acción con contentDescription; la limpieza destructiva de screenshot tiene texto visible. Confirmar legibilidad/tamaños táctiles.

### Release
**Estado:** NO APLICA TODAVÍA  
No es release candidata hasta terminar validación física y SEC-001.

## Criterio de cierre

Una tarea pasa a DONE solo cuando:
- implementación está en main;
- CI relevante verde;
- revisiones aplicables cerradas;
- validación física obligatoria registrada cuando corresponda;
- memoria oficial reconciliada;
- no quedan restos de la función sustituida.

Mientras falte prueba real:
`IMPLEMENTED_PENDING_VALIDATION`.

### PICKER-002 — Aplicaciones instaladas

Validación automática:
1. PickerCategory.APPLICATIONS no se mezcla con archivos APK;
2. installedAppsCompatible() acepta */*, application/* y MIME APK;
3. rechaza image/* y application/pdf;
4. tests/lint/build verdes.

Validación física:
1. abrir una app que permita adjuntar archivo con MIME compatible;
2. elegir **OclAx → Mi dispositivo**;
3. confirmar tarjeta **Aplicaciones**;
4. entrar y ver iconos/nombres reales;
5. elegir una app con APK único y confirmar retorno al caller;
6. buscar por nombre/package;
7. con una app que use splits: caller sin múltiple debe recibir aviso; caller con múltiple debe recibir base + splits;
8. comprobar que no se exportan datos privados de la aplicación.

### PICKER-003 — Android Files / DocumentsProvider / Aplicaciones

Validación automática:
1. IDs de documento de apps instaladas hacen round-trip para APK único, carpeta y parte;
2. IDs malformados/rutas son rechazados;
3. provider compila con categoría Aplicaciones y MIME APK;
4. tests, lint y assembleDebug verdes.

Validación física:
1. desde una app externa elegir **Archivos** (no Elegir con OclAx);
2. entrar a **OclAx → Mi dispositivo**;
3. confirmar categoría **Aplicaciones**;
4. entrar y comprobar nombres/iconos;
5. app de APK único: seleccionar `Nombre.apk` y confirmar retorno al caller;
6. app con splits: abrir su carpeta y comprobar `base.apk` + splits;
7. búsqueda por nombre de app/package;
8. revocar acceso amplio a archivos y comprobar que **Aplicaciones** sigue disponible;
9. confirmar que nunca aparece dataDir ni contenido privado de la app.

### PICKER-004 — Carga estable de Aplicaciones en Android Files

Validación automática:
1. compilar `InstalledAppDocumentInfo` sin afectar la UI principal/picker;
2. DocumentsProvider usa `listInstalledAppDocuments()` para la carpeta;
3. documentos APK virtuales no anuncian thumbnail;
4. IDs seguros siguen cubiertos por `InstalledAppDocumentIdsTest`;
5. testDebugUnitTest + lintDebug + assembleDebug verdes.

Validación física:
1. instalar build sobre run 319;
2. entrar por `Archivos → OclAx → Mi dispositivo → Aplicaciones`;
3. la lista debe cargar sin mensaje de error;
4. recorrer varias apps para confirmar estabilidad;
5. seleccionar una app de APK único;
6. abrir una app split y comprobar base + splits;
7. volver atrás y entrar otra vez para detectar fallos por reconsulta.

### PRODUCT-005 — Selección múltiple e iconos de apps

Validación automática:
1. `BulkActionPolicyTest`;
2. compilar modo selección Compose;
3. ACTION_SEND_MULTIPLE usa FileProvider y grants de lectura;
4. borrado múltiple llama solo a `ItemStore.deleteItem`;
5. compartir lote rechaza más de 100 elementos para acotar el Intent/ClipData;
6. DocumentsProvider resuelve icono por package puntual;
7. el cache se consulta antes de recargar thumbnail;
8. tests, lint y assembleDebug verdes.

Validación física — selección:
1. entrar a **Todo** o una categoría con varios elementos;
2. tocar **Seleccionar**;
3. elegir 2+ tarjetas y confirmar contador/borde/checkbox;
4. **Todos/Ninguno**;
5. compartir 2+ elementos y comprobar recepción;
6. fijar selección mixta y comprobar que todos quedan fijados;
7. seleccionar solo fijados, Desfijar y comprobar estado;
8. eliminar varias copias y confirmar que los originales del teléfono siguen existiendo;
9. Atrás debe cancelar selección antes de salir de la categoría.

Validación física — iconos:
1. **Archivos → OclAx → Mi dispositivo → Aplicaciones**;
2. confirmar que la lista sigue cargando sin ERR-021;
3. observar si Android Files solicita/muestra iconos reales;
4. entrar a una app split y comprobar que la carpeta sigue abriendo;
5. si Files no muestra thumbnail en carpetas, registrar limitación de plataforma sin romper el flujo.

### PRODUCT-006 — Selección múltiple en Mi dispositivo

Validación automática:
1. DeviceBrowser compila con modo selección para archivos y apps;
2. archivos reales usan ACTION_SEND_MULTIPLE con grant temporal de lectura;
3. apps instaladas usan InstalledAppExporter y mantienen base + splits;
4. límites: 100 archivos, 20 apps y máximo 100 APK resultantes;
5. búsqueda se deshabilita durante selección;
6. tests, lint y assembleDebug verdes.

Validación física:
1. Mi dispositivo → Imágenes/PDF/otra categoría;
2. tocar Seleccionar y marcar 2+ archivos;
3. Todos/Ninguno y contador;
4. compartir varios archivos reales;
5. Atrás cancela selección;
6. Mi dispositivo → Aplicaciones;
7. seleccionar 2 apps y compartir;
8. confirmar que no aparece borrado/renombrado masivo de originales.
