package com.bulon.stock.movimientos.strategy;

import com.bulon.stock.movimientos.MovimientoStock;
import com.bulon.stock.movimientos.TipoMovimiento;
import com.bulon.stock.stock.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

/** Resta en origen y suma en destino dentro de la misma transacción: o pasan las dos o ninguna. */
@Component
@RequiredArgsConstructor
public class TrasladoStrategy implements MovimientoStrategy {

    private final StockService stockService;

    @Override
    public Set<TipoMovimiento> tipos() {
        return Set.of(TipoMovimiento.TRASLADO);
    }

    @Override
    public void validar(MovimientoStock m) {
        stockService.verificarDisponible(m.getProducto(), m.getUbicacion(), m.getCantidad());
    }

    @Override
    public void actualizarStock(MovimientoStock m) {
        stockService.mover(m.getProducto(), m.getUbicacion(), -m.getCantidad());
        stockService.mover(m.getProducto(), m.getUbicacionDestino(), m.getCantidad());
    }
}
