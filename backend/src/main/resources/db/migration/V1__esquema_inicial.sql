CREATE TABLE proveedores (
    id            BIGINT IDENTITY(1,1) NOT NULL,
    razon_social  VARCHAR(150) NOT NULL,
    cuit          VARCHAR(13)  NULL,
    telefono      VARCHAR(255) NULL,
    email         VARCHAR(255) NULL,
    CONSTRAINT pk_proveedores PRIMARY KEY (id)
);

-- Índice filtrado: UNIQUE común en SQL Server admite un solo NULL.
CREATE UNIQUE INDEX uq_proveedores_cuit ON proveedores (cuit) WHERE cuit IS NOT NULL;

CREATE TABLE productos (
    id            BIGINT IDENTITY(1,1) NOT NULL,
    codigo        VARCHAR(50)   NOT NULL,
    descripcion   VARCHAR(200)  NOT NULL,
    precio        NUMERIC(12,2) NULL,
    stock_actual  INT           NOT NULL CONSTRAINT df_productos_stock_actual DEFAULT 0,
    stock_minimo  INT           NOT NULL CONSTRAINT df_productos_stock_minimo DEFAULT 0,
    proveedor_id  BIGINT        NULL,
    CONSTRAINT pk_productos PRIMARY KEY (id),
    CONSTRAINT uq_productos_codigo UNIQUE (codigo),
    CONSTRAINT ck_productos_stock_actual CHECK (stock_actual >= 0),
    CONSTRAINT ck_productos_stock_minimo CHECK (stock_minimo >= 0),
    CONSTRAINT fk_productos_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedores (id)
);

CREATE INDEX ix_productos_proveedor ON productos (proveedor_id);

CREATE TABLE movimientos_stock (
    id           BIGINT IDENTITY(1,1) NOT NULL,
    producto_id  BIGINT       NOT NULL,
    tipo         VARCHAR(20)  NOT NULL,
    cantidad     INT          NOT NULL,
    fecha        DATETIME2(6) NOT NULL CONSTRAINT df_movimientos_fecha DEFAULT SYSDATETIME(),
    observacion  VARCHAR(255) NULL,
    CONSTRAINT pk_movimientos_stock PRIMARY KEY (id),
    CONSTRAINT ck_movimientos_tipo CHECK (tipo IN ('ENTRADA', 'SALIDA', 'AJUSTE')),
    CONSTRAINT fk_movimientos_producto FOREIGN KEY (producto_id) REFERENCES productos (id)
);

CREATE INDEX ix_movimientos_producto_fecha ON movimientos_stock (producto_id, fecha DESC);

CREATE TABLE usuarios (
    id        BIGINT IDENTITY(1,1) NOT NULL,
    username  VARCHAR(50)  NOT NULL,
    nombre    VARCHAR(100) NOT NULL,
    rol       VARCHAR(30)  NOT NULL,
    activo    BIT          NOT NULL CONSTRAINT df_usuarios_activo DEFAULT 1,
    CONSTRAINT pk_usuarios PRIMARY KEY (id),
    CONSTRAINT uq_usuarios_username UNIQUE (username),
    CONSTRAINT ck_usuarios_rol CHECK (rol IN ('ADMINISTRADOR', 'ENCARGADO_DEPOSITO', 'VENDEDOR'))
);
