package com.bulon.stock.movimientos;

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
    AJUSTE;

    public boolean requiereProveedor() {
        return this == INGRESO_COMPRA || this == DEVOLUCION_PROVEEDOR;
    }

    public boolean requiereMotivo() {
        return this == AJUSTE || this == ROTURA || this == DEVOLUCION_CLIENTE || this == DEVOLUCION_PROVEEDOR;
    }
}
