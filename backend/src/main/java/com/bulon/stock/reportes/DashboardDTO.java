package com.bulon.stock.reportes;

import com.bulon.stock.movimientos.MovimientoDTO;
import com.bulon.stock.stock.UnidadesPorUbicacion;

import java.util.List;

public record DashboardDTO(
        long productosActivos,
        long pendientesReposicion,
        long bajoMinimo,
        long alertasAbiertas,
        long movimientosHoy,
        List<UnidadesPorUbicacion> unidadesPorUbicacion,
        List<MovimientoDTO> ultimosMovimientos
) {
}
