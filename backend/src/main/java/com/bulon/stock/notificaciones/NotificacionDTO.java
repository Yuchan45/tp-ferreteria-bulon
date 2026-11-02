package com.bulon.stock.notificaciones;

import com.bulon.stock.alertas.NivelAlerta;

import java.time.LocalDateTime;

/** Notificación del usuario; productoId y nivel salen de la alerta, para enlazar al producto. */
public record NotificacionDTO(
        Long id,
        String titulo,
        String mensaje,
        boolean leida,
        LocalDateTime creada,
        Long alertaId,
        Long productoId,
        NivelAlerta nivel
) {

    public static NotificacionDTO from(Notificacion n) {
        var alerta = n.getAlerta();
        return new NotificacionDTO(
                n.getId(),
                n.getTitulo(),
                n.getMensaje(),
                n.isLeida(),
                n.getCreada(),
                alerta != null ? alerta.getId() : null,
                alerta != null ? alerta.getProducto().getId() : null,
                alerta != null ? alerta.getNivel() : null
        );
    }
}
