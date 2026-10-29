/*
 * Bulón S.R.L. — Consultas básicas de uso manual
 *
 * Esquema v2 (migraciones V1 + V2__sucursales_y_reposicion.sql). Base: bulon_stock (SQL Server).
 * Todas son de solo lectura (SELECT). Los parámetros se cambian en cada DECLARE.
 *
 * Efecto de cada tipo de movimiento sobre el stock:
 *   suman:  INGRESO_COMPRA, DEVOLUCION_CLIENTE      restan: SALIDA_VENTA, DEVOLUCION_PROVEEDOR, ROTURA
 *   AJUSTE: aplica la cantidad con su signo
 *
 * Para ejecutar desde la terminal con sqlcmd agregar -I (lo requieren los índices filtrados):
 *   sqlcmd -S localhost,1433 -U sa -P <password> -C -I -d bulon_stock -i queries.sql
 *
 * Índice
 *    1. Sucursales
 *    2. Usuarios
 *    3. Categorías
 *    4. Proveedores
 *    5. Productos
 *    6. Stock por sucursal y reposición
 *    7. Movimientos de stock
 *    8. Alertas, solicitudes de reposición y notificaciones
 *    9. Reportes y resúmenes
 *   10. Controles de integridad
 *   11. Esquema y migraciones
 */

USE bulon_stock;
GO


/* ═══════════════════════════════════════════════════════════════════════
 * 1. SUCURSALES
 * ═══════════════════════════════════════════════════════════════════════ */

-- 1.1 Sucursales con cantidad de empleados y de productos con stock
SELECT s.id, s.codigo, s.nombre, s.direccion, s.telefono, s.activa,
       (SELECT COUNT(*) FROM usuarios u WHERE u.sucursal_id = s.id AND u.activo = 1) AS empleados,
       (SELECT COUNT(*) FROM stock_sucursal ss WHERE ss.sucursal_id = s.id AND ss.cantidad > 0) AS productos_con_stock
FROM sucursales s
ORDER BY s.codigo;
GO


/* ═══════════════════════════════════════════════════════════════════════
 * 2. USUARIOS
 * ═══════════════════════════════════════════════════════════════════════ */

-- 2.1 Usuarios con su sucursal (el administrador no tiene: ve todas)
SELECT u.id, u.username, u.nombre, u.email, u.rol,
       ISNULL(s.nombre, '(todas)') AS sucursal, u.activo, u.creado
FROM usuarios u
LEFT JOIN sucursales s ON s.id = u.sucursal_id
ORDER BY u.rol, u.nombre;

select * from usuarios

-- 2.2 Usuarios activos por rol
SELECT rol, COUNT(*) AS usuarios
FROM usuarios
WHERE activo = 1
GROUP BY rol;

-- 2.3 Empleados de una sucursal
DECLARE @sucursalEmpleados VARCHAR(20) = 'SUC-01';
SELECT *
FROM usuarios u
JOIN sucursales s ON s.id = u.sucursal_id
WHERE s.codigo = @sucursalEmpleados
ORDER BY u.nombre;


-- 2.4 Movimientos registrados por cada usuario
SELECT u.username, u.nombre, COUNT(m.id) AS movimientos, MAX(m.fecha) AS ultimo
FROM usuarios u
LEFT JOIN movimientos_stock m ON m.usuario_id = u.id
GROUP BY u.username, u.nombre
ORDER BY movimientos DESC;
GO


/* ═══════════════════════════════════════════════════════════════════════
 * 3. CATEGORÍAS
 * ═══════════════════════════════════════════════════════════════════════ */
select * from categorias

-- 3.1 Categorías con cantidad de productos (incluye "sin categoría")
SELECT ISNULL(c.nombre, '(sin categoría)') AS categoria, COUNT(p.id) AS productos
FROM productos p
LEFT JOIN categorias c ON c.id = p.categoria_id
GROUP BY c.nombre
ORDER BY productos DESC;
GO


