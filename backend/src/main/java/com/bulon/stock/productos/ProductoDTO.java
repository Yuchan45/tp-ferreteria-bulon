package com.bulon.stock.productos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Producto del catálogo. {@code stockTotal} es de solo lectura: la suma del stock
 * de todas las sucursales (el detalle por sucursal está en /productos/{id}/stock).
 */
public record ProductoDTO(
        Long id,
        @NotBlank @Size(max = 50) String codigo,
        @NotBlank @Size(max = 100) String nombre,
        @Size(max = 500) String descripcion,
        Long categoriaId,
        UnidadMedida unidadMedida,
        @PositiveOrZero BigDecimal precio,
        Boolean activo,
        BigDecimal stockTotal
) {

    public static ProductoDTO from(Producto p, BigDecimal stockTotal) {
        return new ProductoDTO(
                p.getId(),
                p.getCodigo(),
                p.getNombre(),
                p.getDescripcion(),
                p.getCategoria() != null ? p.getCategoria().getId() : null,
                p.getUnidadMedida(),
                p.getPrecio(),
                p.isActivo(),
                stockTotal
        );
    }
}
