-- Alertas de reposición (Observer + Decorator). A lo sumo una alerta abierta por producto:
-- se resuelve sola cuando el stock vuelve a superar el punto de pedido.
CREATE TABLE alertas_stock (
    id           BIGINT IDENTITY(1,1) NOT NULL,
    producto_id  BIGINT       NOT NULL,
    estado       VARCHAR(20)  NOT NULL,
    stock        INT          NOT NULL,
    fecha        DATETIME2(6) NOT NULL,
    resuelta_en  DATETIME2(6) NULL,
    CONSTRAINT pk_alertas_stock PRIMARY KEY (id),
    CONSTRAINT fk_alertas_producto FOREIGN KEY (producto_id) REFERENCES productos (id),
    CONSTRAINT ck_alertas_estado CHECK (estado IN ('BAJO_MINIMO', 'PUNTO_PEDIDO'))
);

CREATE UNIQUE INDEX uq_alertas_abierta_por_producto ON alertas_stock (producto_id) WHERE resuelta_en IS NULL;
CREATE INDEX ix_alertas_fecha ON alertas_stock (fecha DESC);
