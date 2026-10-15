/* =============================================================================
   Bulón S.R.L. — Consultas básicas sobre la base bulon_stock (SQL Server)

   Uso: ejecutar cada bloque por separado (están delimitados por GO) desde
   Azure Data Studio, SSMS, DBeaver o sqlcmd (con sqlcmd agregar la opción -I).

   Este script NO lo ejecuta Flyway: el esquema se define en db/migration.

   IMPORTANTE: la forma normal de registrar movimientos es la aplicación
   (API del backend), que además valida permisos, abre/resuelve las alertas de
   reposición y notifica. Los bloques de la sección 2 son para el curso y para
   inspeccionar la base: muestran qué hace el backend a nivel SQL, pero NO
   tocan alertas_stock, por lo que las alertas pueden quedar desactualizadas.
   ============================================================================= */

USE bulon_stock;
GO

-- Requerido por el índice filtrado de proveedores.cuit y alertas_stock
-- (sqlcmd lo trae en OFF)
SET QUOTED_IDENTIFIER ON;
GO


/* =============================================================================
   1. DATOS DE EJEMPLO
   Los carga el seeder del backend (com.bulon.stock.seed.DataSeeder) al iniciar
   con SEED_ENABLED=true: 3 usuarios (admin, deposito, ventas), 5 proveedores,
   25 productos con su stock inicial repartido entre Sucursal y Depósito y sus
   movimientos. Las consultas de este archivo usan esos datos.
   ============================================================================= */


/* =============================================================================
   2. MOVIMIENTOS DE STOCK
   Igual que el backend: el movimiento y la actualización de existencias van en
   la misma transacción. El stock vive en existencias (producto + ubicación).
     INGRESO_COMPRA, DEVOLUCION  suman en la ubicación
     SALIDA_VENTA, ROTURA_PERDIDA restan en la ubicación
     AJUSTE                      suma o resta según signo (motivo obligatorio)
     TRASLADO                    resta en el origen y suma en el destino
   Cada movimiento guarda la ubicación y el usuario que lo registró.
   Puede no existir la fila de existencias para ese (producto, ubicación):
   se crea en cero dentro de la misma transacción antes de actualizarla.
   ============================================================================= */

-- 2.1 Registrar un INGRESO_COMPRA (recepción de mercadería en el Depósito)
SET XACT_ABORT ON;
DECLARE @codigo    VARCHAR(50) = 'BUL-001',
        @ubicacion VARCHAR(50) = 'Depósito',
        @usuario   VARCHAR(50) = 'deposito',
        @cantidad  INT = 250;
DECLARE @producto_id  BIGINT = (SELECT id FROM productos WHERE codigo = @codigo),
        @ubicacion_id BIGINT = (SELECT id FROM ubicaciones WHERE nombre = @ubicacion),
        @usuario_id   BIGINT = (SELECT id FROM usuarios WHERE username = @usuario);

BEGIN TRANSACTION;
    IF NOT EXISTS (SELECT 1 FROM existencias WHERE producto_id = @producto_id AND ubicacion_id = @ubicacion_id)
        INSERT INTO existencias (producto_id, ubicacion_id, cantidad) VALUES (@producto_id, @ubicacion_id, 0);

    UPDATE existencias SET cantidad = cantidad + @cantidad, version = version + 1
    WHERE producto_id = @producto_id AND ubicacion_id = @ubicacion_id;

    INSERT INTO movimientos_stock (producto_id, tipo, cantidad, ubicacion_id, usuario_id, observacion)
    VALUES (@producto_id, 'INGRESO_COMPRA', @cantidad, @ubicacion_id, @usuario_id, 'Recepción de pedido a proveedor');
COMMIT;
GO

-- 2.2 Registrar una SALIDA_VENTA (venta en la Sucursal). Si no alcanza el stock,
--     el CHECK ck_existencias_cantidad hace fallar el UPDATE y se revierte todo.
SET XACT_ABORT ON;
DECLARE @codigo    VARCHAR(50) = 'BUL-001',
        @ubicacion VARCHAR(50) = 'Sucursal',
        @usuario   VARCHAR(50) = 'ventas',
        @cantidad  INT = 30;
DECLARE @producto_id  BIGINT = (SELECT id FROM productos WHERE codigo = @codigo),
        @ubicacion_id BIGINT = (SELECT id FROM ubicaciones WHERE nombre = @ubicacion),
        @usuario_id   BIGINT = (SELECT id FROM usuarios WHERE username = @usuario);

