# TASKS

## DONE

### BOOT-001 — Instalar gobernanza del proyecto
**Estado:** DONE

Incluye:
- AGENTS.md;
- identidad OclAx 📲;
- memoria oficial;
- política de seguridad y privacidad;
- protocolo de sesión y handoff.

### GOV-002 — Evolucionar gobernanza a protocolo V2
**Estado:** DONE

Incluye:
- excepción formal de bootstrap;
- REVIEW_ROLES separado de AGENTS.md;
- DESIGN.md como memoria visual;
- TOOLS.md para herramientas externas;
- rol Diseño / UX / Accesibilidad;
- protocol_version actualizado a 2.

## PENDING

### SEC-001 — Configurar protecciones del repositorio público
**Estado:** IN_PROGRESS  
**Prioridad:** alta

Completado:
- ruleset activo en main;
- PR obligatorio;
- borrado y force-push bloqueados;
- Secret Scanning/Push Protection revisados para repo público;
- Dependabot configurado;
- CI mínimo Android instalado.

Pendiente:
- CodeQL/code scanning antes de release estable;
- mantener solo opciones sin costo salvo autorización.

### APP-001 — Inicializar base Android
**Estado:** IMPLEMENTED_PENDING_VALIDATION  
**Dependencia:** SEC-001

Implementado:
- proyecto Android mínimo;
- separación entre UI, almacenamiento, compartir y provider;
- Share Sheet para contenido mixto;
- portapapeles para texto e imagen;
- DocumentsProvider para Recientes;
- tests/lint/build en CI;
- APK debug como artefacto.

Validado:
- CI verde en PR y main;
- APK debug generado;
- DocumentsProvider visible en el selector de archivos del sistema en dispositivo real;
- flujo Compartir → OclAx → Pegar validado en Qwen;
- inserción desde OclAx validada en Qwen para los tipos de archivo probados;
- APK disponible en OclAx pero rechazado por Qwen por política de la app receptora.

Pendiente:
- validación física completa entre más aplicaciones.

### PRODUCT-001 — Formalizar alcance MVP
**Estado:** DONE

Alcance central confirmado:
- OclAx funciona como bandeja temporal universal;
- entrada rápida y explícita de contenido;
- texto/imagen compartidos quedan en Recientes y se publican al portapapeles;
- cualquier archivo queda disponible mediante Archivos → OclAx → Recientes;
- cero red;
- autolimpieza y fijado;
- sin IME/Accessibility/Shizuku en el MVP.

### PRODUCT-002 — Diseñar selector ordenado de inserción
**Estado:** IN_PROGRESS

Implementado en main:
- búsqueda;
- filtro Fijados;
- filtros Todo, Imágenes, Documentos, PDF, Apps/APK, Texto/Código, Video, Audio y Otros;
- etiquetas visuales por tipo;
- selector compacto de categorías alineado a la izquierda, reemplazando el riel permanente;
- identidad naranja más viva y saturada;
- tarjetas más compactas;
- acciones compactas Compartir, Fijar y Eliminar;
- Copiar solo para Texto/Código e Imágenes;
- confirmación obligatoria antes de borrar una copia;
- compartir hacia Android Sharesheet.

Validado:
- CI verde en PR y main para el bloque compacto/copiar;
- test unitario confirma que Copiar solo aplica a Texto/Código e Imágenes.

Pendiente:
- validación física del menú desplegable y Copiar;
- iconografía visual final/miniaturas;
- comprobar qué organización puede exponerse también dentro de DocumentsProvider sin añadir navegación innecesaria;
- validación física.

### DATA-001 — Retención segura y Fijados
**Estado:** IMPLEMENTED_PENDING_VALIDATION  
**Prioridad:** alta

Implementado en main:
- política de retención con 24 h por defecto;
- opciones 1 h, 24 h, 3 días, 7 días y nunca;
- exclusión absoluta de elementos fijados de la autolimpieza;
- borrado limitado a copias privadas bajo `filesDir/oclax/items`;
- persistencia del estado fijado;
- controles UI para cambiar retención y fijar/desfijar;
- CI verde antes de fusión.

Pendiente:
- validación física de expiración y fijados.


### RELEASE-001 — Firma estable de APK de pruebas
**Estado:** DONE  
**Prioridad:** alta

Problema:
- los APK debug generados en runners efímeros pueden quedar firmados con claves distintas;
- Android no permite actualizar una instalación existente si la firma cambia.

Objetivo:
- usar una clave de firma de pruebas estable;
- guardarla exclusivamente en GitHub Actions Secrets;
- nunca committear keystore, contraseña ni clave privada;
- mantener separada cualquier futura clave de release.

Preparado en código:
- Gradle acepta firma estable de prueba solo con los cuatro Secrets presentes;
- GitHub Actions reconstruye el keystore únicamente dentro del runner;
- versionCode de CI usa el número monotónico del workflow;
- una configuración parcial de Secrets hace fallar el build.

Validado en CI:
- los cuatro Secrets fueron reconocidos y permanecieron enmascarados;
- el keystore se reconstruyó únicamente dentro del runner;
- CI verde en PR y main;
- el primer APK con firma persistente fue generado como artefacto.

Validación física:
- actualización posterior instalada encima sin conflicto;
- datos internos conservados.


### PRODUCT-003 — Aplicaciones instaladas e iconografía real
**Estado:** IMPLEMENTED_PENDING_VALIDATION  
**Prioridad:** alta

Solicitud confirmada por prueba física:
- mostrar aplicaciones instaladas del dispositivo de forma ordenada;
- usar iconos reales de aplicaciones cuando Android los exponga;
- mejorar iconos/miniaturas de los elementos de OclAx;
- no confundir APK guardados con aplicaciones instaladas.

Implementado:
- categoría Aplicaciones separada de APK;
- listado alfabético de apps lanzables visibles;
- icono real de cada aplicación;
- búsqueda por nombre/paquete;
- iconos Material reconocibles por tipo para imágenes, PDF, APK, documentos, texto/código, video, audio y otros;
- sin `QUERY_ALL_PACKAGES`.

Corrección en curso tras prueba física:
- listado migrado a LauncherApps para mayor fiabilidad;
- etiquetas diferenciadas: Apps instaladas / APK guardados;
- DocumentsProvider organizado por categorías manteniendo acceso directo a recientes.

Pendiente:
- validar físicamente visibilidad, orden e iconos;
- validar carpetas por categoría dentro de Archivos → OclAx;
- miniaturas reales para contenido visual cuando aporte valor;
- decidir más adelante si tocar una app la abre o la convierte en destino directo de compartir.