/* ═══════════════════════════════════════════════════════════════════════
 * 4. PROVEEDORES
 * ═══════════════════════════════════════════════════════════════════════ */

-- 4.1 Proveedores con cantidad de productos que venden y en cuántos son preferidos
SELECT pr.id, pr.razon_social, pr.cuit, pr.telefono, pr.email, pr.contacto, pr.activo,
       COUNT(pp.producto_id) AS productos,
       SUM(CASE WHEN pp.preferido = 1 THEN 1 ELSE 0 END) AS preferido_en
FROM proveedores pr
LEFT JOIN producto_proveedor pp ON pp.proveedor_id = pr.id
GROUP BY pr.id, pr.razon_social, pr.cuit, pr.telefono, pr.email, pr.contacto, pr.activo
ORDER BY pr.razon_social;

-- 4.2 Buscar proveedor por razón social o CUIT
DECLARE @buscarProveedor VARCHAR(150) = 'sur';
SELECT id, razon_social, cuit, telefono, email, contacto
FROM proveedores
WHERE razon_social LIKE '%' + @buscarProveedor + '%'
   OR cuit LIKE '%' + @buscarProveedor + '%';

-- 4.3 Productos que vende un proveedor, con sus condiciones
DECLARE @proveedorCuit VARCHAR(13) = '30-71000006-6';
SELECT p.codigo, p.nombre, pp.preferido, pp.codigo_proveedor, pp.precio_costo, pp.plazo_entrega_dias
FROM producto_proveedor pp
JOIN productos p ON p.id = pp.producto_id
JOIN proveedores pr ON pr.id = pp.proveedor_id
WHERE pr.cuit = @proveedorCuit
ORDER BY p.codigo;

-- 4.4 Comparación de proveedores de un mismo producto (costo y plazo)
DECLARE @codigoComparar VARCHAR(50) = 'HER-003';
SELECT pr.razon_social, pp.preferido, pp.precio_costo, pp.plazo_entrega_dias,
       RANK() OVER (ORDER BY pp.precio_costo)       AS ranking_costo,
       RANK() OVER (ORDER BY pp.plazo_entrega_dias) AS ranking_plazo
FROM producto_proveedor pp
JOIN productos p ON p.id = pp.producto_id
JOIN proveedores pr ON pr.id = pp.proveedor_id
WHERE p.codigo = @codigoComparar;

-- 4.5 Productos con más de un proveedor
SELECT p.codigo, p.nombre, COUNT(*) AS proveedores
FROM producto_proveedor pp
JOIN productos p ON p.id = pp.producto_id
GROUP BY p.codigo, p.nombre
HAVING COUNT(*) > 1
ORDER BY p.codigo;
GO


/* ═══════════════════════════════════════════════════════════════════════
 * 5. PRODUCTOS
 * ═══════════════════════════════════════════════════════════════════════ */

-- 5.1 Catálogo con categoría, proveedor preferido y stock total (todas las sucursales)
SELECT p.id, p.codigo, p.nombre, c.nombre AS categoria, p.unidad_medida, p.precio, p.activo,
       pref.razon_social AS proveedor_preferido,
       (SELECT SUM(ss.cantidad) FROM stock_sucursal ss WHERE ss.producto_id = p.id) AS stock_total
FROM productos p
LEFT JOIN categorias c ON c.id = p.categoria_id
OUTER APPLY (
    SELECT pr.razon_social
    FROM producto_proveedor pp
    JOIN proveedores pr ON pr.id = pp.proveedor_id
    WHERE pp.producto_id = p.id AND pp.preferido = 1
) pref
ORDER BY p.codigo;

-- 5.2 Buscar producto por código, nombre o descripción
DECLARE @buscarProducto VARCHAR(200) = 'bulón';
SELECT id, codigo, nombre, descripcion, unidad_medida, precio
FROM productos
WHERE codigo LIKE '%' + @buscarProducto + '%'
   OR nombre LIKE '%' + @buscarProducto + '%'
   OR descripcion LIKE '%' + @buscarProducto + '%'
