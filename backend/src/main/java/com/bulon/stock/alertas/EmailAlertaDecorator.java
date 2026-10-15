package com.bulon.stock.alertas;

import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/** Un email que falla no debe revertir el movimiento que disparó la alerta: se loguea y sigue. */
@Slf4j
public class EmailAlertaDecorator extends AlertaDecorator {

    private final JavaMailSender mailSender;
    private final String destinatario;

    public EmailAlertaDecorator(Alerta envuelta, JavaMailSender mailSender, String destinatario) {
        super(envuelta);
        this.mailSender = mailSender;
        this.destinatario = destinatario;
    }

    @Override
    public void enviar(AlertaStock alerta) {
        super.enviar(alerta);
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(destinatario);
        mensaje.setSubject("[Bulón] Reponer " + alerta.getProducto().getCodigo());
        mensaje.setText(alerta.mensaje());
        try {
            mailSender.send(mensaje);
        } catch (MailException e) {
            log.warn("No se pudo enviar el email de alerta a {}: {}", destinatario, e.getMessage());
        }
    }
}
