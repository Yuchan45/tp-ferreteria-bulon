package com.bulon.stock.alertas;

public enum EstadoAlerta {
    /** Recién generada, nadie la atendió. */
    ABIERTA,
    /** Hay una solicitud de reposición en curso. */
    EN_GESTION,
    /** El stock volvió a superar el punto de pedido. */
    RESUELTA
}
