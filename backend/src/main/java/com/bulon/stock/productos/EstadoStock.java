package com.bulon.stock.productos;

/** Situación del stock total de un producto frente a su stock mínimo y su punto de pedido. */
public enum EstadoStock {
    /** Stock total en o por debajo del stock mínimo: reponer con urgencia. */
    BAJO_MINIMO,
    /** Stock total en o por debajo del punto de pedido: momento de pedir al proveedor. */
    PUNTO_PEDIDO,
    OK;

    public static EstadoStock de(int stockTotal, int stockMinimo, int puntoPedido) {
        if (stockTotal <= stockMinimo) {
            return BAJO_MINIMO;
        }
        return stockTotal <= puntoPedido ? PUNTO_PEDIDO : OK;
    }

    /** "Pendiente de reposición" de la Entrega 1 (4.5). */
    public boolean pendienteReposicion() {
        return this != OK;
    }
}
