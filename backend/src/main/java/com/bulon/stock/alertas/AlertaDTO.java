package com.bulon.stock.alertas;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Alerta de reposición, con los datos del producto para mostrarla sin otra consulta. */
public record AlertaDTO(
        Long id,
        Long productoId,
        String codigo,
        String nombre,
        Long sucursalId,
        String sucursal,
        NivelAlerta nivel,
        EstadoAlerta estado,
        BigDecimal cantidadAlGenerar,
        BigDecimal puntoPedido,
        BigDecimal stockMinimo,
        LocalDateTime creada
) {

    public static AlertaDTO from(AlertaStock a) {
        return new AlertaDTO(
                a.getId(),
                a.getProducto().getId(),
                a.getProducto().getCodigo(),
                a.getProducto().getNombre(),
                a.getSucursal().getId(),
                a.getSucursal().getNombre(),
                a.getNivel(),
                a.getEstado(),
                a.getCantidadAlGenerar(),
                a.getPuntoPedido(),
                a.getStockMinimo(),
                a.getCreada()
        );
    }
}
