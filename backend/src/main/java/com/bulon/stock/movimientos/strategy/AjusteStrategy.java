package com.bulon.stock.movimientos.strategy;

import com.bulon.stock.movimientos.MovimientoStock;
import com.bulon.stock.movimientos.TipoMovimiento;
import com.bulon.stock.stock.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

/** Suma o resta según el signo; un ajuste negativo no puede dejar la ubicación en negativo. */
@Component
@RequiredArgsConstructor
public class AjusteStrategy implements MovimientoStrategy {

    private final StockService stockService;

    @Override
    public Set<TipoMovimiento> tipos() {
        return Set.of(TipoMovimiento.AJUSTE);
    }

    @Override
    public void validar(MovimientoStock m) {
        if (m.getCantidad() < 0) {
            stockService.verificarDisponible(m.getProducto(), m.getUbicacion(), -m.getCantidad());
        }
    }

    @Override
    public void actualizarStock(MovimientoStock m) {
        stockService.mover(m.getProducto(), m.getUbicacion(), m.getCantidad());
    }
}
