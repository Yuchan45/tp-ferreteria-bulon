package com.bulon.stock.notificaciones;

import com.bulon.stock.alertas.AlertaObserver;
import com.bulon.stock.alertas.AlertaStock;
import com.bulon.stock.alertas.CambioAlerta;
import com.bulon.stock.alertas.NivelAlerta;
import com.bulon.stock.common.exception.ResourceNotFoundException;
import com.bulon.stock.notificaciones.canales.ConEmail;
import com.bulon.stock.notificaciones.canales.Notificador;
import com.bulon.stock.notificaciones.canales.NotificadorSistema;
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
 * avisa a los empleados de esa sucursal y a los administradores, por los canales que
 * corresponden al nivel de la alerta (ver {@link #notificadorPara}).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificacionService implements AlertaObserver {

    /** La campanita muestra las últimas; las viejas quedan en la base. */
    private static final int MAXIMO = 30;

    private final NotificacionRepository notificacionRepository;
    private final UsuarioService usuarioService;
    /** Canal base (campanita), que los decoradores envuelven. */
    private final NotificadorSistema sistema;

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

        Notificador notificador = notificadorPara(alerta.getNivel());
        for (Usuario destinatario : usuarioService.activosDeSucursal(alerta.getSucursal().getId())) {
            notificador.enviar(destinatario, alerta, titulo, mensaje);
        }
    }

    /**
     * Patrón Decorator: los canales se combinan envolviendo al notificador base.
     * Lo urgente sale por más canales: REPONER → campanita; CRITICO → campanita + email.
     */
    Notificador notificadorPara(NivelAlerta nivel) {
        return nivel == NivelAlerta.CRITICO ? new ConEmail(sistema) : sistema;
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
