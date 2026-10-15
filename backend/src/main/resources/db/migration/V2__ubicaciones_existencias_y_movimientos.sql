-- Modelo completo de la Entrega 1: ubicaciones, categorías, producto completo,
-- stock por ubicación, producto ↔ proveedor N:M, contraseña de usuario y los 6 tipos de movimiento.
-- Los datos existentes se migran: el stock pasa al Depósito y el proveedor a la tabla N:M como preferido.

CREATE TABLE ubicaciones (
    id      BIGINT IDENTITY(1,1) NOT NULL,
    nombre  VARCHAR(50) NOT NULL,
    CONSTRAINT pk_ubicaciones PRIMARY KEY (id),
    CONSTRAINT uq_ubicaciones_nombre UNIQUE (nombre)
);

INSERT INTO ubicaciones (nombre) VALUES ('Sucursal'), ('Depósito');

CREATE TABLE categorias (
    id      BIGINT IDENTITY(1,1) NOT NULL,
    nombre  VARCHAR(80) NOT NULL,
    CONSTRAINT pk_categorias PRIMARY KEY (id),
    CONSTRAINT uq_categorias_nombre UNIQUE (nombre)
);

INSERT INTO categorias (nombre) VALUES ('General');

ALTER TABLE productos ADD
    nombre                  VARCHAR(120) NULL,
    categoria_id            BIGINT       NULL,
    unidad_medida           VARCHAR(20)  NOT NULL CONSTRAINT df_productos_unidad_medida DEFAULT 'UNIDAD',
    punto_pedido            INT          NOT NULL CONSTRAINT df_productos_punto_pedido DEFAULT 0,
    activo                  BIT          NOT NULL CONSTRAINT df_productos_activo DEFAULT 1,
    proveedor_preferido_id  BIGINT       NULL;
GO

UPDATE productos
SET nombre = LEFT(descripcion, 120),
    categoria_id = (SELECT id FROM categorias WHERE nombre = 'General'),
    punto_pedido = stock_minimo,
    proveedor_preferido_id = proveedor_id;

ALTER TABLE productos ALTER COLUMN nombre VARCHAR(120) NOT NULL;
ALTER TABLE productos ALTER COLUMN categoria_id BIGINT NOT NULL;
ALTER TABLE productos ALTER COLUMN descripcion VARCHAR(200) NULL;
ALTER TABLE productos ADD
    CONSTRAINT fk_productos_categoria FOREIGN KEY (categoria_id) REFERENCES categorias (id),
    CONSTRAINT fk_productos_proveedor_preferido FOREIGN KEY (proveedor_preferido_id) REFERENCES proveedores (id),
    CONSTRAINT ck_productos_punto_pedido CHECK (punto_pedido >= stock_minimo),
    CONSTRAINT ck_productos_unidad_medida
        CHECK (unidad_medida IN ('UNIDAD', 'METRO', 'KILOGRAMO', 'LITRO', 'CAJA', 'ROLLO', 'PAQUETE'));

CREATE INDEX ix_productos_categoria ON productos (categoria_id);

CREATE TABLE producto_proveedor (
    producto_id   BIGINT NOT NULL,
    proveedor_id  BIGINT NOT NULL,
    CONSTRAINT pk_producto_proveedor PRIMARY KEY (producto_id, proveedor_id),
    CONSTRAINT fk_producto_proveedor_producto FOREIGN KEY (producto_id) REFERENCES productos (id),
    CONSTRAINT fk_producto_proveedor_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedores (id)
);

CREATE INDEX ix_producto_proveedor_proveedor ON producto_proveedor (proveedor_id);

INSERT INTO producto_proveedor (producto_id, proveedor_id)
SELECT id, proveedor_id FROM productos WHERE proveedor_id IS NOT NULL;

-- Stock por producto y ubicación. version = bloqueo optimista (@Version) contra salidas simultáneas.
CREATE TABLE existencias (
    id           BIGINT IDENTITY(1,1) NOT NULL,
    producto_id  BIGINT NOT NULL,
    ubicacion_id BIGINT NOT NULL,
    cantidad     INT    NOT NULL CONSTRAINT df_existencias_cantidad DEFAULT 0,
    version      BIGINT NOT NULL CONSTRAINT df_existencias_version DEFAULT 0,
    CONSTRAINT pk_existencias PRIMARY KEY (id),
    CONSTRAINT uq_existencias_producto_ubicacion UNIQUE (producto_id, ubicacion_id),
    CONSTRAINT ck_existencias_cantidad CHECK (cantidad >= 0),
    CONSTRAINT fk_existencias_producto FOREIGN KEY (producto_id) REFERENCES productos (id),
    CONSTRAINT fk_existencias_ubicacion FOREIGN KEY (ubicacion_id) REFERENCES ubicaciones (id)
);