BEGIN TRANSACTION;
    IF NOT EXISTS (SELECT 1 FROM existencias WHERE producto_id = @producto_id AND ubicacion_id = @ubicacion_id)
        INSERT INTO existencias (producto_id, ubicacion_id, cantidad) VALUES (@producto_id, @ubicacion_id, 0);

    UPDATE existencias SET cantidad = cantidad - @cantidad, version = version + 1
    WHERE producto_id = @producto_id AND ubicacion_id = @ubicacion_id;

    INSERT INTO movimientos_stock (producto_id, tipo, cantidad, ubicacion_id, usuario_id, observacion)
    VALUES (@producto_id, 'SALIDA_VENTA', @cantidad, @ubicacion_id, @usuario_id, 'Venta mostrador');
COMMIT;
GO

-- 2.3 Registrar una DEVOLUCION (un cliente devuelve mercadería a la Sucursal).
--     PIN-005 todavía no tiene existencias: la fila se crea en la misma transacción.
SET XACT_ABORT ON;
DECLARE @codigo    VARCHAR(50) = 'PIN-005',
        @ubicacion VARCHAR(50) = 'Sucursal',
        @usuario   VARCHAR(50) = 'ventas',
        @cantidad  INT = 2;
DECLARE @producto_id  BIGINT = (SELECT id FROM productos WHERE codigo = @codigo),
        @ubicacion_id BIGINT = (SELECT id FROM ubicaciones WHERE nombre = @ubicacion),
        @usuario_id   BIGINT = (SELECT id FROM usuarios WHERE username = @usuario);

BEGIN TRANSACTION;
    IF NOT EXISTS (SELECT 1 FROM existencias WHERE producto_id = @producto_id AND ubicacion_id = @ubicacion_id)
        INSERT INTO existencias (producto_id, ubicacion_id, cantidad) VALUES (@producto_id, @ubicacion_id, 0);

    UPDATE existencias SET cantidad = cantidad + @cantidad, version = version + 1
    WHERE producto_id = @producto_id AND ubicacion_id = @ubicacion_id;

    INSERT INTO movimientos_stock (producto_id, tipo, cantidad, ubicacion_id, usuario_id, observacion)
    VALUES (@producto_id, 'DEVOLUCION', @cantidad, @ubicacion_id, @usuario_id, 'Devolución de cliente');
COMMIT;
GO

-- 2.4 Registrar una ROTURA_PERDIDA (mercadería dañada o perdida en el Depósito)
SET XACT_ABORT ON;
DECLARE @codigo    VARCHAR(50) = 'BUL-001',
        @ubicacion VARCHAR(50) = 'Depósito',
        @usuario   VARCHAR(50) = 'deposito',
        @cantidad  INT = 5;
DECLARE @producto_id  BIGINT = (SELECT id FROM productos WHERE codigo = @codigo),
        @ubicacion_id BIGINT = (SELECT id FROM ubicaciones WHERE nombre = @ubicacion),
        @usuario_id   BIGINT = (SELECT id FROM usuarios WHERE username = @usuario);

BEGIN TRANSACTION;
    IF NOT EXISTS (SELECT 1 FROM existencias WHERE producto_id = @producto_id AND ubicacion_id = @ubicacion_id)
        INSERT INTO existencias (producto_id, ubicacion_id, cantidad) VALUES (@producto_id, @ubicacion_id, 0);

    UPDATE existencias SET cantidad = cantidad - @cantidad, version = version + 1
    WHERE producto_id = @producto_id AND ubicacion_id = @ubicacion_id;

    INSERT INTO movimientos_stock (producto_id, tipo, cantidad, ubicacion_id, usuario_id, observacion)
    VALUES (@producto_id, 'ROTURA_PERDIDA', @cantidad, @ubicacion_id, @usuario_id, 'Caja dañada en el depósito');
COMMIT;
GO

-- 2.5 Registrar un AJUSTE (por ejemplo, faltante detectado en un inventario).
--     La cantidad lleva signo (nunca 0) y el motivo es obligatorio
--     (ck_movimientos_cantidad y ck_movimientos_motivo_ajuste).
SET XACT_ABORT ON;
DECLARE @codigo    VARCHAR(50) = 'BUL-001',
        @ubicacion VARCHAR(50) = 'Depósito',
        @usuario   VARCHAR(50) = 'deposito',
        @cantidad  INT = -5;   -- negativo resta, positivo suma
