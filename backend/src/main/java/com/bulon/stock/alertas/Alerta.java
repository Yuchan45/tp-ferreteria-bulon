package com.bulon.stock.alertas;

/**
 * Decorator (Entrega 2, 3.5): canal por el que sale una alerta. {@link AlertaBase} la registra en el
 * sistema y los decoradores le suman comportamiento (log, email) sin una subclase por combinación.
 * La cadena se arma en {@link AlertaConfig}.
 */
public interface Alerta {

    void enviar(AlertaStock alerta);
}
