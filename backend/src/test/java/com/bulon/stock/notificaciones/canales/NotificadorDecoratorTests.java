package com.bulon.stock.notificaciones.canales;

import com.bulon.stock.alertas.AlertaStock;
import com.bulon.stock.usuarios.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Los decoradores por separado: un notificador falso registra lo que le llega. */
@ExtendWith(OutputCaptureExtension.class)
class NotificadorDecoratorTests {

    /** Hace de canal base: anota a quién se le envió. */
    private static class NotificadorFalso implements Notificador {
        final List<String> enviados = new ArrayList<>();

        @Override
        public void enviar(Usuario destinatario, AlertaStock alerta, String titulo, String mensaje) {
            enviados.add(destinatario.getUsername());
        }
    }

    @Test
    void elDecoradorDelegaEnElEnvueltoYAgregaElEmail(CapturedOutput salida) {
        NotificadorFalso base = new NotificadorFalso();

        new ConEmail(base).enviar(usuario("ventas", "ventas@bulon.com.ar"), null, "Stock bajo: Pinza", "Quedan 2");

        assertThat(base.enviados).containsExactly("ventas");                  // el canal envuelto se usó
        assertThat(salida).contains("[EMAIL] a ventas@bulon.com.ar: Stock bajo: Pinza - Quedan 2");
    }

    @Test
    void sinEmailSoloSaleElCanalEnvuelto(CapturedOutput salida) {
        NotificadorFalso base = new NotificadorFalso();

        new ConEmail(base).enviar(usuario("deposito", null), null, "Stock crítico: Pinza", "Quedan 2");

        assertThat(base.enviados).containsExactly("deposito");
        assertThat(salida).doesNotContain("[EMAIL]");
    }

    @Test
    void losDecoradoresSeApilan(CapturedOutput salida) {
        NotificadorFalso base = new NotificadorFalso();

        // Dos capas de email: cada una agrega su envío y el base se usa una sola vez
        new ConEmail(new ConEmail(base)).enviar(usuario("admin", "admin@bulon.com.ar"), null, "T", "M");

        assertThat(base.enviados).containsExactly("admin");
        assertThat(salida.getOut().split("\\[EMAIL]", -1)).hasSize(3);
    }

    private static Usuario usuario(String username, String email) {
        Usuario u = new Usuario();
        u.setUsername(username);
        u.setEmail(email);
        return u;
    }
}