DECLARE @producto_id  BIGINT = (SELECT id FROM productos WHERE codigo = @codigo),
        @ubicacion_id BIGINT = (SELECT id FROM ubicaciones WHERE nombre = @ubicacion),
        @usuario_id   BIGINT = (SELECT id FROM usuarios WHERE username = @usuario);

BEGIN TRANSACTION;
    IF NOT EXISTS (SELECT 1 FROM existencias WHERE producto_id = @producto_id AND ubicacion_id = @ubicacion_id)
        INSERT INTO existencias (producto_id, ubicacion_id, cantidad) VALUES (@producto_id, @ubicacion_id, 0);

    UPDATE existencias SET cantidad = cantidad + @cantidad, version = version + 1
    WHERE producto_id = @producto_id AND ubicacion_id = @ubicacion_id;

    INSERT INTO movimientos_stock (producto_id, tipo, cantidad, ubicacion_id, usuario_id, observacion, motivo)
    VALUES (@producto_id, 'AJUSTE', @cantidad, @ubicacion_id, @usuario_id,
            'Ajuste por inventario físico', 'Faltante detectado en el inventario físico');
COMMIT;
GO

-- 2.6 Registrar un TRASLADO entre ubicaciones (Depósito -> Sucursal).
--     Resta en el origen y suma en el destino en la misma transacción. El
--     movimiento guarda el origen en ubicacion_id y el destino en
--     ubicacion_destino_id (ck_movimientos_traslado exige que sean distintos).
SET XACT_ABORT ON;
DECLARE @codigo   VARCHAR(50) = 'BUL-001',
        @origen   VARCHAR(50) = 'Depósito',
        @destino  VARCHAR(50) = 'Sucursal',
        @usuario  VARCHAR(50) = 'deposito',
        @cantidad INT = 100;
DECLARE @producto_id BIGINT = (SELECT id FROM productos WHERE codigo = @codigo),
        @origen_id   BIGINT = (SELECT id FROM ubicaciones WHERE nombre = @origen),
        @destino_id  BIGINT = (SELECT id FROM ubicaciones WHERE nombre = @destino),
        @usuario_id  BIGINT = (SELECT id FROM usuarios WHERE username = @usuario);

BEGIN TRANSACTION;
    IF NOT EXISTS (SELECT 1 FROM existencias WHERE producto_id = @producto_id AND ubicacion_id = @origen_id)
        INSERT INTO existencias (producto_id, ubicacion_id, cantidad) VALUES (@producto_id, @origen_id, 0);
    IF NOT EXISTS (SELECT 1 FROM existencias WHERE producto_id = @producto_id AND ubicacion_id = @destino_id)
        INSERT INTO existencias (producto_id, ubicacion_id, cantidad) VALUES (@producto_id, @destino_id, 0);

    -- Si el origen no alcanza, el CHECK falla y se revierte todo
    UPDATE existencias SET cantidad = cantidad - @cantidad, version = version + 1
    WHERE producto_id = @producto_id AND ubicacion_id = @origen_id;

    UPDATE existencias SET cantidad = cantidad + @cantidad, version = version + 1
    WHERE producto_id = @producto_id AND ubicacion_id = @destino_id;

    INSERT INTO movimientos_stock
        (producto_id, tipo, cantidad, ubicacion_id, ubicacion_destino_id, usuario_id, observacion)
    VALUES (@producto_id, 'TRASLADO', @cantidad, @origen_id, @destino_id, @usuario_id, 'Reposición de la Sucursal');
COMMIT;
GO

-- 2.7 Historial de movimientos de un producto (más reciente primero)
DECLARE @codigo VARCHAR(50) = 'BUL-001';

SELECT m.fecha, m.tipo, m.cantidad, u.nombre AS ubicacion, ud.nombre AS destino,
       us.username AS usuario, m.observacion, m.motivo
FROM movimientos_stock m
JOIN productos p    ON p.id = m.producto_id
JOIN ubicaciones u  ON u.id = m.ubicacion_id
LEFT JOIN ubicaciones ud ON ud.id = m.ubicacion_destino_id
JOIN usuarios us    ON us.id = m.usuario_id
WHERE p.codigo = @codigo
ORDER BY m.fecha DESC, m.id DESC;
GO

