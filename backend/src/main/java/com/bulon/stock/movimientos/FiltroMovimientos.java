package com.bulon.stock.movimientos;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** Filtros del historial; todos opcionales. {@code ubicacionId} matchea origen o destino. */
public record FiltroMovimientos(
        Long productoId,
        TipoMovimiento tipo,
        Long usuarioId,
        Long ubicacionId,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
) {
}
