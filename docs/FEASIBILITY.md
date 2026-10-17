# FEASIBILITY — Validación técnica del producto local

**Estado:** viable en Android moderno; las nuevas operaciones sobre originales requieren validación física por OEM/API.

## Conclusión

OclAx es técnicamente viable como:

> una bandeja local y temporal de contenido mixto que recibe contenido de forma explícita, lo organiza, permite explorar el dispositivo y vuelve a exponerlo rápidamente a otras aplicaciones.

No depende de nube ni red.

## Entrada viable

Vías robustas:
- Android Share Sheet: `ACTION_SEND` / `ACTION_SEND_MULTIPLE`;
- picker propio para `ACTION_GET_CONTENT`;
- DocumentsProvider/SAF;
- acceso explícito a **Mi dispositivo**;
- portapapeles mientras OclAx está en primer plano cuando aplica.

No prometer:
- captura universal del portapapeles en segundo plano;
- controlar la UI de la app Files;
- que todas las apps externas acepten todos los MIME.

## Almacenamiento

Viable:
- payload privado;
- metadata separada;
- miniaturas;
- texto, imágenes, PDF, APK, video, audio, documentos y otros;
- retención configurable;
- Fijados;
- límites de tamaño/espacio;
- cleanup oportunista + programación mediante AlarmManager.

## Capturas de pantalla

Objetivo:
- evitar que una captura recién creada solo para compartir quede acumulada en galería.

Viable de forma segura si el origen puede verificarse con MediaStore:
1. identificar screenshot por nombre/ruta;
2. exigir `DATE_ADDED`;
3. limitar la ventana a ≤60 s;
4. persistir la URI exacta;
5. al vencer, comprobar que sigue siendo la misma fila/archivo;
6. borrar mediante ContentResolver.

Limitación:
- algunos OEM/apps pueden compartir una screenshot mediante un provider que no permite relacionarla de forma segura con MediaStore;
- en ese caso OclAx debe conservar el original, no adivinar.

## Renombrado

### Mi dispositivo
MediaStore permite actualizar `DISPLAY_NAME` cuando Android concede acceso suficiente.

### Copia OclAx
El nombre interno/visible es controlado por OclAx. Si se conservó una URI MediaStore de origen, puede intentarse el mismo rename sobre el original.

Limitaciones:
- providers externos pueden ser read-only;
- Android puede negar una actualización aunque la URI siga siendo legible;
- el producto debe informar el resultado real.

## Salida hacia otras apps

Viable:
- Share Sheet;
- ClipboardManager para texto/imagen;
- FileProvider para copias privadas;
- DocumentsProvider;
- picker propio `ACTION_GET_CONTENT`.

Para archivos arbitrarios, selector/Share son las rutas confiables. No existe “pegar cualquier archivo en cualquier app” universal.

## DocumentsProvider

Puede exponer:
- contenido privado OclAx;
- jerarquías virtuales;
- archivos reales referenciados por MediaStore;
- búsqueda;
- recientes;
- miniaturas.

Límite de plataforma:
- Android Files dibuja la interfaz;
- OclAx no puede inyectar su Compose UI dentro de esa aplicación;
- para apariencia propia existe `OclAxPickerActivity`.

## Mi dispositivo

Con acceso amplio aprobado:
- inventario de archivos por MediaStore;
- apps instaladas por PackageManager;
- abrir/compartir/copiar;
- renombrar;
- eliminar original con confirmación cuando corresponda;
- miniaturas y categorías.

Riesgos:
- inventarios grandes requieren medir I/O antes de optimizar;
- permisos pueden ser revocados;
- APIs de MediaStore cambian por versión Android.

## Seguridad

Requisitos:
- cero permiso de Internet;
- entradas tratadas como hostiles;
- nombres sanitizados;
- no ejecutar contenido recibido;
- límites de tamaño;
- URI `content://`;
- metadatos de origen privados;
- borrado automático fail-closed;
- no buscar coincidencias aproximadas para destruir un original.

## Dirección actual

Flujo principal:

`crear/seleccionar/compartir → OclAx → usar temporalmente → autolimpieza`

y, cuando otra app pide un archivo:

`otra app → Elegir con OclAx o Archivos → OclAx / Mi dispositivo → seleccionar → volver`

Prioridades:
1. recepción local;
2. contenido temporal;
3. acceso real al dispositivo;
4. renombrado;
5. autolimpieza segura;
6. picker/provider;
7. cero red.

## Fuera de alcance

Retirado por DEC-032:
- transferencia OclAx↔OclAx;
- LAN;
- Syncthing;
- relay;
- discovery;
- servidores.

También fuera por ahora:
- IME propio;
- Accessibility/Shizuku;
- nube;
- IA;
- editor de archivos avanzado.

Si cualquiera de estos objetivos vuelve, necesita una nueva decisión explícita; no se conserva infraestructura inactiva.