-- 2.8 Movimientos en un rango de fechas
DECLARE @desde DATETIME2 = DATEADD(DAY, -7, CAST(GETDATE() AS DATE)),
        @hasta DATETIME2 = DATEADD(DAY, 1, CAST(GETDATE() AS DATE));

SELECT m.fecha, p.codigo, p.nombre, m.tipo, m.cantidad, u.nombre AS ubicacion,
       us.username AS usuario, m.observacion
FROM movimientos_stock m
JOIN productos p   ON p.id = m.producto_id
JOIN ubicaciones u ON u.id = m.ubicacion_id
JOIN usuarios us   ON us.id = m.usuario_id
WHERE m.fecha >= @desde AND m.fecha < @hasta
ORDER BY m.fecha DESC, m.id DESC;
GO

-- 2.9 Movimientos registrados por un usuario
DECLARE @usuario VARCHAR(50) = 'deposito';

SELECT m.fecha, p.codigo, m.tipo, m.cantidad, u.nombre AS ubicacion, m.observacion
FROM movimientos_stock m
JOIN usuarios us   ON us.id = m.usuario_id
JOIN productos p   ON p.id = m.producto_id
JOIN ubicaciones u ON u.id = m.ubicacion_id
WHERE us.username = @usuario
ORDER BY m.fecha DESC, m.id DESC;
GO

-- 2.10 Cantidad de movimientos por usuario y tipo
SELECT us.username, m.tipo, COUNT(*) AS movimientos
FROM movimientos_stock m
JOIN usuarios us ON us.id = m.usuario_id
GROUP BY us.username, m.tipo
ORDER BY us.username, m.tipo;
GO


/* =============================================================================
   3. CONSULTAS DE PRODUCTOS Y STOCK
   El stock total de un producto es la suma de sus existencias en todas las
   ubicaciones. Solo se consideran los productos activos.
   ============================================================================= */

-- 3.1 Listado de productos con categoría, proveedor preferido y stock total
SELECT p.codigo, p.nombre, c.nombre AS categoria, p.unidad_medida, p.precio,
       ISNULL(SUM(e.cantidad), 0) AS stock_total, p.stock_minimo, p.punto_pedido,
       ISNULL(pr.razon_social, '(sin proveedor)') AS proveedor_preferido
FROM productos p
JOIN categorias c ON c.id = p.categoria_id
LEFT JOIN existencias e ON e.producto_id = p.id
LEFT JOIN proveedores pr ON pr.id = p.proveedor_preferido_id
WHERE p.activo = 1
GROUP BY p.id, p.codigo, p.nombre, c.nombre, p.unidad_medida, p.precio,
         p.stock_minimo, p.punto_pedido, pr.razon_social
ORDER BY p.codigo;
GO

-- 3.2 Stock por ubicación (una columna por ubicación y el total)
SELECT p.codigo, p.nombre,
       ISNULL(SUM(CASE WHEN u.nombre = 'Sucursal' THEN e.cantidad END), 0) AS sucursal,
       ISNULL(SUM(CASE WHEN u.nombre = 'Depósito' THEN e.cantidad END), 0) AS deposito,
       ISNULL(SUM(e.cantidad), 0) AS total
FROM productos p
LEFT JOIN existencias e ON e.producto_id = p.id
LEFT JOIN ubicaciones u ON u.id = e.ubicacion_id
WHERE p.activo = 1
GROUP BY p.id, p.codigo, p.nombre
ORDER BY p.codigo;
GO

-- 3.3 Stock de un producto en cada ubicación (incluye ubicaciones sin existencias)
DECLARE @codigo VARCHAR(50) = 'BUL-001';

SELECT u.nombre AS ubicacion, ISNULL(e.cantidad, 0) AS cantidad
FROM ubicaciones u
CROSS JOIN productos p
LEFT JOIN existencias e ON e.ubicacion_id = u.id AND e.producto_id = p.id
WHERE p.codigo = @codigo
ORDER BY u.nombre;
GO

-- 3.4 Buscar productos por código, nombre o descripción
DECLARE @texto VARCHAR(100) = 'bulón';

SELECT p.codigo, p.nombre, p.precio, ISNULL(SUM(e.cantidad), 0) AS stock_total
FROM productos p
LEFT JOIN existencias e ON e.producto_id = p.id
WHERE p.codigo LIKE '%' + @texto + '%'
   OR p.nombre LIKE '%' + @texto + '%'
   OR p.descripcion LIKE '%' + @texto + '%'
