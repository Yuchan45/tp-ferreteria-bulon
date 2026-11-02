package com.bulon.stock.stock;

import java.math.BigDecimal;

/** Stock de un producto en una sucursal, con su estado calculado. */
public record StockSucursalDTO(
        Long productoId,
        String codigo,
        String nombre,
        Long sucursalId,
        String sucursal,
        BigDecimal cantidad,
        BigDecimal stockMinimo,
        BigDecimal puntoPedido,
        EstadoStock estado
) {

    public static StockSucursalDTO from(StockSucursal s) {
        return new StockSucursalDTO(
                s.getProducto().getId(),
                s.getProducto().getCodigo(),
                s.getProducto().getNombre(),
                s.getSucursal().getId(),
                s.getSucursal().getNombre(),
                s.getCantidad(),
                s.getStockMinimo(),
                s.getPuntoPedido(),
                s.estado()
        );
    }
}
