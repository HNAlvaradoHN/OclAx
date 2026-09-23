# ARCHITECTURE

## Estado

Arquitectura conceptual inicial. No hay código todavía.

## Plataforma

Android.

## Principio

Un cambio visual no debe afectar innecesariamente lógica, datos o integraciones del sistema.

## Capas previstas

### UI / Presentación
Pantallas, navegación, componentes y estados visuales.

### Theme / Personalización
Colores, tipografía, formas, tamaños, espaciados y tokens visuales.

### Domain / Lógica
Reglas del producto y casos de uso.

### Data / Persistencia
Metadatos, archivos locales, preferencias y repositorios de datos.

### Platform / Android
Integraciones con APIs del sistema, selector de archivos, portapapeles, trabajos en segundo plano y permisos.

## Reglas

- No lógica de negocio enterrada en UI.
- No dependencias circulares.
- No sobre-modularización.
- Lo visual debe ser reemplazable sin reescribir el núcleo.
- Una implementación sustituida se elimina cuando ya no tenga consumidores.

## Tecnología

La selección concreta de librerías y versiones se decidirá y verificará antes de crear la base Android.
No se considera aprobada una dependencia solo por haber sido mencionada en una conversación.