GROUP BY p.id, p.codigo, p.nombre, p.precio
ORDER BY p.nombre;
GO

-- 3.5 Productos pendientes de reposición: stock total en o por debajo del punto
--     de pedido. BAJO_MINIMO si además llegó al stock mínimo (urgente);
--     PUNTO_PEDIDO si todavía está entre el mínimo y el punto de pedido.
--     (misma regla que EstadoStock.de en el backend)
SELECT p.codigo, p.nombre, t.stock_total, p.stock_minimo, p.punto_pedido,
       CASE WHEN t.stock_total <= p.stock_minimo THEN 'BAJO_MINIMO' ELSE 'PUNTO_PEDIDO' END AS estado,
       p.punto_pedido - t.stock_total AS faltante_para_punto_pedido,
       pr.razon_social AS proveedor_preferido, pr.telefono, pr.email
FROM productos p
CROSS APPLY (SELECT ISNULL(SUM(e.cantidad), 0) AS stock_total
             FROM existencias e WHERE e.producto_id = p.id) t
LEFT JOIN proveedores pr ON pr.id = p.proveedor_preferido_id
WHERE p.activo = 1
  AND t.stock_total <= p.punto_pedido
ORDER BY CASE WHEN t.stock_total <= p.stock_minimo THEN 0 ELSE 1 END, faltante_para_punto_pedido DESC;
GO

-- 3.6 Alertas de reposición abiertas (a lo sumo una por producto)
SELECT a.fecha, p.codigo, p.nombre, a.estado, a.stock, p.stock_minimo, p.punto_pedido
FROM alertas_stock a
JOIN productos p ON p.id = a.producto_id
WHERE a.resuelta_en IS NULL
ORDER BY CASE a.estado WHEN 'BAJO_MINIMO' THEN 0 ELSE 1 END, a.fecha DESC;
GO

-- 3.7 Productos sin proveedor preferido
SELECT codigo, nombre
FROM productos
WHERE proveedor_preferido_id IS NULL;
GO

-- 3.8 Productos con su lista de proveedores (relación N:M)
SELECT p.codigo, p.nombre, pr.razon_social,
       CASE WHEN pr.id = p.proveedor_preferido_id THEN 'sí' ELSE 'no' END AS preferido
FROM productos p
JOIN producto_proveedor pp ON pp.producto_id = p.id
JOIN proveedores pr ON pr.id = pp.proveedor_id
ORDER BY p.codigo, pr.razon_social;
GO


/* =============================================================================
   4. REPORTES
   ============================================================================= */

-- 4.1 Stock valorizado total
SELECT COUNT(DISTINCT p.id)                          AS productos,
       ISNULL(SUM(e.cantidad), 0)                    AS unidades,
       ISNULL(SUM(e.cantidad * ISNULL(p.precio, 0)), 0) AS valor_total
FROM productos p
LEFT JOIN existencias e ON e.producto_id = p.id
WHERE p.activo = 1;
GO

-- 4.2 Stock valorizado por ubicación
SELECT u.nombre AS ubicacion,
       COUNT(DISTINCT p.id)                      AS productos,
       SUM(e.cantidad)                           AS unidades,
       SUM(e.cantidad * ISNULL(p.precio, 0))     AS valor_total
FROM existencias e
JOIN productos p   ON p.id = e.producto_id
JOIN ubicaciones u ON u.id = e.ubicacion_id
WHERE p.activo = 1
GROUP BY u.nombre
ORDER BY valor_total DESC;
GO

-- 4.3 Stock valorizado por proveedor preferido
SELECT ISNULL(pr.razon_social, '(sin proveedor)') AS proveedor,
       COUNT(DISTINCT p.id)                       AS productos,
       ISNULL(SUM(e.cantidad), 0)                 AS unidades,
       ISNULL(SUM(e.cantidad * ISNULL(p.precio, 0)), 0) AS valor_total
FROM productos p
LEFT JOIN existencias e ON e.producto_id = p.id
LEFT JOIN proveedores pr ON pr.id = p.proveedor_preferido_id
WHERE p.activo = 1
GROUP BY pr.razon_social
ORDER BY valor_total DESC;
GO

-- 4.4 Resumen de movimientos por tipo en el mes actual
SELECT tipo,
       COUNT(*)      AS movimientos,
       SUM(cantidad) AS unidades
