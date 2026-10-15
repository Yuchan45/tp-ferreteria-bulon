package com.bulon.stock.alertas;

import lombok.RequiredArgsConstructor;

/** Notificación dentro del sistema: la alerta queda guardada y visible en /alertas y el panel. */
@RequiredArgsConstructor
public class AlertaBase implements Alerta {

    private final AlertaStockRepository alertaRepository;

    @Override
    public void enviar(AlertaStock alerta) {
        alertaRepository.save(alerta);
    }
}
