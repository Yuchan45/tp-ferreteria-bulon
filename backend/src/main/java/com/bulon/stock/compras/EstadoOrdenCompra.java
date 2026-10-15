package com.bulon.stock.compras;

/** BORRADOR → ENVIADA → RECIBIDA. Se puede cancelar mientras no esté recibida. */
public enum EstadoOrdenCompra {
    BORRADOR,
    ENVIADA,
    RECIBIDA,
    CANCELADA;

    public boolean puedePasarA(EstadoOrdenCompra destino) {
        return switch (this) {
            case BORRADOR -> destino == ENVIADA || destino == CANCELADA;
            case ENVIADA -> destino == RECIBIDA || destino == CANCELADA;
            case RECIBIDA, CANCELADA -> false;
        };
    }
}
