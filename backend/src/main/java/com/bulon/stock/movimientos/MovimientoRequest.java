package com.bulon.stock.movimientos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** El usuario responsable no viaja en el body: sale de la sesión. */
public record MovimientoRequest(
        @NotNull Long productoId,
        @NotNull TipoMovimiento tipo,
        @NotNull Integer cantidad,
        @NotNull Long ubicacionId,
        Long ubicacionDestinoId,
        @Size(max = 255) String motivo,
        @Size(max = 255) String observacion
) {
}
