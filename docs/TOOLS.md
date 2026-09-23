# TOOLS — Herramientas externas de OclAx

GitHub sigue siendo la fuente técnica de verdad. Ninguna herramienta externa reemplaza AGENTS.md, la memoria oficial, el código, pruebas o CI.

## GitHub

**Estado:** obligatorio / autorizado.  
**Uso:** código, documentación, historial, PR, CI, issues y memoria oficial.  
**Regla:** no publicar secretos ni datos privados.

## ChatGPT / OclAx 📲

**Estado:** coordinador autorizado.  
**Uso:** desarrollo, revisión, investigación, documentación y coordinación conforme a AGENTS.md.  
**Regla:** sincronizar antes de modificar.

## Syncthing

**Estado:** candidato técnico integrado para prueba local; pendiente validación física antes de adopción final.  
**Uso previsto:** motor de transporte OclAx ↔ OclAx detrás de una capa propia, sin exponer al usuario carpetas/sincronización internas.  
**Versión fijada para el spike (2026-09-23):** v2.1.5, commit verificado.  
**Build:** el CI de OclAx construye Android arm64-v8a, armeabi-v7a, x86_64 y x86 en un job sin secretos de firma, genera/verifica SHA-256 y solo después entrega los binarios al job que compila/firma el APK.  
**Referencia Android:** el wrapper oficial `syncthing/syncthing-android` está archivado; `researchxxl/syncthing-android` mantiene un fork comunitario activo y sirve solo como referencia de empaquetado/foreground service, no como dependencia automática.

**Costo:** software libre; los relays públicos no implican un servicio pago de OclAx, pero tampoco son un SLA. No desplegar relay/servidor propio con costo sin autorización.

**Licencia:** Syncthing se distribuye bajo MPL-2.0. Antes de una release estable de OclAx que incluya el binario, revisar y cumplir avisos/atribución y disponibilidad del código fuente cubierto; el repo pinnea versión/commit y no debe tratar la licencia como opcional.

**Privacidad/seguridad:**
- tráfico entre pares cifrado extremo a extremo por el motor;
- discovery/relay pueden revelar metadatos de conexión como IP y device ID a esos servicios;
- API/GUI local debe quedar enlazada únicamente a loopback;
- API key local generada por instalación y almacenada de forma privada;
- el modo de prueba arranca aislado: listener de sincronización solo en loopback; discovery global/local, relay, NAT, auto-upgrade, usage reporting y crash reporting desactivados;
- binarios nativos deben fijarse por versión, verificarse y construirse en CI aislada de secretos privilegiados.

## Google Stitch

**Estado:** opcional para diseño/prototipado.  
**Uso previsto:** explorar interfaces y prototipos; ayudar a definir DESIGN.md.  
**No usar para:** almacenar secretos, datos privados o convertirse en fuente técnica de verdad.  
**Regla:** decisiones duraderas vuelven a GitHub.

## Google AI Studio

**Estado:** opcional para experimentos/prototipos.  
**Uso previsto:** pruebas de conceptos o segunda opinión técnica cuando aporte valor.  
**Reglas:** no subir secretos/datos privados; no convertirlo en dependencia obligatoria; verificar costos, cuotas y condiciones vigentes antes de uso que pueda generar consumo.

## Google Antigravity

**Estado:** reservado / no obligatorio.  
**Uso futuro:** trabajo de desarrollo paralelo o especializado si aporta valor real.  
**Regla:** antes de conceder acceso de escritura o integrarlo al flujo, registrar una decisión explícita y revisar seguridad/costo.

## Google Mixboard

**Estado:** opcional y ocasional.  
**Uso previsto:** exploración visual/moodboard.  
**Regla:** no usar información privada del proyecto o usuarios.

## Google Opal

**Estado:** no necesario actualmente.  
**Razón:** no resuelve una necesidad central del flujo Android nativo actual.  
**Regla:** reevaluar solo si aparece un caso de uso concreto.

## Regla general

Antes de introducir una herramienta nueva:
- demostrar el problema que resuelve;
- revisar privacidad;
- revisar permisos;
- revisar costo/cuotas actuales;
- evitar duplicar capacidades existentes;
- usar mínimo acceso;
- registrar la decisión si pasa a formar parte del flujo.
