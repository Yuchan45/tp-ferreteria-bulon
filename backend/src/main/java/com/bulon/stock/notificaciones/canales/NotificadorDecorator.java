package com.bulon.stock.notificaciones.canales;

import com.bulon.stock.alertas.AlertaStock;
import com.bulon.stock.usuarios.Usuario;

/**
 * Decorador base: envuelve a otro {@link Notificador} y le delega el envío. Cada
 * decorador concreto llama a {@code super.enviar(...)} y después agrega su canal,
 * así los canales se combinan apilando envoltorios: {@code new ConEmail(new NotificadorSistema(...))}.
 */
public abstract class NotificadorDecorator implements Notificador {

    private final Notificador envuelto;

    protected NotificadorDecorator(Notificador envuelto) {
        this.envuelto = envuelto;
    }

    @Override
    public void enviar(Usuario destinatario, AlertaStock alerta, String titulo, String mensaje) {
        envuelto.enviar(destinatario, alerta, titulo, mensaje);
    }
}
