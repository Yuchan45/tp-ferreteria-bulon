/*
 * V2 — Stock por sucursal y reposición a proveedores (modelo en db/dbdiagram/v2.txt).
 *
 * - El stock pasa de productos a stock_sucursal (una fila por producto y sucursal).
 * - Un producto puede tener varios proveedores (producto_proveedor, con un preferido).
 * - Roles ADMINISTRADOR / EMPLEADO; el empleado pertenece a una sucursal.
 * - Movimientos con sucursal, usuario responsable, proveedor y seis tipos.
 * - Tablas para alertas, solicitudes de reposición y notificaciones.
 *
 * Migra los datos existentes: crea la sucursal SUC-01, le asigna todo el stock,
 * pasa el proveedor de cada producto a preferido y adapta roles y movimientos.
 * Los GO separan lotes: SQL Server compila cada lote antes de ejecutarlo, así que una
 * columna agregada no puede usarse en el mismo lote. Flyway aplica todo en una
 * transacción: si algo falla, no queda nada a medias.
 */

-- ─────────────────────────── 1. Sucursales ───────────────────────────

CREATE TABLE sucursales (
    id         BIGINT IDENTITY(1,1) NOT NULL,
    codigo     VARCHAR(20)  NOT NULL,
    nombre     VARCHAR(100) NOT NULL,
    direccion  VARCHAR(200) NULL,
    telefono   VARCHAR(30)  NULL,
    activa     BIT          NOT NULL CONSTRAINT df_sucursales_activa DEFAULT 1,
    CONSTRAINT pk_sucursales PRIMARY KEY (id),
    CONSTRAINT uq_sucursales_codigo UNIQUE (codigo)
);

-- Hoy Bulón opera una sola sucursal: todo el stock existente pasa a ella
INSERT INTO sucursales (codigo, nombre) VALUES ('SUC-01', 'Sucursal Central');
GO

-- ─────────────────────────── 2. Usuarios ───────────────────────────

ALTER TABLE usuarios DROP CONSTRAINT ck_usuarios_rol;
GO

-- Los dos roles operativos de v1 pasan a EMPLEADO
UPDATE usuarios SET rol = 'EMPLEADO' WHERE rol IN ('ENCARGADO_DEPOSITO', 'VENDEDOR');
ALTER TABLE usuarios ALTER COLUMN rol VARCHAR(20) NOT NULL;

ALTER TABLE usuarios ADD
    email         VARCHAR(150) NULL,
    password_hash VARCHAR(100) NULL,
    sucursal_id   BIGINT       NULL,
    creado        DATETIME2(6) NOT NULL CONSTRAINT df_usuarios_creado DEFAULT SYSDATETIME();
GO

-- Contraseña de desarrollo "bulon1234" (BCrypt) para los usuarios existentes;
-- cambiarla cuando exista el login
UPDATE usuarios SET password_hash = '$2a$10$099UwDUOiLDUTTXeie7BzOspu313D3ftVe2PjkfjpJq1nK1BSQVbe';
UPDATE usuarios SET sucursal_id = (SELECT id FROM sucursales WHERE codigo = 'SUC-01') WHERE rol = 'EMPLEADO';
ALTER TABLE usuarios ALTER COLUMN password_hash VARCHAR(100) NOT NULL;

ALTER TABLE usuarios ADD
    CONSTRAINT ck_usuarios_rol CHECK (rol IN ('ADMINISTRADOR', 'EMPLEADO')),
    -- El empleado pertenece a una sucursal; el administrador ve todas
    CONSTRAINT ck_usuarios_sucursal CHECK (
        (rol = 'EMPLEADO' AND sucursal_id IS NOT NULL) OR (rol = 'ADMINISTRADOR' AND sucursal_id IS NULL)),
    CONSTRAINT fk_usuarios_sucursal FOREIGN KEY (sucursal_id) REFERENCES sucursales (id);

CREATE UNIQUE INDEX uq_usuarios_email ON usuarios (email) WHERE email IS NOT NULL;
CREATE INDEX ix_usuarios_sucursal ON usuarios (sucursal_id);
GO

