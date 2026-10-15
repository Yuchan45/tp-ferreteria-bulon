package com.bulon.stock.compras;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record OrdenCompraRequest(
        @NotNull Long proveedorId,
        @Size(max = 255) String observacion,
        @NotEmpty List<@Valid Item> items
) {

    public record Item(@NotNull Long productoId, @NotNull @Positive Integer cantidad) {
    }
}
