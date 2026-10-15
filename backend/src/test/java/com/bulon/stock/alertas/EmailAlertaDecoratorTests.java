package com.bulon.stock.alertas;

import com.bulon.stock.productos.EstadoStock;
import com.bulon.stock.productos.Producto;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EmailAlertaDecoratorTests {

    private final List<AlertaStock> registradas = new ArrayList<>();
    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final Alerta canal = new EmailAlertaDecorator(new LogAlertaDecorator(registradas::add), mailSender,
            "deposito@bulon.com.ar");

    private static AlertaStock alerta() {
        Producto producto = new Producto();
        producto.setCodigo("BUL-002");
        producto.setNombre("Bulón 3/8");
        producto.setStockMinimo(100);
        AlertaStock alerta = new AlertaStock();
        alerta.setProducto(producto);
        alerta.setEstado(EstadoStock.BAJO_MINIMO);
        alerta.setStock(60);
        return alerta;
    }

    @Test
    void registraLaAlertaYEnviaUnEmailConElMensaje() {
        AlertaStock alerta = alerta();

        canal.enviar(alerta);

        ArgumentCaptor<SimpleMailMessage> email = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(email.capture());
        assertThat(registradas).containsExactly(alerta);
        assertThat(email.getValue().getTo()).containsExactly("deposito@bulon.com.ar");
        assertThat(email.getValue().getSubject()).isEqualTo("[Bulón] Reponer BUL-002");
        assertThat(email.getValue().getText())
                .isEqualTo("BUL-002 · Bulón 3/8: quedan 60 unidades, bajo el stock mínimo (100)");
    }

    @Test
    void unEmailQueFallaNoImpideRegistrarLaAlerta() {
        doThrow(new MailSendException("SMTP caído")).when(mailSender).send(any(SimpleMailMessage.class));

        canal.enviar(alerta());

        assertThat(registradas).hasSize(1);
    }
}
