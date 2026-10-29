package com.bulon.stock.movimientos;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Movimiento de stock. Al registrar por la API, {@code usuarioId} sale del token del
 * usuario logueado; {@code id} y {@code fecha} los completa el backend.
 */
public record MovimientoStockDTO(
        Long id,
        @NotNull Long productoId,
        @NotNull Long sucursalId,
        Long usuarioId,
        @NotNull TipoMovimiento tipo,
        @NotNull @Digits(integer = 9, fraction = 3) BigDecimal cantidad,
        LocalDateTime fecha,
        @Size(max = 255) String motivo,
        @Size(max = 500) String observaciones,
        @Size(max = 50) String referencia,
        Long proveedorId
) {

    /** Copia con el responsable indicado (el usuario logueado). */
    public MovimientoStockDTO conUsuario(Long responsableId) {
        return new MovimientoStockDTO(id, productoId, sucursalId, responsableId, tipo, cantidad, fecha,
                motivo, observaciones, referencia, proveedorId);
    }

    public static MovimientoStockDTO from(MovimientoStock m) {
        return new MovimientoStockDTO(
                m.getId(),
                m.getProducto().getId(),
                m.getSucursal().getId(),
                m.getUsuario().getId(),
                m.getTipo(),
                m.getCantidad(),
                m.getFecha(),
                m.getMotivo(),
                m.getObservaciones(),
                m.getReferencia(),
                m.getProveedor() != null ? m.getProveedor().getId() : null
        );
    }
}
