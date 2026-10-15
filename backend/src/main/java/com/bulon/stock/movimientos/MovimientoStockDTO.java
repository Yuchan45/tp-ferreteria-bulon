package com.bulon.stock.movimientos;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record MovimientoStockDTO(
        Long id,
        @NotNull Long productoId,
        @NotNull TipoMovimiento tipo,
        @NotNull Integer cantidad,
        LocalDateTime fecha,
        String observacion
) {

    public static MovimientoStockDTO from(MovimientoStock m) {
        return new MovimientoStockDTO(
                m.getId(),
                m.getProducto().getId(),
                m.getTipo(),
                m.getCantidad(),
                m.getFecha(),
                m.getObservacion()
        );
    }
}
