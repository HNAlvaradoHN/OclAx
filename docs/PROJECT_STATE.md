# PROJECT_STATE

## Estado

**Fase:** refinamiento de la primera vertical Android local.  
**Aplicación:** funcional en main; renovación local fusionada con CI verde. Validación física en progreso.  
**Repositorio:** público.  
**Protocol version:** 6.  
**Autorización actual:** retirar LAN, renovar el producto local, añadir limpieza inteligente de capturas, renombrado real y acceso combinado desde selectores.

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

## Pendientes reales / bloqueos

**IMPLEMENTADO_PENDIENTE_VALIDACIÓN:**
- compilar y pasar tests/lint/CI del nuevo bloque;
- confirmar físicamente que el OEM entrega una URI MediaStore al compartir una captura reciente desde su flujo de screenshot;
- validar expiración real de captura reciente y supervivencia de captura >60 s;
- validar Fijados;
- validar renombrado OclAx↔original y renombrado desde Mi dispositivo;
- validar **Archivos → OclAx** mostrando contenido real del dispositivo;
- confirmar que el picker propio sigue devolviendo archivos correctamente;
- auditar APK/Manifest final para comprobar ausencia total de runtime/permisos LAN.

## Siguiente paso exacto

1. Ejecutar CI de la rama con tests, lint y assembleDebug.
2. Corregir cualquier fallo introducido por la renovación.
3. Ejecutar revisiones obligatorias de Seguridad, Privacidad, Arquitectura, Plataforma Android, Calidad/Limpieza, QA y UX.
4. Fusionar solo con CI verde.
5. Validar en dispositivo:
   - captura nueva compartida en ≤60 s;
   - captura existente compartida después de >60 s;
   - Fijados;
   - renombrado;
   - Files/DocumentsProvider con contenido OclAx + Mi dispositivo.
6. Registrar resultados físicos y cerrar las tareas correspondientes.