-- ─────────────────────────── 3. Catálogo ───────────────────────────

CREATE TABLE categorias (
    id     BIGINT IDENTITY(1,1) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    CONSTRAINT pk_categorias PRIMARY KEY (id),
    CONSTRAINT uq_categorias_nombre UNIQUE (nombre)
);

ALTER TABLE productos ADD
    nombre          VARCHAR(100) NULL,
    categoria_id    BIGINT       NULL,
    unidad_medida   VARCHAR(20)  NOT NULL CONSTRAINT df_productos_unidad_medida DEFAULT 'UNIDAD',
    activo          BIT          NOT NULL CONSTRAINT df_productos_activo DEFAULT 1,
    actualizado_por BIGINT       NULL,
    actualizado_en  DATETIME2(6) NULL;

ALTER TABLE proveedores ALTER COLUMN telefono VARCHAR(30) NULL;
ALTER TABLE proveedores ALTER COLUMN email VARCHAR(150) NULL;
ALTER TABLE proveedores ADD
    contacto        VARCHAR(100) NULL,
    activo          BIT          NOT NULL CONSTRAINT df_proveedores_activo DEFAULT 1,
    actualizado_por BIGINT       NULL,
    actualizado_en  DATETIME2(6) NULL;
GO

-- En v1 la descripción hacía de nombre: se copia (máx. 100 caracteres)
UPDATE productos SET nombre = LEFT(descripcion, 100);
ALTER TABLE productos ALTER COLUMN nombre VARCHAR(100) NOT NULL;
ALTER TABLE productos ALTER COLUMN descripcion VARCHAR(500) NULL;

ALTER TABLE productos ADD
    CONSTRAINT fk_productos_categoria FOREIGN KEY (categoria_id) REFERENCES categorias (id),
    CONSTRAINT fk_productos_actualizado_por FOREIGN KEY (actualizado_por) REFERENCES usuarios (id),
    CONSTRAINT ck_productos_unidad_medida CHECK (unidad_medida IN ('UNIDAD', 'METRO', 'KILOGRAMO', 'LITRO', 'CAJA', 'ROLLO')),
    CONSTRAINT ck_productos_precio CHECK (precio IS NULL OR precio >= 0);
CREATE INDEX ix_productos_categoria ON productos (categoria_id);

ALTER TABLE proveedores ADD
    CONSTRAINT fk_proveedores_actualizado_por FOREIGN KEY (actualizado_por) REFERENCES usuarios (id);

CREATE TABLE producto_proveedor (
    producto_id        BIGINT        NOT NULL,
    proveedor_id       BIGINT        NOT NULL,
    preferido          BIT           NOT NULL CONSTRAINT df_producto_proveedor_preferido DEFAULT 0,
    codigo_proveedor   VARCHAR(50)   NULL,
    precio_costo       NUMERIC(12,2) NULL,
    plazo_entrega_dias INT           NULL,
    CONSTRAINT pk_producto_proveedor PRIMARY KEY (producto_id, proveedor_id),
    CONSTRAINT fk_producto_proveedor_producto FOREIGN KEY (producto_id) REFERENCES productos (id),
    CONSTRAINT fk_producto_proveedor_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedores (id),
    CONSTRAINT ck_producto_proveedor_costo CHECK (precio_costo IS NULL OR precio_costo >= 0),
    CONSTRAINT ck_producto_proveedor_plazo CHECK (plazo_entrega_dias IS NULL OR plazo_entrega_dias >= 0)
);
-- Un solo proveedor preferido por producto
CREATE UNIQUE INDEX uq_producto_proveedor_preferido ON producto_proveedor (producto_id) WHERE preferido = 1;
CREATE INDEX ix_producto_proveedor_proveedor ON producto_proveedor (proveedor_id);

-- El único proveedor de v1 pasa a ser el preferido
INSERT INTO producto_proveedor (producto_id, proveedor_id, preferido)
SELECT id, proveedor_id, 1 FROM productos WHERE proveedor_id IS NOT NULL;
GO

-- ─────────────────────────── 4. Stock por sucursal ───────────────────────────