ORDER BY codigo;

-- 5.3 Detalle de un producto por código, con su stock en cada sucursal
DECLARE @codigo VARCHAR(50) = 'HER-003';
SELECT p.codigo, p.nombre, p.descripcion, c.nombre AS categoria, p.unidad_medida, p.precio,
       s.nombre AS sucursal, ss.cantidad, ss.stock_minimo, ss.punto_pedido
FROM productos p
LEFT JOIN categorias c ON c.id = p.categoria_id
LEFT JOIN stock_sucursal ss ON ss.producto_id = p.id
LEFT JOIN sucursales s ON s.id = ss.sucursal_id
WHERE p.codigo = @codigo;

-- 5.4 Productos que se venden fraccionados (metro, kilo, litro)
SELECT codigo, nombre, unidad_medida, precio
FROM productos
WHERE unidad_medida IN ('METRO', 'KILOGRAMO', 'LITRO')
ORDER BY codigo;

-- 5.5 Productos sin precio, sin categoría o sin proveedor
SELECT p.codigo, p.nombre,
       CASE WHEN p.precio IS NULL THEN 'sí' ELSE '' END AS sin_precio,
       CASE WHEN p.categoria_id IS NULL THEN 'sí' ELSE '' END AS sin_categoria,
       CASE WHEN NOT EXISTS (SELECT 1 FROM producto_proveedor pp WHERE pp.producto_id = p.id)
            THEN 'sí' ELSE '' END AS sin_proveedor
FROM productos p
WHERE p.precio IS NULL
   OR p.categoria_id IS NULL
   OR NOT EXISTS (SELECT 1 FROM producto_proveedor pp WHERE pp.producto_id = p.id)
ORDER BY p.codigo;

-- 5.6 Productos inactivos
SELECT codigo, nombre, actualizado_en
FROM productos
WHERE activo = 0;
GO


/* ═══════════════════════════════════════════════════════════════════════
 * 6. STOCK POR SUCURSAL Y REPOSICIÓN
 * ═══════════════════════════════════════════════════════════════════════ */

-- 6.1 Estado del stock de cada producto en cada sucursal
--     (misma regla que el backend: CRITICO ≤ mínimo, REPONER ≤ punto de pedido)
SELECT s.codigo AS sucursal, p.codigo, p.nombre, ss.cantidad, ss.stock_minimo, ss.punto_pedido,
       CASE
           WHEN ss.cantidad <= ss.stock_minimo THEN 'CRITICO'
           WHEN ss.cantidad <= ss.punto_pedido THEN 'REPONER'
           ELSE 'OK'
       END AS estado
FROM stock_sucursal ss
JOIN productos p ON p.id = ss.producto_id
JOIN sucursales s ON s.id = ss.sucursal_id
ORDER BY s.codigo, p.codigo;

-- 6.2 A reponer, ordenado por urgencia (equivale a GET /api/productos/bajo-stock?sucursalId=)
DECLARE @sucursalReponer VARCHAR(20) = 'SUC-01';
SELECT p.codigo, p.nombre, ss.cantidad, ss.stock_minimo, ss.punto_pedido,
       ss.punto_pedido - ss.cantidad AS faltante_hasta_punto_pedido,
       CASE WHEN ss.cantidad <= ss.stock_minimo THEN 'CRITICO' ELSE 'REPONER' END AS nivel,
       pref.razon_social AS proveedor_preferido, pref.telefono
FROM stock_sucursal ss
JOIN productos p ON p.id = ss.producto_id
JOIN sucursales s ON s.id = ss.sucursal_id
OUTER APPLY (
    SELECT pr.razon_social, pr.telefono
    FROM producto_proveedor pp
    JOIN proveedores pr ON pr.id = pp.proveedor_id
    WHERE pp.producto_id = p.id AND pp.preferido = 1
) pref
WHERE s.codigo = @sucursalReponer
  AND ss.cantidad <= ss.punto_pedido
