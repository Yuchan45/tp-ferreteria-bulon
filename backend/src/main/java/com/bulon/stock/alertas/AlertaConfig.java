package com.bulon.stock.alertas;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
public class AlertaConfig {

    /** Email solo si hay servidor SMTP (spring.mail.host) y destinatario (ALERTAS_EMAIL). */
    @Bean
    Alerta alerta(AlertaStockRepository alertaRepository,
                  ObjectProvider<JavaMailSender> mailSender,
                  @Value("${app.alertas.email:}") String destinatario) {
        Alerta alerta = new LogAlertaDecorator(new AlertaBase(alertaRepository));
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender != null && !destinatario.isBlank()) {
            alerta = new EmailAlertaDecorator(alerta, sender, destinatario);
        }
        return alerta;
    }
}