-- NUMERIC(12,3): permite vender por metro, kilo o litro (equivale a DECIMAL en SQL Server)
CREATE TABLE stock_sucursal (
    producto_id     BIGINT        NOT NULL,
    sucursal_id     BIGINT        NOT NULL,
    cantidad        NUMERIC(12,3) NOT NULL CONSTRAINT df_stock_sucursal_cantidad DEFAULT 0,
    stock_minimo    NUMERIC(12,3) NOT NULL CONSTRAINT df_stock_sucursal_minimo DEFAULT 0,
    punto_pedido    NUMERIC(12,3) NOT NULL CONSTRAINT df_stock_sucursal_punto_pedido DEFAULT 0,
    version         INT           NOT NULL CONSTRAINT df_stock_sucursal_version DEFAULT 0,
    actualizado_por BIGINT        NULL,
    actualizado_en  DATETIME2(6)  NULL,
    CONSTRAINT pk_stock_sucursal PRIMARY KEY (producto_id, sucursal_id),
    CONSTRAINT fk_stock_sucursal_producto FOREIGN KEY (producto_id) REFERENCES productos (id),
    CONSTRAINT fk_stock_sucursal_sucursal FOREIGN KEY (sucursal_id) REFERENCES sucursales (id),
    CONSTRAINT fk_stock_sucursal_actualizado_por FOREIGN KEY (actualizado_por) REFERENCES usuarios (id),
    CONSTRAINT ck_stock_sucursal_cantidad CHECK (cantidad >= 0),
    CONSTRAINT ck_stock_sucursal_minimo CHECK (stock_minimo >= 0),
    CONSTRAINT ck_stock_sucursal_punto_pedido CHECK (punto_pedido >= stock_minimo)
);
CREATE INDEX ix_stock_sucursal_sucursal ON stock_sucursal (sucursal_id);

-- v1 tenía un solo umbral: queda como mínimo y como punto de pedido (ajustable después)
INSERT INTO stock_sucursal (producto_id, sucursal_id, cantidad, stock_minimo, punto_pedido)
SELECT p.id, s.id, p.stock_actual, p.stock_minimo, p.stock_minimo
FROM productos p
CROSS JOIN sucursales s;
GO

-- El stock y el proveedor ya no viven en productos
ALTER TABLE productos DROP CONSTRAINT
    fk_productos_proveedor,
    ck_productos_stock_actual,
    ck_productos_stock_minimo,
    df_productos_stock_actual,
    df_productos_stock_minimo;
DROP INDEX ix_productos_proveedor ON productos;
ALTER TABLE productos DROP COLUMN stock_actual, stock_minimo, proveedor_id;
GO

-- ─────────────────────────── 5. Reposición ───────────────────────────

CREATE TABLE solicitudes_reposicion (
    id           BIGINT IDENTITY(1,1) NOT NULL,
    sucursal_id  BIGINT       NOT NULL,
    proveedor_id BIGINT       NOT NULL,
    usuario_id   BIGINT       NOT NULL,
    estado       VARCHAR(15)  NOT NULL CONSTRAINT df_solicitudes_estado DEFAULT 'PENDIENTE',
    creada       DATETIME2(6) NOT NULL CONSTRAINT df_solicitudes_creada DEFAULT SYSDATETIME(),
    enviada      DATETIME2(6) NULL,
    recibida     DATETIME2(6) NULL,
    observacion  VARCHAR(255) NULL,
    CONSTRAINT pk_solicitudes_reposicion PRIMARY KEY (id),
    CONSTRAINT fk_solicitudes_sucursal FOREIGN KEY (sucursal_id) REFERENCES sucursales (id),
    CONSTRAINT fk_solicitudes_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedores (id),
    CONSTRAINT fk_solicitudes_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT ck_solicitudes_estado CHECK (estado IN ('PENDIENTE', 'ENVIADA', 'RECIBIDA', 'CANCELADA'))
);
CREATE INDEX ix_solicitudes_sucursal_estado ON solicitudes_reposicion (sucursal_id, estado);
CREATE INDEX ix_solicitudes_proveedor ON solicitudes_reposicion (proveedor_id);

