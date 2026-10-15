package com.bulon.stock.reportes;

/**
 * {@code puntoPedidoSugerido} = ventas diarias promedio × días de entrega del proveedor + stock mínimo.
 */
public record RotacionDTO(Long productoId, String codigo, String nombre, long unidadesVendidas,
                          int stockTotal, int puntoPedido, int puntoPedidoSugerido) {
}
