package com.bulon.stock.movimientos;

import com.bulon.stock.usuarios.Rol;

/** Los 6 tipos de la Entrega 1 (4.2). El vendedor solo registra ventas y devoluciones. */
public enum TipoMovimiento {
    INGRESO_COMPRA(false),
    SALIDA_VENTA(true),
    DEVOLUCION(true),
    ROTURA_PERDIDA(false),
    AJUSTE(false),
    TRASLADO(false);

    private final boolean permitidoVendedor;

    TipoMovimiento(boolean permitidoVendedor) {
        this.permitidoVendedor = permitidoVendedor;
    }

    public boolean permitidoPara(Rol rol) {
        return rol != Rol.VENDEDOR || permitidoVendedor;
    }
}
