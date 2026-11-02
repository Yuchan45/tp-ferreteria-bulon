package com.bulon.stock.notificaciones.canales;

import com.bulon.stock.alertas.AlertaStock;
import com.bulon.stock.notificaciones.Notificacion;
import com.bulon.stock.notificaciones.NotificacionRepository;
import com.bulon.stock.usuarios.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Componente base del Decorator: el canal que siempre se usa. Guarda la notificación
 * en la tabla notificaciones y el frontend la muestra en la campanita.
 */
@Component
@RequiredArgsConstructor
public class NotificadorSistema implements Notificador {

    private final NotificacionRepository notificacionRepository;

    @Override
    public void enviar(Usuario destinatario, AlertaStock alerta, String titulo, String mensaje) {
        Notificacion notificacion = new Notificacion();
        notificacion.setUsuario(destinatario);
        notificacion.setAlerta(alerta);
        notificacion.setTitulo(titulo);
        notificacion.setMensaje(mensaje);
        notificacionRepository.save(notificacion);
    }
}