CREATE TABLE solicitud_items (
    solicitud_id        BIGINT        NOT NULL,
    producto_id         BIGINT        NOT NULL,
    cantidad_solicitada NUMERIC(12,3) NOT NULL,
    cantidad_recibida   NUMERIC(12,3) NULL,
    CONSTRAINT pk_solicitud_items PRIMARY KEY (solicitud_id, producto_id),
    CONSTRAINT fk_solicitud_items_solicitud FOREIGN KEY (solicitud_id) REFERENCES solicitudes_reposicion (id),
    CONSTRAINT fk_solicitud_items_producto FOREIGN KEY (producto_id) REFERENCES productos (id),
    CONSTRAINT ck_solicitud_items_solicitada CHECK (cantidad_solicitada > 0),
    CONSTRAINT ck_solicitud_items_recibida CHECK (cantidad_recibida IS NULL OR cantidad_recibida >= 0)
);
GO

-- ─────────────────────────── 6. Movimientos ───────────────────────────

ALTER TABLE movimientos_stock DROP CONSTRAINT ck_movimientos_tipo;
DROP INDEX ix_movimientos_producto_fecha ON movimientos_stock;
EXEC sp_rename 'movimientos_stock.observacion', 'observaciones', 'COLUMN';
GO

ALTER TABLE movimientos_stock ALTER COLUMN tipo VARCHAR(25) NOT NULL;
ALTER TABLE movimientos_stock ALTER COLUMN cantidad NUMERIC(12,3) NOT NULL;
ALTER TABLE movimientos_stock ALTER COLUMN observaciones VARCHAR(500) NULL;
ALTER TABLE movimientos_stock ADD
    sucursal_id  BIGINT       NULL,
    usuario_id   BIGINT       NULL,
    motivo       VARCHAR(255) NULL,
    referencia   VARCHAR(50)  NULL,
    proveedor_id BIGINT       NULL,
    solicitud_id BIGINT       NULL;
GO

-- Los movimientos de v1 no guardaban responsable: se asignan a un administrador.
-- Si hubiera movimientos pero ningún administrador, se crea uno inactivo para eso.
IF EXISTS (SELECT 1 FROM movimientos_stock)
   AND NOT EXISTS (SELECT 1 FROM usuarios WHERE rol = 'ADMINISTRADOR')
    INSERT INTO usuarios (username, nombre, rol, password_hash, activo)
    VALUES ('migracion', 'Usuario de migración', 'ADMINISTRADOR',
            '$2a$10$099UwDUOiLDUTTXeie7BzOspu313D3ftVe2PjkfjpJq1nK1BSQVbe', 0);

UPDATE movimientos_stock SET
    sucursal_id = (SELECT id FROM sucursales WHERE codigo = 'SUC-01'),
    usuario_id  = (SELECT MIN(id) FROM usuarios WHERE rol = 'ADMINISTRADOR'),
    tipo = CASE tipo WHEN 'ENTRADA' THEN 'INGRESO_COMPRA'
                     WHEN 'SALIDA'  THEN 'SALIDA_VENTA'
                     ELSE tipo END;

-- Las entradas de v1 pasan a ser compras al proveedor preferido del producto
UPDATE m SET proveedor_id = pp.proveedor_id
FROM movimientos_stock m
JOIN producto_proveedor pp ON pp.producto_id = m.producto_id AND pp.preferido = 1
WHERE m.tipo = 'INGRESO_COMPRA';

-- En v2 el ajuste exige motivo: se toma la observación que tenía
UPDATE movimientos_stock
SET motivo = ISNULL(NULLIF(LTRIM(RTRIM(observaciones)), ''), 'Ajuste migrado desde v1')
WHERE tipo = 'AJUSTE';

ALTER TABLE movimientos_stock ALTER COLUMN sucursal_id BIGINT NOT NULL;
ALTER TABLE movimientos_stock ALTER COLUMN usuario_id BIGINT NOT NULL;

