package com.bulon.stock.movimientos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Un movimiento listo para mostrar: con los datos del producto, del responsable y del
 * proveedor, su efecto con signo sobre el stock y el stock que quedó después.
 */
public record MovimientoDetalleDTO(
        Long id,
        Long productoId,
        String codigo,
        String nombre,
        Long sucursalId,
        Long usuarioId,
        String usuario,
        TipoMovimiento tipo,
        BigDecimal cantidad,
        /** Cuánto cambió el stock: positivo si entró, negativo si salió. */
        BigDecimal efecto,
        /** Stock de la sucursal inmediatamente después del movimiento. */
        BigDecimal stockResultante,
        LocalDateTime fecha,
        String motivo,
        String observaciones,
        String referencia,
        Long proveedorId,
        String proveedor
) {

    static MovimientoDetalleDTO from(MovimientoStock m, BigDecimal efecto, BigDecimal stockResultante) {
        return new MovimientoDetalleDTO(
                m.getId(),
                m.getProducto().getId(),
                m.getProducto().getCodigo(),
                m.getProducto().getNombre(),
                m.getSucursal().getId(),
                m.getUsuario().getId(),
                m.getUsuario().getNombre(),
                m.getTipo(),
                m.getCantidad(),
                efecto,
                stockResultante,
                m.getFecha(),
                m.getMotivo(),
                m.getObservaciones(),
                m.getReferencia(),
                m.getProveedor() != null ? m.getProveedor().getId() : null,
                m.getProveedor() != null ? m.getProveedor().getRazonSocial() : null
        );
    }
}
