package com.bulon.stock.alertas;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LogAlertaDecorator extends AlertaDecorator {

    public LogAlertaDecorator(Alerta envuelta) {
        super(envuelta);
    }

    @Override
    public void enviar(AlertaStock alerta) {
        super.enviar(alerta);
        log.warn("Alerta de reposición: {}", alerta.mensaje());
    }
}
