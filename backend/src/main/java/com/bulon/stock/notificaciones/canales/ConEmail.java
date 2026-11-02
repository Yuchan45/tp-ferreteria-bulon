package com.bulon.stock.notificaciones.canales;

import com.bulon.stock.alertas.AlertaStock;
import com.bulon.stock.usuarios.Usuario;
import lombok.extern.slf4j.Slf4j;

/**
 * Decorador que suma el canal email. Para el TP el envío es simulado: se escribe en el
 * log lo que se mandaría. Un usuario sin email recibe solo los demás canales.
 */
@Slf4j
public class ConEmail extends NotificadorDecorator {

    public ConEmail(Notificador envuelto) {
        super(envuelto);
    }

    @Override
    public void enviar(Usuario destinatario, AlertaStock alerta, String titulo, String mensaje) {
        super.enviar(destinatario, alerta, titulo, mensaje);   // primero los canales envueltos
        if (destinatario.getEmail() == null || destinatario.getEmail().isBlank()) {
            return;
        }
        log.info("[EMAIL] a {}: {} - {}", destinatario.getEmail(), titulo, mensaje);
    }
}