ORDER BY CASE WHEN ss.punto_pedido > 0 THEN ss.cantidad / ss.punto_pedido ELSE 1 END, p.codigo;

-- 6.3 Pedido sugerido por proveedor preferido: cuánto pedir para volver al punto de pedido
SELECT s.codigo AS sucursal, pr.razon_social AS proveedor, pr.telefono, pr.email,
       COUNT(*) AS productos,
       SUM(ss.punto_pedido - ss.cantidad) AS unidades_a_pedir,
       SUM((ss.punto_pedido - ss.cantidad) * pp.precio_costo) AS costo_estimado
FROM stock_sucursal ss
JOIN sucursales s ON s.id = ss.sucursal_id
JOIN producto_proveedor pp ON pp.producto_id = ss.producto_id AND pp.preferido = 1
JOIN proveedores pr ON pr.id = pp.proveedor_id
WHERE ss.cantidad <= ss.punto_pedido
GROUP BY s.codigo, pr.razon_social, pr.telefono, pr.email
ORDER BY s.codigo, costo_estimado DESC;

-- 6.4 Proveedor más barato para cada producto a reponer (lo que hará la Strategy "menor costo")
SELECT p.codigo, p.nombre, mejor.razon_social AS proveedor_mas_barato, mejor.precio_costo, mejor.plazo_entrega_dias
FROM stock_sucursal ss
JOIN productos p ON p.id = ss.producto_id
CROSS APPLY (
    SELECT TOP (1) pr.razon_social, pp.precio_costo, pp.plazo_entrega_dias
    FROM producto_proveedor pp
    JOIN proveedores pr ON pr.id = pp.proveedor_id
    WHERE pp.producto_id = p.id AND pp.precio_costo IS NOT NULL
    ORDER BY pp.precio_costo, pp.plazo_entrega_dias
) mejor
WHERE ss.cantidad <= ss.punto_pedido
ORDER BY p.codigo;

-- 6.5 Stock total de cada producto sumando todas las sucursales
SELECT p.codigo, p.nombre, p.unidad_medida, SUM(ss.cantidad) AS stock_total
FROM productos p
JOIN stock_sucursal ss ON ss.producto_id = p.id
GROUP BY p.codigo, p.nombre, p.unidad_medida
ORDER BY p.codigo;

-- 6.6 Valor del inventario a precio de venta y a costo (proveedor preferido), por sucursal
SELECT s.codigo AS sucursal,
       SUM(ss.cantidad * p.precio)        AS valor_venta,
       SUM(ss.cantidad * pp.precio_costo) AS valor_costo
FROM stock_sucursal ss
JOIN sucursales s ON s.id = ss.sucursal_id
JOIN productos p ON p.id = ss.producto_id
LEFT JOIN producto_proveedor pp ON pp.producto_id = p.id AND pp.preferido = 1
GROUP BY s.codigo;

-- 6.7 Cambios recientes de umbrales (auditoría de stock mínimo y punto de pedido)
SELECT TOP (20) p.codigo, s.codigo AS sucursal, ss.stock_minimo, ss.punto_pedido,
       u.username AS actualizado_por, ss.actualizado_en
FROM stock_sucursal ss
JOIN productos p ON p.id = ss.producto_id
JOIN sucursales s ON s.id = ss.sucursal_id
LEFT JOIN usuarios u ON u.id = ss.actualizado_por
ORDER BY ss.actualizado_en DESC;
GO


/* ═══════════════════════════════════════════════════════════════════════
 * 7. MOVIMIENTOS DE STOCK
 * ═══════════════════════════════════════════════════════════════════════ */

-- 7.1 Últimos 20 movimientos con sucursal, responsable y proveedor
SELECT TOP (20) m.fecha, s.codigo AS sucursal, p.codigo, p.nombre, m.tipo, m.cantidad,
       u.username AS responsable, pr.razon_social AS proveedor, m.referencia, m.motivo, m.observaciones
