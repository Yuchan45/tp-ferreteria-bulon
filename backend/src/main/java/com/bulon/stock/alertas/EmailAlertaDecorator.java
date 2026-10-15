package com.bulon.stock.alertas;

import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * El email sale después del commit: si el movimiento se revierte no se avisa nada, y un SMTP lento
 * no retiene la transacción. Un email que falla se loguea y no afecta al movimiento.
 */
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
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    mandar(mensaje);
                }
            });
        } else {
            mandar(mensaje);
        }
    }

    private void mandar(SimpleMailMessage mensaje) {
        try {
            mailSender.send(mensaje);
        } catch (MailException e) {
            log.warn("No se pudo enviar el email de alerta a {}: {}", destinatario, e.getMessage());
        }
    }
}
