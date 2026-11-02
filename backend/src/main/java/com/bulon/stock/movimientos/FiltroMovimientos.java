package com.bulon.stock.movimientos;

import java.time.LocalDate;

/**
 * Filtros del historial de movimientos; los null no filtran. Hace falta un producto o una
 * sucursal. {@code desde} y {@code hasta} incluyen ambos días; {@code q} busca por código o
 * nombre del producto, sin mayúsculas ni acentos.
 */
public record FiltroMovimientos(
        Long productoId,
        Long sucursalId,
        LocalDate desde,
        LocalDate hasta,
        TipoMovimiento tipo,
        String q
) {

    public static FiltroMovimientos deSucursal(Long sucursalId) {
        return new FiltroMovimientos(null, sucursalId, null, null, null, null);
    }

    public static FiltroMovimientos deProducto(Long productoId) {
        return new FiltroMovimientos(productoId, null, null, null, null, null);
    }
}
