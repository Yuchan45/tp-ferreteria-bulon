/* =============================================================================
   Bulón S.R.L. — Consultas básicas sobre la base bulon_stock (SQL Server)

   Uso: ejecutar cada bloque por separado (están delimitados por GO) desde
   Azure Data Studio, SSMS, DBeaver o sqlcmd (con sqlcmd agregar la opción -I).

   Este script NO lo ejecuta Flyway: el esquema se define en db/migration.
   ============================================================================= */

USE bulon_stock;
GO

-- Requerido por el índice filtrado de proveedores.cuit (sqlcmd lo trae en OFF)
SET QUOTED_IDENTIFIER ON;
GO


/* =============================================================================
   1. DATOS DE EJEMPLO
   Los carga el seeder del backend (com.bulon.stock.seed.DataSeeder) al iniciar
   con SEED_ENABLED=true: 3 usuarios, 5 proveedores y 25 productos con su stock
   inicial. Las consultas de este archivo usan esos datos.
   ============================================================================= */


/* =============================================================================
   2. MOVIMIENTOS DE STOCK
   Igual que el backend: el movimiento y la actualización del stock van en la
   misma transacción. ENTRADA suma, SALIDA resta, AJUSTE suma o resta según signo.
   ============================================================================= */

-- 2.1 Registrar una ENTRADA (compra / recepción de mercadería)
SET XACT_ABORT ON;
DECLARE @codigo VARCHAR(50) = 'BUL-001',
        @cantidad INT = 250;

BEGIN TRANSACTION;
    UPDATE productos SET stock_actual = stock_actual + @cantidad WHERE codigo = @codigo;

    INSERT INTO movimientos_stock (producto_id, tipo, cantidad, observacion)
    SELECT id, 'ENTRADA', @cantidad, 'Recepción de pedido a proveedor'
    FROM productos WHERE codigo = @codigo;
COMMIT;
GO

-- 2.2 Registrar una SALIDA (venta). Si no alcanza el stock, el CHECK
--     ck_productos_stock_actual hace fallar el UPDATE y se revierte todo.
SET XACT_ABORT ON;
DECLARE @codigo VARCHAR(50) = 'BUL-001',
        @cantidad INT = 180;

BEGIN TRANSACTION;
    UPDATE productos SET stock_actual = stock_actual - @cantidad WHERE codigo = @codigo;

    INSERT INTO movimientos_stock (producto_id, tipo, cantidad, observacion)
    SELECT id, 'SALIDA', @cantidad, 'Venta mostrador'
    FROM productos WHERE codigo = @codigo;
COMMIT;
GO

-- 2.3 Registrar un AJUSTE (por ejemplo, faltante detectado en un inventario)
SET XACT_ABORT ON;
DECLARE @codigo VARCHAR(50) = 'BUL-001',
        @cantidad INT = -5;   -- negativo resta, positivo suma

BEGIN TRANSACTION;
    UPDATE productos SET stock_actual = stock_actual + @cantidad WHERE codigo = @codigo;

    INSERT INTO movimientos_stock (producto_id, tipo, cantidad, observacion)
    SELECT id, 'AJUSTE', @cantidad, 'Ajuste por inventario físico'
    FROM productos WHERE codigo = @codigo;
COMMIT;
GO

-- 2.4 Historial de movimientos de un producto (más reciente primero)
DECLARE @codigo VARCHAR(50) = 'BUL-001';

SELECT m.fecha, m.tipo, m.cantidad, m.observacion
FROM movimientos_stock m
JOIN productos p ON p.id = m.producto_id
WHERE p.codigo = @codigo
ORDER BY m.fecha DESC;
GO

-- 2.5 Movimientos en un rango de fechas
DECLARE @desde DATETIME2 = DATEADD(DAY, -7, CAST(GETDATE() AS DATE)),
        @hasta DATETIME2 = DATEADD(DAY, 1, CAST(GETDATE() AS DATE));

SELECT m.fecha, p.codigo, p.descripcion, m.tipo, m.cantidad, m.observacion
FROM movimientos_stock m
JOIN productos p ON p.id = m.producto_id
WHERE m.fecha >= @desde AND m.fecha < @hasta
ORDER BY m.fecha DESC;
GO


/* =============================================================================
   3. CONSULTAS DE PRODUCTOS Y STOCK
   ============================================================================= */

-- 3.1 Listado de productos con su proveedor
SELECT p.codigo, p.descripcion, p.precio, p.stock_actual, p.stock_minimo,
       ISNULL(pr.razon_social, '(sin proveedor)') AS proveedor
FROM productos p
LEFT JOIN proveedores pr ON pr.id = p.proveedor_id
ORDER BY p.codigo;
GO

-- 3.2 Buscar productos por código o descripción
DECLARE @texto VARCHAR(100) = 'bulón';

SELECT codigo, descripcion, precio, stock_actual
FROM productos
WHERE codigo LIKE '%' + @texto + '%'
   OR descripcion LIKE '%' + @texto + '%'
ORDER BY descripcion;
GO

-- 3.3 Alerta de reposición: productos en o por debajo del stock mínimo
--     (misma regla que GET /api/productos/bajo-stock)
SELECT p.codigo, p.descripcion, p.stock_actual, p.stock_minimo,
       p.stock_minimo - p.stock_actual AS faltante,
       pr.razon_social AS proveedor, pr.telefono, pr.email
