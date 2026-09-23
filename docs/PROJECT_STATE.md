# PROJECT_STATE

## Estado

**Fase:** gobernanza y seguridad inicial.  
**Aplicación:** todavía no implementada.  
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

## Qué todavía no existe

- Código Android.
- Build.
- Tests.
- CI de aplicación.
- Configuración final de protecciones de GitHub.
- Release.

## Bloqueos

Ninguno conocido para continuar con la configuración del repositorio.

## Investigación reciente

- La viabilidad técnica del concepto está documentada en `docs/FEASIBILITY.md`.
- El estado de protecciones del repositorio está documentado en `docs/SECURITY_PROTECTIONS.md`.
- La integración actual no puede cambiar administrativamente branch protection/rulesets.

## Siguiente paso exacto

1. Activar manualmente la protección de `main` en GitHub.
2. Inicializar la base Android mínima.
3. Construir primero una prueba vertical del flujo principal:
   - Compartir texto/imagen a OclAx;
   - guardar en Recientes;
   - publicar texto/imagen al portapapeles;
   - exponer Recientes mediante DocumentsProvider;
   - seleccionar un elemento desde otra app.
4. Validar ese flujo en dispositivo antes de ampliar UI o funciones.
