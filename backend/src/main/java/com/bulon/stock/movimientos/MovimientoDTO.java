package com.bulon.stock.movimientos;

import java.time.LocalDateTime;

public record MovimientoDTO(
        Long id,
        LocalDateTime fecha,
        TipoMovimiento tipo,
        Long productoId,
        String productoCodigo,
        String productoNombre,
        int cantidad,
        Long ubicacionId,
        String ubicacion,
        Long ubicacionDestinoId,
        String ubicacionDestino,
        String usuario,
        String motivo,
        String observacion
) {

    public static MovimientoDTO from(MovimientoStock m) {
        return new MovimientoDTO(
                m.getId(),
                m.getFecha(),
                m.getTipo(),
                m.getProducto().getId(),
                m.getProducto().getCodigo(),
                m.getProducto().getNombre(),
                m.getCantidad(),
                m.getUbicacion().getId(),
                m.getUbicacion().getNombre(),
                m.getUbicacionDestino() != null ? m.getUbicacionDestino().getId() : null,
                m.getUbicacionDestino() != null ? m.getUbicacionDestino().getNombre() : null,
                m.getUsuario().getUsername(),
                m.getMotivo(),
                m.getObservacion()
        );
    }
}
