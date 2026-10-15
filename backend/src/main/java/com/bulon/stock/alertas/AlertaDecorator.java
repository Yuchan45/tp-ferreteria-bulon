package com.bulon.stock.alertas;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class AlertaDecorator implements Alerta {

    private final Alerta envuelta;

    @Override
    public void enviar(AlertaStock alerta) {
        envuelta.enviar(alerta);
    }
}
