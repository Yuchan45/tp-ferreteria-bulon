package com.bulon.stock.movimientos;

/** Tipos de movimiento. Las reglas de cada uno están en su estrategia ({@code movimientos.estrategias}). */
public enum TipoMovimiento {
    /** Entra stock comprado a un proveedor (requiere proveedor). */
    INGRESO_COMPRA,
    /** Sale stock vendido. */
    SALIDA_VENTA,
    /** Un cliente devuelve mercadería: entra stock. */
    DEVOLUCION_CLIENTE,
    /** Se devuelve mercadería al proveedor: sale stock (requiere proveedor). */
    DEVOLUCION_PROVEEDOR,
    /** Rotura o pérdida: sale stock. */
    ROTURA,
    /** Corrección por recuento, con signo (+ o −). */
    AJUSTE
}
