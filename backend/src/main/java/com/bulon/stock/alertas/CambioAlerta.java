package com.bulon.stock.alertas;

/** Qué le pasó a una alerta, para que cada observador decida si le interesa. */
public enum CambioAlerta {
    /** Se generó una alerta nueva. */
    ABIERTA,
    /** Una alerta REPONER pasó a CRITICO. */
    ESCALADA,
    /** El stock se recuperó y la alerta se cerró. */
    RESUELTA
}
