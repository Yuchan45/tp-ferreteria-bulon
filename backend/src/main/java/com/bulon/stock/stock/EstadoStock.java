package com.bulon.stock.stock;

public enum EstadoStock {
    /** Por encima del punto de pedido. */
    OK,
    /** En o por debajo del punto de pedido: conviene pedir. */
    REPONER,
    /** En o por debajo del stock mínimo: piso de seguridad alcanzado. */
    CRITICO
}
