package com.bulon.stock.notificaciones;

import com.bulon.stock.alertas.AlertaObserver;
import com.bulon.stock.alertas.AlertaStock;
import com.bulon.stock.alertas.CambioAlerta;
import com.bulon.stock.alertas.NivelAlerta;
import com.bulon.stock.common.exception.ResourceNotFoundException;
import com.bulon.stock.usuarios.Usuario;
import com.bulon.stock.usuarios.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Observa las alertas (es un {@link AlertaObserver}): cuando se abre una o sube a CRITICO,
 * avisa a los empleados de esa sucursal y a los administradores.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificacionService implements AlertaObserver {

    /** La campanita muestra las últimas; las viejas quedan en la base. */
    private static final int MAXIMO = 30;

    private final NotificacionRepository notificacionRepository;
    private final UsuarioService usuarioService;

    @Override
    @Transactional
    public void alertaActualizada(AlertaStock alerta, CambioAlerta cambio) {
        // Que el stock se recupere no requiere que nadie haga nada
        if (cambio == CambioAlerta.RESUELTA) {
            return;
        }
        String titulo = (alerta.getNivel() == NivelAlerta.CRITICO ? "Stock crítico: " : "Reponer: ")
                + alerta.getProducto().getNombre();
        String mensaje = "Quedan " + numero(alerta.getCantidadAlGenerar()) + " en " + alerta.getSucursal().getNombre()
                + " (punto de pedido " + numero(alerta.getPuntoPedido())
                + ", mínimo " + numero(alerta.getStockMinimo()) + ").";

        for (Usuario destinatario : usuarioService.activosDeSucursal(alerta.getSucursal().getId())) {
            enviar(destinatario, alerta, titulo, mensaje);
        }
    }

    /** Canal in-app: queda en la tabla notificaciones y la muestra la campanita. */
    private void enviar(Usuario destinatario, AlertaStock alerta, String titulo, String mensaje) {
        Notificacion notificacion = new Notificacion();
        notificacion.setUsuario(destinatario);
        notificacion.setAlerta(alerta);
        notificacion.setTitulo(titulo);
        notificacion.setMensaje(mensaje);
        notificacionRepository.save(notificacion);
    }

    public BandejaDTO bandeja(Long usuarioId) {
        List<NotificacionDTO> ultimas = notificacionRepository
                .findByUsuarioIdOrderByCreadaDescIdDesc(usuarioId, Limit.of(MAXIMO)).stream()
                .map(NotificacionDTO::from)
                .toList();
        return new BandejaDTO(notificacionRepository.countByUsuarioIdAndLeidaFalse(usuarioId), ultimas);
    }

    /** Solo el destinatario puede marcarla: para otro usuario, la notificación "no existe". */
    @Transactional
    public NotificacionDTO marcarLeida(Long id, Long usuarioId) {
        Notificacion notificacion = notificacionRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Notificación", id));
        notificacion.setLeida(true);
        return NotificacionDTO.from(notificacion);
    }

    @Transactional
    public void marcarTodasLeidas(Long usuarioId) {
        notificacionRepository.marcarTodasLeidas(usuarioId);
    }

    private static String numero(BigDecimal valor) {
        return valor.stripTrailingZeros().toPlainString();
    }

    /** Lo que muestra la campanita: cuántas faltan leer y las últimas. */
    public record BandejaDTO(long noLeidas, List<NotificacionDTO> notificaciones) {
    }
}
