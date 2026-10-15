package com.bulon.stock.compras;

import jakarta.validation.constraints.NotNull;

public record RecepcionRequest(@NotNull Long ubicacionId) {
}