FROM movimientos_stock m
JOIN productos p ON p.id = m.producto_id
JOIN sucursales s ON s.id = m.sucursal_id
JOIN usuarios u ON u.id = m.usuario_id
LEFT JOIN proveedores pr ON pr.id = m.proveedor_id
ORDER BY m.fecha DESC, m.id DESC;

-- 7.2 Historial de un producto en una sucursal con el stock resultante después de cada movimiento
--     (no se guarda: se calcula con una suma acumulada del efecto de cada movimiento)
DECLARE @codigoHistorial VARCHAR(50) = 'HER-003';
DECLARE @sucursalHistorial VARCHAR(20) = 'SUC-01';
SELECT m.fecha, m.tipo, m.cantidad,
       SUM(CASE WHEN m.tipo IN ('INGRESO_COMPRA', 'DEVOLUCION_CLIENTE', 'AJUSTE') THEN m.cantidad
                ELSE -m.cantidad END)
           OVER (ORDER BY m.fecha, m.id ROWS UNBOUNDED PRECEDING) AS stock_resultante,
       u.username AS responsable, m.referencia, m.motivo, m.observaciones
FROM movimientos_stock m
JOIN productos p ON p.id = m.producto_id
JOIN sucursales s ON s.id = m.sucursal_id
JOIN usuarios u ON u.id = m.usuario_id
WHERE p.codigo = @codigoHistorial AND s.codigo = @sucursalHistorial
ORDER BY m.fecha DESC, m.id DESC;

-- 7.3 Movimientos en un rango de fechas
DECLARE @desde DATE = DATEADD(DAY, -30, CAST(GETDATE() AS DATE));
DECLARE @hasta DATE = CAST(GETDATE() AS DATE);
SELECT m.fecha, s.codigo AS sucursal, p.codigo, m.tipo, m.cantidad, u.username AS responsable, m.referencia
FROM movimientos_stock m
JOIN productos p ON p.id = m.producto_id
JOIN sucursales s ON s.id = m.sucursal_id
JOIN usuarios u ON u.id = m.usuario_id
WHERE m.fecha >= @desde AND m.fecha < DATEADD(DAY, 1, @hasta)
ORDER BY m.fecha DESC;

-- 7.4 Movimientos de hoy
SELECT m.fecha, s.codigo AS sucursal, p.codigo, p.nombre, m.tipo, m.cantidad, u.username AS responsable
FROM movimientos_stock m
JOIN productos p ON p.id = m.producto_id
JOIN sucursales s ON s.id = m.sucursal_id
JOIN usuarios u ON u.id = m.usuario_id
WHERE CAST(m.fecha AS DATE) = CAST(GETDATE() AS DATE)
ORDER BY m.fecha DESC;

-- 7.5 Movimientos de un tipo
DECLARE @tipo VARCHAR(25) = 'AJUSTE';
SELECT m.fecha, s.codigo AS sucursal, p.codigo, m.cantidad, m.motivo, u.username AS responsable
FROM movimientos_stock m
JOIN productos p ON p.id = m.producto_id
JOIN sucursales s ON s.id = m.sucursal_id
JOIN usuarios u ON u.id = m.usuario_id
WHERE m.tipo = @tipo
ORDER BY m.fecha DESC;

-- 7.6 Compras y devoluciones a proveedor, por proveedor
SELECT pr.razon_social, m.tipo, COUNT(*) AS movimientos, SUM(m.cantidad) AS cantidad
FROM movimientos_stock m
JOIN proveedores pr ON pr.id = m.proveedor_id
GROUP BY pr.razon_social, m.tipo
ORDER BY pr.razon_social, m.tipo;

-- 7.7 Buscar movimientos por comprobante (remito, factura o ticket)
DECLARE @referencia VARCHAR(50) = 'TK-0001-00000101';
SELECT m.fecha, p.codigo, m.tipo, m.cantidad, m.motivo
FROM movimientos_stock m
JOIN productos p ON p.id = m.producto_id
WHERE m.referencia = @referencia;
GO