FROM movimientos_stock
WHERE fecha >= DATEFROMPARTS(YEAR(GETDATE()), MONTH(GETDATE()), 1)
GROUP BY tipo
ORDER BY tipo;
GO

-- 4.5 Top 5 productos con más salidas por venta en los últimos 30 días
SELECT TOP 5 p.codigo, p.nombre, SUM(m.cantidad) AS unidades_vendidas
FROM movimientos_stock m
JOIN productos p ON p.id = m.producto_id
WHERE m.tipo = 'SALIDA_VENTA'
  AND m.fecha >= DATEADD(DAY, -30, GETDATE())
GROUP BY p.codigo, p.nombre
ORDER BY unidades_vendidas DESC;
GO

-- 4.6 Control de consistencia: cada existencia debe coincidir con lo que
--     implican sus movimientos en esa ubicación. Si devuelve filas, hubo cambios
--     de stock sin movimiento. Un TRASLADO resta en el origen y suma en el destino.
WITH efecto AS (
    SELECT producto_id, ubicacion_id,
           CASE tipo WHEN 'SALIDA_VENTA' THEN -cantidad WHEN 'ROTURA_PERDIDA' THEN -cantidad
                     WHEN 'TRASLADO' THEN -cantidad ELSE cantidad END AS delta
    FROM movimientos_stock
    UNION ALL
    SELECT producto_id, ubicacion_destino_id, cantidad
    FROM movimientos_stock
    WHERE tipo = 'TRASLADO'
),
esperado AS (
    SELECT producto_id, ubicacion_id, SUM(delta) AS cantidad
    FROM efecto
    GROUP BY producto_id, ubicacion_id
)
SELECT p.codigo, u.nombre AS ubicacion,
       ISNULL(e.cantidad, 0)  AS stock_existencias,
       ISNULL(x.cantidad, 0)  AS stock_segun_movimientos
FROM productos p
CROSS JOIN ubicaciones u
LEFT JOIN existencias e ON e.producto_id = p.id AND e.ubicacion_id = u.id
LEFT JOIN esperado x    ON x.producto_id = p.id AND x.ubicacion_id = u.id
WHERE ISNULL(e.cantidad, 0) <> ISNULL(x.cantidad, 0);
GO

-- 4.7 Stock actual por ubicación de los productos tocados en la sección 2
--     (BUL-001 y PIN-005); se contrasta con 4.6, que debe devolver 0 filas.
SELECT p.codigo, u.nombre AS ubicacion, e.cantidad, e.version
FROM existencias e
JOIN productos p   ON p.id = e.producto_id
JOIN ubicaciones u ON u.id = e.ubicacion_id
WHERE p.codigo IN ('BUL-001', 'PIN-005')
ORDER BY p.codigo, u.nombre;
GO


/* =============================================================================
   5. ÓRDENES DE COMPRA, PROVEEDORES Y USUARIOS
   ============================================================================= */

-- 5.1 Órdenes de compra pendientes (borrador o enviadas, sin recibir)
SELECT oc.id, oc.estado, oc.fecha_creacion, pr.razon_social AS proveedor,
       us.username AS creada_por, COUNT(i.id) AS items, ISNULL(SUM(i.cantidad), 0) AS unidades
FROM ordenes_compra oc
JOIN proveedores pr ON pr.id = oc.proveedor_id
JOIN usuarios us    ON us.id = oc.usuario_id
LEFT JOIN ordenes_compra_items i ON i.orden_id = oc.id
WHERE oc.estado IN ('BORRADOR', 'ENVIADA')
GROUP BY oc.id, oc.estado, oc.fecha_creacion, pr.razon_social, us.username
ORDER BY oc.fecha_creacion DESC;
GO

-- 5.2 Detalle de una orden de compra
DECLARE @orden BIGINT = (SELECT TOP 1 id FROM ordenes_compra ORDER BY fecha_creacion DESC);

SELECT oc.id, oc.estado, pr.razon_social AS proveedor, p.codigo, p.nombre, i.cantidad
FROM ordenes_compra oc
JOIN proveedores pr ON pr.id = oc.proveedor_id
JOIN ordenes_compra_items i ON i.orden_id = oc.id
JOIN productos p ON p.id = i.producto_id
WHERE oc.id = @orden
ORDER BY p.codigo;
GO

-- 5.3 Proveedores con cantidad de productos que abastecen (N:M) y como preferido
SELECT pr.razon_social, pr.cuit, pr.telefono, pr.email,
       COUNT(DISTINCT pp.producto_id)  AS productos,
       COUNT(DISTINCT pf.id)           AS como_preferido
