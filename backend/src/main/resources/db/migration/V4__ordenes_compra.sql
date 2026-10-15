CREATE TABLE ordenes_compra (
    id                      BIGINT IDENTITY(1,1) NOT NULL,
    proveedor_id            BIGINT       NOT NULL,
    estado                  VARCHAR(20)  NOT NULL,
    usuario_id              BIGINT       NOT NULL,
    fecha_creacion          DATETIME2(6) NOT NULL,
    fecha_recepcion         DATETIME2(6) NULL,
    ubicacion_recepcion_id  BIGINT       NULL,
    observacion             VARCHAR(255) NULL,
    CONSTRAINT pk_ordenes_compra PRIMARY KEY (id),
    CONSTRAINT fk_ordenes_compra_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedores (id),
    CONSTRAINT fk_ordenes_compra_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT fk_ordenes_compra_ubicacion FOREIGN KEY (ubicacion_recepcion_id) REFERENCES ubicaciones (id),
    CONSTRAINT ck_ordenes_compra_estado CHECK (estado IN ('BORRADOR', 'ENVIADA', 'RECIBIDA', 'CANCELADA'))
);

CREATE INDEX ix_ordenes_compra_estado ON ordenes_compra (estado, fecha_creacion DESC);

CREATE TABLE ordenes_compra_items (
    id           BIGINT IDENTITY(1,1) NOT NULL,
    orden_id     BIGINT NOT NULL,
    producto_id  BIGINT NOT NULL,
    cantidad     INT    NOT NULL,
    CONSTRAINT pk_ordenes_compra_items PRIMARY KEY (id),
    CONSTRAINT uq_ordenes_compra_items UNIQUE (orden_id, producto_id),
    CONSTRAINT ck_ordenes_compra_items_cantidad CHECK (cantidad > 0),
    CONSTRAINT fk_ordenes_compra_items_orden FOREIGN KEY (orden_id) REFERENCES ordenes_compra (id),
    CONSTRAINT fk_ordenes_compra_items_producto FOREIGN KEY (producto_id) REFERENCES productos (id)
);