FROM productos p
LEFT JOIN proveedores pr ON pr.id = p.proveedor_id
WHERE p.stock_actual <= p.stock_minimo
ORDER BY faltante DESC;
GO

-- 3.4 Productos sin proveedor asignado
SELECT codigo, descripcion
FROM productos
WHERE proveedor_id IS NULL;
GO


/* =============================================================================
   4. REPORTES
   ============================================================================= */

-- 4.1 Stock valorizado total
SELECT COUNT(*)                         AS productos,
       SUM(stock_actual)                AS unidades,
       SUM(stock_actual * ISNULL(precio, 0)) AS valor_total
FROM productos;
GO

-- 4.2 Stock valorizado por proveedor
SELECT ISNULL(pr.razon_social, '(sin proveedor)') AS proveedor,
       COUNT(p.id)                                AS productos,
       SUM(p.stock_actual)                        AS unidades,
       SUM(p.stock_actual * ISNULL(p.precio, 0))  AS valor_total
FROM productos p
LEFT JOIN proveedores pr ON pr.id = p.proveedor_id
GROUP BY pr.razon_social
ORDER BY valor_total DESC;
GO

-- 4.3 Resumen de movimientos por tipo en el mes actual
SELECT tipo,
       COUNT(*)      AS movimientos,
       SUM(cantidad) AS unidades
FROM movimientos_stock
WHERE fecha >= DATEFROMPARTS(YEAR(GETDATE()), MONTH(GETDATE()), 1)
GROUP BY tipo
ORDER BY tipo;
GO

-- 4.4 Top 5 productos con más salidas en los últimos 30 días
SELECT TOP 5 p.codigo, p.descripcion, SUM(m.cantidad) AS unidades_salidas
FROM movimientos_stock m
JOIN productos p ON p.id = m.producto_id
WHERE m.tipo = 'SALIDA'
  AND m.fecha >= DATEADD(DAY, -30, GETDATE())
GROUP BY p.codigo, p.descripcion
ORDER BY unidades_salidas DESC;
GO

-- 4.5 Control de consistencia: el stock actual debe coincidir con la suma de
--     sus movimientos. Si devuelve filas, hubo cambios de stock sin movimiento.
SELECT p.codigo, p.stock_actual, ISNULL(mv.total, 0) AS stock_segun_movimientos
FROM productos p
LEFT JOIN (
    SELECT producto_id,
           SUM(CASE tipo WHEN 'SALIDA' THEN -cantidad ELSE cantidad END) AS total
    FROM movimientos_stock
    GROUP BY producto_id
) mv ON mv.producto_id = p.id
WHERE p.stock_actual <> ISNULL(mv.total, 0);
GO


/* =============================================================================
   5. PROVEEDORES Y USUARIOS
   ============================================================================= */

-- 5.1 Proveedores con cantidad de productos que abastecen
SELECT pr.razon_social, pr.cuit, pr.telefono, pr.email,
       COUNT(p.id) AS productos
FROM proveedores pr
LEFT JOIN productos p ON p.proveedor_id = pr.id
GROUP BY pr.id, pr.razon_social, pr.cuit, pr.telefono, pr.email
ORDER BY pr.razon_social;
GO

-- 5.2 Usuarios activos por rol
SELECT rol, username, nombre
FROM usuarios
WHERE activo = 1
ORDER BY rol, username;
GO

-- 5.3 Cantidad de usuarios por rol
SELECT rol, COUNT(*) AS usuarios, SUM(CAST(activo AS INT)) AS activos
FROM usuarios
GROUP BY rol;
GO


/* =============================================================================
   6. MANTENIMIENTO
   Modifican datos: revisar los parámetros antes de ejecutar.
   ============================================================================= */

-- 6.1 Actualizar precios de un proveedor en un porcentaje
DECLARE @proveedor VARCHAR(150) = 'Bulonera del Sur S.A.',
        @porcentaje DECIMAL(5,2) = 10.00;

UPDATE p
SET p.precio = ROUND(p.precio * (1 + @porcentaje / 100), 2)
FROM productos p
JOIN proveedores pr ON pr.id = p.proveedor_id
WHERE pr.razon_social = @proveedor;
GO

-- 6.2 Cambiar el stock mínimo de un producto
UPDATE productos SET stock_minimo = 150 WHERE codigo = 'BUL-001';
GO

-- 6.3 Desactivar un usuario (baja lógica: no se borra)
UPDATE usuarios SET activo = 0 WHERE username = 'ventas';
GO

-- 6.4 Borrar un producto solo si no tiene movimientos
--     (con movimientos, la FK fk_movimientos_producto impide el borrado)
DELETE FROM productos
WHERE codigo = 'PIN-005'
  AND NOT EXISTS (SELECT 1 FROM movimientos_stock m WHERE m.producto_id = productos.id);
GO

-- 6.5 Cantidad de filas por tabla
SELECT 'proveedores' AS tabla, COUNT(*) AS filas FROM proveedores
UNION ALL SELECT 'productos',         COUNT(*) FROM productos
UNION ALL SELECT 'movimientos_stock', COUNT(*) FROM movimientos_stock
UNION ALL SELECT 'usuarios',          COUNT(*) FROM usuarios;
GO

-- 6.6 Migraciones de Flyway aplicadas
SELECT installed_rank, version, description, installed_on, success
FROM flyway_schema_history
ORDER BY installed_rank;
GO
