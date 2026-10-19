# TOOLS — Herramientas externas de OclAx

GitHub es la fuente técnica de verdad. Ninguna herramienta externa reemplaza AGENTS.md, memoria oficial, código, pruebas o CI.

## GitHub

**Estado:** obligatorio / autorizado.  
**Uso:** repositorio, ramas, PR, CI, issues, memoria oficial y artefactos.  
**Reglas:** mínimo privilegio; no publicar secretos ni datos privados.

## ChatGPT / OclAx 📲

**Estado:** coordinador autorizado.  
**Uso:** desarrollo, revisión, investigación, documentación y coordinación según AGENTS.md.  
**Regla:** sincronizar antes de modificar; autonomía solo dentro de una tarea autorizada.

## Android / Gradle

**Estado:** stack activo.  
**Uso:** Kotlin, Jetpack Compose, MediaStore, DocumentsProvider, FileProvider, AlarmManager y APIs de plataforma.  
**Regla:** preferir APIs estándar antes de añadir dependencias.

## GitHub Actions

**Estado:** CI activo.  
**Uso:** unit tests, lint, assembleDebug y artefacto APK.  
**Permisos:** `contents: read`.  
**Firma:** main puede reconstruir un keystore de pruebas desde Secrets; PR no recibe esos secretos.

El pipeline nativo de Syncthing fue eliminado por DEC-032.

## CodeQL

**Estado:** activo en revisión final.  
**Uso:** análisis estático Java/Kotlin en PR, push a main y ejecución semanal.  
**Costo:** sin costo para este repositorio público.  
**Permisos CI:** `contents: read` + `security-events: write`.  
**Regla:** un hallazgo bloqueante impide cerrar release hasta corregirlo o justificar con evidencia que sea falso positivo.

## Syncthing

**Estado:** RETIRADO.  
**Razón:** el dueño eliminó la función LAN/transferencia entre dispositivos.

No es dependencia, herramienta activa ni componente de CI. Las referencias históricas permanecen solo en commits/decisiones antiguas y no justifican reinstalarlo sin una nueva decisión explícita.

## Google Stitch

**Estado:** opcional.  
**Uso:** exploración visual/prototipos.  
**No usar para:** secretos, datos privados ni fuente de verdad.

## Google AI Studio

**Estado:** opcional para experimentos puntuales.  
**Reglas:** no enviar secretos/datos privados; verificar costo/cuota antes de cualquier uso facturable.

## Otras herramientas

Cualquier herramienta nueva que se vuelva parte real del flujo debe registrar:
- necesidad;
- permisos;
- costo;
- datos que recibe;
- riesgo de privacidad;
- alternativa local/nativa.

No activar servicios pagos ni transferir información privada sin autorización explícita.
