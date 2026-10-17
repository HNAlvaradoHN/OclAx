# SECURITY — Memoria técnica

## Principio

El repositorio es público. Nunca deben entrar secretos, credenciales, documentos privados, capturas reales con PII, rutas personales, datos de terceros ni logs sensibles.

Prioridad:
1. seguridad;
2. privacidad e integridad;
3. exactitud;
4. estabilidad.

## Superficie vigente

OclAx es una aplicación Android local.

**No usa red:**
- no declara `INTERNET`;
- no mantiene listener;
- no ejecuta Syncthing;
- no usa multicast, discovery, relay, NAT ni foreground service de datos;
- no compila ni empaqueta runtimes nativos de transferencia.

DEC-032 retiró el bloque de red completo.

## Contenido recibido

Todo contenido externo se considera no confiable.

Controles:
- solo URI `content://` para importación;
- nombres sanitizados y desacoplados de rutas;
- directorios privados opacos por UUID;
- streaming con buffers acotados;
- límite defensivo de 4 GiB;
- margen de almacenamiento libre reservado;
- no autoejecutar APK ni archivos recibidos;
- FileProvider limitado al almacenamiento privado configurado;
- DocumentsProvider de solo lectura.

## Acceso amplio al dispositivo

OclAx conserva `MANAGE_EXTERNAL_STORAGE` porque **Mi dispositivo** es una función explícita aprobada por el dueño.

También conserva:
- `QUERY_ALL_PACKAGES` para inventario de aplicaciones;
- permisos legacy de almacenamiento solo en APIs antiguas;
- `REQUEST_INSTALL_PACKAGES` para flujo explícito de APK;
- `RECEIVE_BOOT_COMPLETED` únicamente para reprogramar retención.

Reglas:
- negar/revocar acceso amplio no debe romper la bandeja OclAx;
- ninguna operación sobre originales se oculta como una operación sobre copias;
- borrar manualmente un original requiere acción explícita/confirmación cuando Android la exige;
- renombrar desde **Mi dispositivo** modifica el original porque el usuario lo pidió expresamente.

## Autolimpieza y originales

### Regla general

La retención elimina copias privadas OclAx.

### Única excepción automática

DEC-033 permite eliminar también un original **solo** para una captura de pantalla recién creada que cumpla simultáneamente:
- MIME de imagen;
- URI MediaStore;
- nombre/ruta reconocible como screenshot;
- `DATE_ADDED` disponible;
- edad de 0 a 60 000 ms en el momento de compartir.

No se usa `DATE_MODIFIED` como sustituto de creación para autorizar borrado.

Antes de borrar:
- se usa exactamente la URI persistida;
- se comprueba que siga existiendo;
- se compara tamaño cuando está disponible;
- se compara `DATE_ADDED`;
- si algo no coincide, se conserva el original;
- si Android niega el borrado, se conserva el original.

No se busca “una foto parecida” ni otro archivo con el mismo nombre.

**Fijados:** no expiran y por tanto no disparan borrado automático del original.

## Metadata de origen

Para soportar renombrado y limpieza segura se pueden persistir, dentro del directorio privado del elemento:
- URI de origen;
- nombre visible;
- tamaño;
- fecha de alta MediaStore;
- booleano de elegibilidad de limpieza del original.

No se envían fuera del dispositivo, no se imprimen en logs y no se suben al repo.

## Renombrado

### Copia OclAx
- el nombre visible puede cambiar;
- si existe un origen MediaStore enlazado, OclAx intenta aplicar el mismo `DISPLAY_NAME`;
- si el origen no admite escritura, se informa en vez de afirmar éxito total.

### Mi dispositivo
- `DeviceContentRepository` usa MediaStore/ContentResolver;
- el nombre se sanitiza;
- la URI original no se reemplaza por una ruta de filesystem inventada.

## Selectores

### OclAxPickerActivity
- solo `ACTION_GET_CONTENT`;
- respeta MIME solicitado;
- devuelve URI `content://` y permiso temporal de lectura;
- no importa el archivo solo para devolverlo.

### DocumentsProvider
- exportado porque Android lo requiere;
- protegido por `android.permission.MANAGE_DOCUMENTS`;
- expone archivos en modo lectura;
- puede mostrar contenido OclAx y referencias a contenido real del dispositivo;
- abrir contenido real delega en el ContentResolver de Android.

## Retención programada

`RetentionCleanupReceiver`:
- no está exportado a apps de terceros;
- acepta solo BOOT_COMPLETED o la acción interna de cleanup;
- no solicita alarma exacta;
- no inicia red ni servicio persistente.

Riesgo residual: Android puede diferir la alarma bajo ahorro de batería. Esto afecta puntualidad, no el límite de qué archivos se pueden borrar.

## Aplicaciones instaladas

Compartir una app:
- solo copia `sourceDir` + `splitSourceDirs`;
- nunca copia `dataDir`, preferencias, DB, cuentas o caché privada;
- exportación temporal limitada y eliminable;
- compartir no equivale a instalar/ejecutar.

## GitHub Actions

CI:
- `permissions: contents: read`;
- PR no recibe secretos de firma;
- main puede reconstruir temporalmente el keystore de pruebas desde Secrets;
- no se imprime ni se publica el keystore;
- ya no descarga/compila código Syncthing.

Antes de agregar una dependencia/servicio:
- justificar necesidad;
- revisar licencia/mantenimiento/vulnerabilidades/costo;
- preferir plataforma estándar si basta.

## Revisiones requeridas para la renovación local

Seguridad:
- confirmar Manifest sin permisos LAN;
- confirmar receiver no exportado;
- revisar borrado de screenshot fail-closed;
- revisar rename sin path traversal.

Privacidad:
- metadata de origen permanece privada;
- no borrar fotos normales/antiguas;
- no registrar URIs/nombres privados.

Plataforma Android:
- probar MediaStore update/delete en API objetivo;
- probar reinicio/alarma;
- probar DocumentsProvider con acceso concedido/revocado.

QA:
- screenshot ≤60 s;
- screenshot >60 s;
- imagen normal reciente;
- Fijados;
- source URI inválida;
- rename permitido/denegado;
- picker/provider.

## Incidentes

Si un secreto llega a un commit público:
1. asumir compromiso;
2. detener exposición;
3. informar;
4. revocar/rotar;
5. eliminar;
6. revisar historial/artefactos;
7. reescribir historial solo con autorización explícita;
8. documentar sin copiar el secreto.

## Picker de aplicaciones instaladas

PICKER-002 reutiliza InstalledAppExporter:
- solo copia sourceDir y splitSourceDirs;
- no toca ni exporta dataDir, preferencias, DB, cuentas, sesiones o caché privada;
- usa FileProvider y grants temporales de lectura;
- respeta el MIME pedido por el caller;
- una instalación split no se degrada silenciosamente a solo base.apk;
- no añade permisos nuevos.

## Aplicaciones instaladas por DocumentsProvider

PICKER-003 expone solo código de instalación:
- `sourceDir` y `splitSourceDirs` obtenidos de PackageManager;
- file descriptors de solo lectura;
- IDs virtuales validados; un documentId no se convierte en una ruta arbitraria;
- icono/nombre/package como metadata visible;
- nunca `dataDir`, preferencias, DB, cuentas, sesiones o caché privada;
- sin permisos nuevos;
- apps con splits se mantienen separadas en sus APK componentes; no se inventa un APK monolítico.
