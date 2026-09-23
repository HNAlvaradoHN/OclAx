# PROJECT_STATE

## Estado

**Fase:** primera prueba vertical Android.  
**Aplicación:** implementada, pendiente de validación.  
**Repositorio:** público.  
**Protocol version:** 2.

## Qué ya está definido

- Nombre: OclAx.
- Significado: Open Content, Local Access & eXchange.
- Identidad del agente: OclAx 📲.
- Repositorio como fuente técnica de verdad.
- Handshake obligatorio por chat.
- Memoria oficial del proyecto.
- Prioridad máxima a seguridad, privacidad e integridad.
- Arquitectura separada por responsabilidades.
- Feedback del usuario clasificado en BLOQUEANTE / NO BLOQUEANTE.
- Reemplazo real: lo viejo se elimina cuando deja de tener consumidores.
- Aplicación Android local-first orientada a contenido temporal.
- OclAx se define como selector/bandeja ordenada para insertar, no como gestor de archivos completo.

## Qué funciona

- Protocolo maestro instalado.
- Identidad oficial instalada.
- Estructura de memoria oficial iniciada.
- Revisores detallados instalados en `docs/REVIEW_ROLES.md`.
- Memoria visual inicial instalada en `docs/DESIGN.md`.
- Política de herramientas externas instalada en `docs/TOOLS.md`.

## Qué ya existe en la prueba vertical

- base Android nativa;
- recepción por Share Sheet;
- almacenamiento local privado;
- publicación de texto/imagen al portapapeles;
- DocumentsProvider de solo lectura;
- búsqueda básica;
- tests unitarios;
- lint/build/CI;
- generación de APK debug.

## Qué todavía no existe

- validación física en dispositivo;
- autolimpieza configurable;
- fijados;
- categorías visuales completas;
- release.

## Bloqueos

Ninguno conocido para continuar con la configuración del repositorio.

## Investigación reciente

- La viabilidad técnica del concepto está documentada en `docs/FEASIBILITY.md`.
- El estado de protecciones del repositorio está documentado en `docs/SECURITY_PROTECTIONS.md`.
- La integración actual no puede cambiar administrativamente branch protection/rulesets.

## Siguiente paso exacto

1. Obtener CI verde de la prueba vertical.
2. Instalar el APK debug en un teléfono real.
3. Validar: Compartir → OclAx → portapapeles y + / Archivos → OclAx → Recientes → insertar.
4. Corregir cualquier incompatibilidad real antes de ampliar UI o funciones.