/* ═══════════════════════════════════════════════════════════════════════
 * 8. ALERTAS, SOLICITUDES DE REPOSICIÓN Y NOTIFICACIONES
 *    (las tablas existen; se completan cuando se implementen los patrones
 *     Observer y State en el backend)
 * ═══════════════════════════════════════════════════════════════════════ */

-- 8.1 Alertas activas por sucursal y nivel
SELECT s.codigo AS sucursal, p.codigo, a.nivel, a.estado, a.cantidad_al_generar,
       a.punto_pedido, a.stock_minimo, a.creada
FROM alertas_stock a
JOIN productos p ON p.id = a.producto_id
JOIN sucursales s ON s.id = a.sucursal_id
WHERE a.estado IN ('ABIERTA', 'EN_GESTION')
ORDER BY CASE a.nivel WHEN 'CRITICO' THEN 0 ELSE 1 END, a.creada;

-- 8.2 Productos a reponer que todavía no tienen alerta activa
SELECT s.codigo AS sucursal, p.codigo, ss.cantidad, ss.punto_pedido
FROM stock_sucursal ss
JOIN productos p ON p.id = ss.producto_id
JOIN sucursales s ON s.id = ss.sucursal_id
WHERE ss.cantidad <= ss.punto_pedido
  AND NOT EXISTS (SELECT 1 FROM alertas_stock a
                  WHERE a.producto_id = ss.producto_id AND a.sucursal_id = ss.sucursal_id
                    AND a.estado IN ('ABIERTA', 'EN_GESTION'))
ORDER BY s.codigo, p.codigo;

-- 8.3 Solicitudes de reposición con su proveedor y cantidad de ítems
SELECT sr.id, s.codigo AS sucursal, pr.razon_social AS proveedor, sr.estado,
       sr.creada, sr.enviada, sr.recibida, u.username AS creada_por,
       (SELECT COUNT(*) FROM solicitud_items si WHERE si.solicitud_id = sr.id) AS items
FROM solicitudes_reposicion sr
JOIN sucursales s ON s.id = sr.sucursal_id
JOIN proveedores pr ON pr.id = sr.proveedor_id
JOIN usuarios u ON u.id = sr.usuario_id
ORDER BY sr.creada DESC;

-- 8.4 Ítems de solicitudes recibidas con diferencia entre lo pedido y lo recibido
SELECT sr.id AS solicitud, p.codigo, si.cantidad_solicitada, si.cantidad_recibida,
       si.cantidad_solicitada - ISNULL(si.cantidad_recibida, 0) AS diferencia
FROM solicitud_items si
JOIN solicitudes_reposicion sr ON sr.id = si.solicitud_id
JOIN productos p ON p.id = si.producto_id
WHERE sr.estado = 'RECIBIDA'
  AND si.cantidad_recibida < si.cantidad_solicitada;

-- 8.5 Notificaciones sin leer por usuario
SELECT u.username, COUNT(*) AS sin_leer, MAX(n.creada) AS ultima
FROM notificaciones n
JOIN usuarios u ON u.id = n.usuario_id
WHERE n.leida = 0
GROUP BY u.username;
GO


/* ═══════════════════════════════════════════════════════════════════════
 * 9. REPORTES Y RESÚMENES
 * ═══════════════════════════════════════════════════════════════════════ */

-- 9.1 Resumen general (los indicadores del panel)
SELECT
    (SELECT COUNT(*) FROM productos WHERE activo = 1)                                  AS productos_activos,
    (SELECT COUNT(*) FROM stock_sucursal
      WHERE cantidad > stock_minimo AND cantidad <= punto_pedido)                      AS a_reponer,
    (SELECT COUNT(*) FROM stock_sucursal WHERE cantidad <= stock_minimo)               AS criticos,
    (SELECT COUNT(*) FROM stock_sucursal WHERE cantidad = 0)                           AS sin_stock,
    (SELECT COUNT(*) FROM movimientos_stock
      WHERE CAST(fecha AS DATE) = CAST(GETDATE() AS DATE))                             AS movimientos_hoy,
    (SELECT COUNT(*) FROM proveedores WHERE activo = 1)                                AS proveedores,
    (SELECT COUNT(*) FROM sucursales WHERE activa = 1)                                 AS sucursales;