FROM proveedores pr
LEFT JOIN producto_proveedor pp ON pp.proveedor_id = pr.id
LEFT JOIN productos pf ON pf.proveedor_preferido_id = pr.id
GROUP BY pr.id, pr.razon_social, pr.cuit, pr.telefono, pr.email
ORDER BY pr.razon_social;
GO

-- 5.4 Usuarios activos por rol
SELECT rol, username, nombre
FROM usuarios
WHERE activo = 1
ORDER BY rol, username;
GO

-- 5.5 Cantidad de usuarios por rol
SELECT rol, COUNT(*) AS usuarios, SUM(CAST(activo AS INT)) AS activos
FROM usuarios
GROUP BY rol;
GO


/* =============================================================================
   6. MANTENIMIENTO
   Modifican datos: revisar los parámetros antes de ejecutar.
   ============================================================================= */

-- 6.1 Actualizar precios de los productos de un proveedor preferido en un porcentaje
DECLARE @proveedor VARCHAR(150) = 'Bulonera del Sur S.A.',
        @porcentaje DECIMAL(5,2) = 10.00;

UPDATE p
SET p.precio = ROUND(p.precio * (1 + @porcentaje / 100), 2)
FROM productos p
JOIN proveedores pr ON pr.id = p.proveedor_preferido_id
WHERE pr.razon_social = @proveedor;
GO

-- 6.2 Cambiar el stock mínimo de un producto. El punto de pedido nunca puede
--     ser menor que el mínimo (ck_productos_punto_pedido): se sube junto con él.
UPDATE productos SET stock_minimo = 150, punto_pedido = 300 WHERE codigo = 'BUL-001';
GO

-- 6.3 Desactivar un usuario (baja lógica: no se borra)
UPDATE usuarios SET activo = 0 WHERE username = 'ventas';
GO

-- 6.4 Dar de baja un producto (baja lógica: no se borra, deja de listarse)
UPDATE productos SET activo = 0 WHERE codigo = 'PIN-005';
GO

-- 6.5 Borrar un producto solo si no tiene movimientos, alertas ni órdenes de compra.
--     Primero se limpian sus existencias en cero y su lista de proveedores
--     (las FK fk_movimientos_producto, fk_alertas_producto y
--     fk_ordenes_compra_items_producto impiden borrar un producto con historial).
DECLARE @codigo VARCHAR(50) = 'PIN-005';
DECLARE @id BIGINT = (SELECT id FROM productos WHERE codigo = @codigo);

IF @id IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM movimientos_stock WHERE producto_id = @id)
   AND NOT EXISTS (SELECT 1 FROM alertas_stock WHERE producto_id = @id)
   AND NOT EXISTS (SELECT 1 FROM ordenes_compra_items WHERE producto_id = @id)
   AND NOT EXISTS (SELECT 1 FROM existencias WHERE producto_id = @id AND cantidad > 0)
BEGIN
    DELETE FROM existencias WHERE producto_id = @id;
    DELETE FROM producto_proveedor WHERE producto_id = @id;
    DELETE FROM productos WHERE id = @id;
END
GO

-- 6.6 Cantidad de filas por tabla
SELECT 'proveedores' AS tabla, COUNT(*) AS filas FROM proveedores
UNION ALL SELECT 'categorias',            COUNT(*) FROM categorias
UNION ALL SELECT 'ubicaciones',           COUNT(*) FROM ubicaciones
UNION ALL SELECT 'productos',             COUNT(*) FROM productos
UNION ALL SELECT 'producto_proveedor',    COUNT(*) FROM producto_proveedor
UNION ALL SELECT 'existencias',           COUNT(*) FROM existencias
UNION ALL SELECT 'movimientos_stock',     COUNT(*) FROM movimientos_stock
UNION ALL SELECT 'alertas_stock',         COUNT(*) FROM alertas_stock
UNION ALL SELECT 'ordenes_compra',        COUNT(*) FROM ordenes_compra
UNION ALL SELECT 'ordenes_compra_items',  COUNT(*) FROM ordenes_compra_items
UNION ALL SELECT 'usuarios',              COUNT(*) FROM usuarios;
GO

-- 6.7 Migraciones de Flyway aplicadas
SELECT installed_rank, version, description, installed_on, success
FROM flyway_schema_history
ORDER BY installed_rank;
GO
