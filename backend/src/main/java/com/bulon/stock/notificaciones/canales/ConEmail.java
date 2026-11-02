package com.bulon.stock.notificaciones.canales;

import com.bulon.stock.alertas.AlertaStock;
import com.bulon.stock.usuarios.Usuario;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.ansi.AnsiStyle;

/**
 * Decorador que suma el canal email. Para el TP el envío es simulado: se escribe en el
 * log lo que se mandaría, resaltado en color para que se vea en la demo (si la consola
 * no admite colores, sale como texto común). Un usuario sin email recibe solo los demás canales.
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
        log.info("{}", AnsiOutput.toString(AnsiStyle.BOLD, AnsiColor.BRIGHT_MAGENTA,
                ">>> [EMAIL] a " + destinatario.getEmail() + ": " + titulo + " - " + mensaje));
    }
}
