/*
 * V3 — Quita las solicitudes de reposición, que quedaron fuera del alcance del TP.
 *
 * La V2 había creado las tablas solicitudes_reposicion y solicitud_items, las columnas
 * solicitud_id de alertas y movimientos y el estado EN_GESTION de las alertas (una alerta
 * con una solicitud en curso). Nada de eso se usa: la reposición se registra como un
 * ingreso por compra, que resuelve la alerta. Las tablas estaban vacías.
 */

-- ─────────────────────── 1. Referencias desde alertas y movimientos ───────────────────────

ALTER TABLE movimientos_stock DROP CONSTRAINT fk_movimientos_solicitud;
ALTER TABLE movimientos_stock DROP COLUMN solicitud_id;

ALTER TABLE alertas_stock DROP CONSTRAINT fk_alertas_solicitud;
ALTER TABLE alertas_stock DROP COLUMN solicitud_id;
GO

-- ─────────────────────────── 2. Tablas de solicitudes ───────────────────────────

DROP TABLE solicitud_items;
DROP TABLE solicitudes_reposicion;
GO

-- ──────────────────── 3. Estados de alerta: sin EN_GESTION ────────────────────

-- Por si alguna alerta hubiera quedado en ese estado, sigue activa como ABIERTA
UPDATE alertas_stock SET estado = 'ABIERTA' WHERE estado = 'EN_GESTION';

ALTER TABLE alertas_stock DROP CONSTRAINT ck_alertas_estado;
ALTER TABLE alertas_stock ADD CONSTRAINT ck_alertas_estado CHECK (estado IN ('ABIERTA', 'RESUELTA'));

-- Una sola alerta activa (ABIERTA) por producto y sucursal
DROP INDEX uq_alertas_activa ON alertas_stock;
CREATE UNIQUE INDEX uq_alertas_activa ON alertas_stock (producto_id, sucursal_id)
    WHERE estado = 'ABIERTA';
GO
