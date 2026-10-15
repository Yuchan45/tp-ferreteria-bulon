package com.bulon.stock.productos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ProductoDTO(
        Long id,
        @NotBlank String codigo,
        @NotBlank String descripcion,
        @PositiveOrZero BigDecimal precio,
        Integer stockActual,
        @PositiveOrZero Integer stockMinimo,
        Long proveedorId
) {

    public static ProductoDTO from(Producto p) {
        return new ProductoDTO(
                p.getId(),
                p.getCodigo(),
                p.getDescripcion(),
                p.getPrecio(),
                p.getStockActual(),
                p.getStockMinimo(),
                p.getProveedor() != null ? p.getProveedor().getId() : null
        );
    }
}