INSERT INTO existencias (producto_id, ubicacion_id, cantidad)
SELECT id, (SELECT id FROM ubicaciones WHERE nombre = 'Depósito'), stock_actual
FROM productos WHERE stock_actual > 0;

DROP INDEX ix_productos_proveedor ON productos;
ALTER TABLE productos DROP CONSTRAINT fk_productos_proveedor, ck_productos_stock_actual, df_productos_stock_actual;
ALTER TABLE productos DROP COLUMN proveedor_id, stock_actual;

-- NULL = usuario creado antes del login: no puede ingresar hasta que un administrador le asigne contraseña.
ALTER TABLE usuarios ADD password_hash VARCHAR(100) NULL;

ALTER TABLE movimientos_stock DROP CONSTRAINT ck_movimientos_tipo;
ALTER TABLE movimientos_stock ADD
    ubicacion_id          BIGINT       NULL,
    ubicacion_destino_id  BIGINT       NULL,
    usuario_id            BIGINT       NULL,
    motivo                VARCHAR(255) NULL;
GO

-- Movimientos previos a esta migración: se asignan al Depósito y a un usuario técnico inactivo.
IF EXISTS (SELECT 1 FROM movimientos_stock)
    AND NOT EXISTS (SELECT 1 FROM usuarios WHERE username = 'sistema')
    INSERT INTO usuarios (username, nombre, rol, activo)
    VALUES ('sistema', 'Sistema (movimientos previos al login)', 'ADMINISTRADOR', 0);

UPDATE movimientos_stock
SET tipo = CASE tipo WHEN 'ENTRADA' THEN 'INGRESO_COMPRA' WHEN 'SALIDA' THEN 'SALIDA_VENTA' ELSE tipo END,
    ubicacion_id = (SELECT id FROM ubicaciones WHERE nombre = 'Depósito'),
    usuario_id = (SELECT id FROM usuarios WHERE username = 'sistema'),
    motivo = CASE WHEN tipo = 'AJUSTE' THEN 'Ajuste previo a la migración' END;

ALTER TABLE movimientos_stock ALTER COLUMN ubicacion_id BIGINT NOT NULL;
ALTER TABLE movimientos_stock ALTER COLUMN usuario_id BIGINT NOT NULL;
ALTER TABLE movimientos_stock ADD
    CONSTRAINT fk_movimientos_ubicacion FOREIGN KEY (ubicacion_id) REFERENCES ubicaciones (id),
    CONSTRAINT fk_movimientos_ubicacion_destino FOREIGN KEY (ubicacion_destino_id) REFERENCES ubicaciones (id),
    CONSTRAINT fk_movimientos_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT ck_movimientos_tipo CHECK (tipo IN
        ('INGRESO_COMPRA', 'SALIDA_VENTA', 'DEVOLUCION', 'ROTURA_PERDIDA', 'AJUSTE', 'TRASLADO')),
    -- V1 aceptaba AJUSTE en 0; la aplicación ya no, pero la migración no debe fallar por esas filas
    CONSTRAINT ck_movimientos_cantidad CHECK (cantidad > 0 OR tipo = 'AJUSTE'),
    CONSTRAINT ck_movimientos_traslado CHECK (
        (tipo = 'TRASLADO' AND ubicacion_destino_id IS NOT NULL AND ubicacion_destino_id <> ubicacion_id)
        OR (tipo <> 'TRASLADO' AND ubicacion_destino_id IS NULL)),
    CONSTRAINT ck_movimientos_motivo_ajuste CHECK (tipo <> 'AJUSTE' OR motivo IS NOT NULL);

CREATE INDEX ix_movimientos_fecha ON movimientos_stock (fecha DESC);
CREATE INDEX ix_movimientos_usuario ON movimientos_stock (usuario_id);
