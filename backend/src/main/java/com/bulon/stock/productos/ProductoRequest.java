package com.bulon.stock.productos;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record ProductoRequest(
        @NotBlank @Size(max = 50) String codigo,
        @NotBlank @Size(max = 120) String nombre,
        @Size(max = 200) String descripcion,
        @NotNull Long categoriaId,
        @NotNull UnidadMedida unidadMedida,
        @PositiveOrZero BigDecimal precio,
        @NotNull @PositiveOrZero Integer stockMinimo,
        @NotNull @PositiveOrZero Integer puntoPedido,
        Boolean activo,
        List<Long> proveedorIds,
        Long proveedorPreferidoId
) {
}