-- 9.2 Movimientos y cantidades por tipo
SELECT tipo, COUNT(*) AS movimientos, SUM(cantidad) AS cantidad
FROM movimientos_stock
GROUP BY tipo
ORDER BY tipo;

-- 9.3 Movimientos por día y tipo (últimos 30 días)
SELECT CAST(fecha AS DATE) AS dia,
       SUM(CASE WHEN tipo = 'INGRESO_COMPRA' THEN 1 ELSE 0 END)                         AS compras,
       SUM(CASE WHEN tipo = 'SALIDA_VENTA' THEN 1 ELSE 0 END)                           AS ventas,
       SUM(CASE WHEN tipo IN ('DEVOLUCION_CLIENTE', 'DEVOLUCION_PROVEEDOR') THEN 1 ELSE 0 END) AS devoluciones,
       SUM(CASE WHEN tipo = 'ROTURA' THEN 1 ELSE 0 END)                                 AS roturas,
       SUM(CASE WHEN tipo = 'AJUSTE' THEN 1 ELSE 0 END)                                 AS ajustes
FROM movimientos_stock
WHERE fecha >= DATEADD(DAY, -30, CAST(GETDATE() AS DATE))
GROUP BY CAST(fecha AS DATE)
ORDER BY dia DESC;

-- 9.4 Productos más vendidos (mayor rotación) en los últimos 30 días
SELECT TOP (10) p.codigo, p.nombre, p.unidad_medida,
       SUM(m.cantidad) AS cantidad_vendida, COUNT(*) AS ventas
FROM movimientos_stock m
JOIN productos p ON p.id = m.producto_id
WHERE m.tipo = 'SALIDA_VENTA'
  AND m.fecha >= DATEADD(DAY, -30, CAST(GETDATE() AS DATE))
GROUP BY p.codigo, p.nombre, p.unidad_medida
ORDER BY cantidad_vendida DESC;

-- 9.5 Pérdidas: roturas y devoluciones a proveedor valorizadas a costo
SELECT p.codigo, p.nombre, m.tipo, SUM(m.cantidad) AS cantidad,
       SUM(m.cantidad * pp.precio_costo) AS valor_costo
FROM movimientos_stock m
JOIN productos p ON p.id = m.producto_id
LEFT JOIN producto_proveedor pp ON pp.producto_id = p.id AND pp.preferido = 1
WHERE m.tipo IN ('ROTURA', 'DEVOLUCION_PROVEEDOR')
GROUP BY p.codigo, p.nombre, m.tipo
ORDER BY valor_costo DESC;

-- 9.6 Productos sin movimientos en los últimos 30 días
SELECT p.codigo, p.nombre
FROM productos p
WHERE NOT EXISTS (
    SELECT 1 FROM movimientos_stock m
    WHERE m.producto_id = p.id
      AND m.fecha >= DATEADD(DAY, -30, CAST(GETDATE() AS DATE))
)
ORDER BY p.codigo;
GO


/* ═══════════════════════════════════════════════════════════════════════
 * 10. CONTROLES DE INTEGRIDAD (cada consulta debería devolver 0 filas)
 * ═══════════════════════════════════════════════════════════════════════ */

-- 10.1 Stock de cada sucursal vs. stock reconstruido desde sus movimientos
SELECT p.codigo, s.codigo AS sucursal, ss.cantidad,
       ISNULL(mov.total, 0) AS segun_movimientos
FROM stock_sucursal ss
JOIN productos p ON p.id = ss.producto_id
JOIN sucursales s ON s.id = ss.sucursal_id
OUTER APPLY (
    SELECT SUM(CASE WHEN m.tipo IN ('INGRESO_COMPRA', 'DEVOLUCION_CLIENTE', 'AJUSTE') THEN m.cantidad
                    ELSE -m.cantidad END) AS total
    FROM movimientos_stock m
    WHERE m.producto_id = ss.producto_id AND m.sucursal_id = ss.sucursal_id
) mov
WHERE ss.cantidad <> ISNULL(mov.total, 0);

