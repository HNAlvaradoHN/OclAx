# DECISIONS

## DEC-001 — Nombre y significado

**Decisión:** usar **OclAx**.

**Significado:** Open Content, Local Access & eXchange.

**Concepto:** contenido local disponible para guardar temporalmente, organizar y mover rápidamente entre aplicaciones.

---

## DEC-002 — Repositorio público

**Decisión:** el proyecto se desarrolla públicamente en GitHub.

**Consecuencia:** ningún secreto, dato personal, documento privado, credencial o material sensible puede entrar al repositorio.

---

## DEC-003 — Local-first

**Decisión:** el producto prioriza funcionamiento local y rapidez, evitando depender de red para el flujo principal.

**Motivo:** el objetivo es una herramienta temporal rápida, no almacenamiento remoto permanente.

---

## DEC-004 — Almacenamiento temporal

**Decisión:** OclAx no se diseña como archivo permanente.

**Objetivo:** guardar → usar/insertar → limpiar.

La política exacta de retención será configurable o automática, pero evitar acumulación innecesaria es requisito del producto.

---

## DEC-005 — Separación de responsabilidades

**Decisión:** UI, personalización, lógica, datos y plataforma permanecen desacoplados en lo razonable.

**Motivo:** permitir cambios de diseño sin romper el resto del proyecto.

---

## DEC-006 — Handshake de sesión

**Decisión:** cada chat nuevo debe sincronizarse y obtener un número persistente antes de trabajar.

Formato:

`Ing. OclAx📲 #[NÚMERO]`

Sin handshake válido, la sesión no está autorizada para modificar el proyecto.


---

## DEC-007 — Revisores separados de AGENTS.md

**Decisión:** AGENTS.md define cuándo revisar y `docs/REVIEW_ROLES.md` contiene los checklists y responsabilidades detalladas.

**Motivo:** mantener la constitución legible y permitir evolucionar revisores sin inflar el protocolo principal.

---

## DEC-008 — DESIGN.md como fuente de verdad visual

**Decisión:** cuando exista UI, `docs/DESIGN.md` conserva las decisiones visuales y de accesibilidad.

**Motivo:** separar cambios de diseño de lógica, datos e infraestructura.

**Consecuencia:** herramientas visuales externas son auxiliares; las decisiones duraderas vuelven a GitHub.

---

## DEC-009 — Registro de herramientas externas

**Decisión:** `docs/TOOLS.md` registra herramientas externas relevantes, su estado, límites, privacidad y condiciones de uso.

**Motivo:** evitar agregar herramientas por moda o convertirlas accidentalmente en una fuente de verdad paralela.
