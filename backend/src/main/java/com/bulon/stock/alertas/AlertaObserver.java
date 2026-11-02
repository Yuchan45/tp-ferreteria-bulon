package com.bulon.stock.alertas;

/**
 * Patrón Observer (segundo nivel): quien necesite enterarse de las alertas implementa esta
 * interfaz. {@link AlertaService} es el Subject: observa el stock y, cuando abre o agrava
 * una alerta, avisa a estos observadores (hoy, las notificaciones).
 */
public interface AlertaObserver {

    void alertaActualizada(AlertaStock alerta, CambioAlerta cambio);
}
