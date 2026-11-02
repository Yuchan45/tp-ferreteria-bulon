package com.bulon.stock.stock;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** Umbrales de reposición de un producto en una sucursal. */
public record UmbralesRequest(
        @NotNull @PositiveOrZero @Digits(integer = 9, fraction = 3) BigDecimal stockMinimo,
        @NotNull @PositiveOrZero @Digits(integer = 9, fraction = 3) BigDecimal puntoPedido
) {
}