ALTER TABLE movimientos_stock ADD
    CONSTRAINT fk_movimientos_sucursal FOREIGN KEY (sucursal_id) REFERENCES sucursales (id),
    CONSTRAINT fk_movimientos_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT fk_movimientos_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedores (id),
    CONSTRAINT fk_movimientos_solicitud FOREIGN KEY (solicitud_id) REFERENCES solicitudes_reposicion (id),
    CONSTRAINT ck_movimientos_tipo CHECK (tipo IN ('INGRESO_COMPRA', 'SALIDA_VENTA', 'DEVOLUCION_CLIENTE',
                                                   'DEVOLUCION_PROVEEDOR', 'ROTURA', 'AJUSTE')),
    -- Positiva salvo en el ajuste, que lleva signo y no puede ser 0
    CONSTRAINT ck_movimientos_cantidad CHECK (
        (tipo = 'AJUSTE' AND cantidad <> 0) OR (tipo <> 'AJUSTE' AND cantidad > 0));

CREATE INDEX ix_movimientos_producto_sucursal_fecha ON movimientos_stock (producto_id, sucursal_id, fecha DESC);
CREATE INDEX ix_movimientos_sucursal_fecha ON movimientos_stock (sucursal_id, fecha DESC);
CREATE INDEX ix_movimientos_usuario ON movimientos_stock (usuario_id);
GO

-- ─────────────────────────── 7. Alertas y notificaciones ───────────────────────────

CREATE TABLE alertas_stock (
    id                  BIGINT IDENTITY(1,1) NOT NULL,
    producto_id         BIGINT        NOT NULL,
    sucursal_id         BIGINT        NOT NULL,
    nivel               VARCHAR(10)   NOT NULL,
    estado              VARCHAR(15)   NOT NULL CONSTRAINT df_alertas_estado DEFAULT 'ABIERTA',
    cantidad_al_generar NUMERIC(12,3) NOT NULL,
    punto_pedido        NUMERIC(12,3) NOT NULL,
    stock_minimo        NUMERIC(12,3) NOT NULL,
    solicitud_id        BIGINT        NULL,
    creada              DATETIME2(6)  NOT NULL CONSTRAINT df_alertas_creada DEFAULT SYSDATETIME(),
    resuelta            DATETIME2(6)  NULL,
    CONSTRAINT pk_alertas_stock PRIMARY KEY (id),
    CONSTRAINT fk_alertas_producto FOREIGN KEY (producto_id) REFERENCES productos (id),
    CONSTRAINT fk_alertas_sucursal FOREIGN KEY (sucursal_id) REFERENCES sucursales (id),
    CONSTRAINT fk_alertas_solicitud FOREIGN KEY (solicitud_id) REFERENCES solicitudes_reposicion (id),
    CONSTRAINT ck_alertas_nivel CHECK (nivel IN ('REPONER', 'CRITICO')),
    CONSTRAINT ck_alertas_estado CHECK (estado IN ('ABIERTA', 'EN_GESTION', 'RESUELTA'))
);
-- Una sola alerta activa por producto y sucursal
CREATE UNIQUE INDEX uq_alertas_activa ON alertas_stock (producto_id, sucursal_id)
    WHERE estado IN ('ABIERTA', 'EN_GESTION');
CREATE INDEX ix_alertas_sucursal_estado ON alertas_stock (sucursal_id, estado);

CREATE TABLE notificaciones (
    id         BIGINT IDENTITY(1,1) NOT NULL,
    usuario_id BIGINT       NOT NULL,
    alerta_id  BIGINT       NULL,
    titulo     VARCHAR(150) NOT NULL,
    mensaje    VARCHAR(500) NOT NULL,
    leida      BIT          NOT NULL CONSTRAINT df_notificaciones_leida DEFAULT 0,
    creada     DATETIME2(6) NOT NULL CONSTRAINT df_notificaciones_creada DEFAULT SYSDATETIME(),
    CONSTRAINT pk_notificaciones PRIMARY KEY (id),
    CONSTRAINT fk_notificaciones_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT fk_notificaciones_alerta FOREIGN KEY (alerta_id) REFERENCES alertas_stock (id)
);
CREATE INDEX ix_notificaciones_usuario ON notificaciones (usuario_id, leida, creada DESC);
GO