-- 10.2 Productos a los que les falta la fila de stock en alguna sucursal
SELECT p.codigo, s.codigo AS sucursal
FROM productos p
CROSS JOIN sucursales s
WHERE NOT EXISTS (SELECT 1 FROM stock_sucursal ss WHERE ss.producto_id = p.id AND ss.sucursal_id = s.id);

-- 10.3 Compras o devoluciones a proveedor de un proveedor que no vende ese producto
SELECT m.id, m.fecha, p.codigo, m.tipo, pr.razon_social
FROM movimientos_stock m
JOIN productos p ON p.id = m.producto_id
JOIN proveedores pr ON pr.id = m.proveedor_id
WHERE NOT EXISTS (SELECT 1 FROM producto_proveedor pp
                  WHERE pp.producto_id = m.producto_id AND pp.proveedor_id = m.proveedor_id);

-- 10.4 Compras o devoluciones a proveedor sin proveedor
SELECT m.id, m.fecha, m.producto_id, m.tipo
FROM movimientos_stock m
WHERE m.tipo IN ('INGRESO_COMPRA', 'DEVOLUCION_PROVEEDOR') AND m.proveedor_id IS NULL;

-- 10.5 Movimientos que requieren motivo y no lo tienen
SELECT m.id, m.fecha, m.producto_id, m.tipo
FROM movimientos_stock m
WHERE m.tipo IN ('AJUSTE', 'ROTURA', 'DEVOLUCION_CLIENTE', 'DEVOLUCION_PROVEEDOR')
  AND (m.motivo IS NULL OR LTRIM(RTRIM(m.motivo)) = '');

-- 10.6 Empleados que registraron movimientos en otra sucursal
SELECT m.id, m.fecha, u.username, su.codigo AS su_sucursal, sm.codigo AS sucursal_movimiento
FROM movimientos_stock m
JOIN usuarios u ON u.id = m.usuario_id
JOIN sucursales su ON su.id = u.sucursal_id
JOIN sucursales sm ON sm.id = m.sucursal_id
WHERE u.rol = 'EMPLEADO' AND u.sucursal_id <> m.sucursal_id;

-- 10.7 Cantidades con decimales en productos que se manejan por unidad
SELECT m.id, p.codigo, p.unidad_medida, m.cantidad
FROM movimientos_stock m
JOIN productos p ON p.id = m.producto_id
WHERE p.unidad_medida IN ('UNIDAD', 'CAJA', 'ROLLO')
  AND m.cantidad <> ROUND(m.cantidad, 0);
GO


/* ═══════════════════════════════════════════════════════════════════════
 * 11. ESQUEMA Y MIGRACIONES
 * ═══════════════════════════════════════════════════════════════════════ */

-- 11.1 Migraciones aplicadas por Flyway
SELECT installed_rank, version, description, installed_on, execution_time, success
FROM flyway_schema_history
ORDER BY installed_rank;

-- 11.2 Cantidad de filas por tabla
SELECT t.name AS tabla, SUM(ps.row_count) AS filas
FROM sys.tables t
JOIN sys.dm_db_partition_stats ps ON ps.object_id = t.object_id AND ps.index_id IN (0, 1)
GROUP BY t.name
ORDER BY t.name;

-- 11.3 Columnas de una tabla
DECLARE @tabla SYSNAME = 'stock_sucursal';
SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, NUMERIC_PRECISION, NUMERIC_SCALE,
       IS_NULLABLE, COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = @tabla
ORDER BY ORDINAL_POSITION;

-- 11.4 Restricciones CHECK (reglas de negocio que valida la base)
SELECT OBJECT_NAME(parent_object_id) AS tabla, name AS restriccion, definition
FROM sys.check_constraints
ORDER BY tabla, name;
GO
