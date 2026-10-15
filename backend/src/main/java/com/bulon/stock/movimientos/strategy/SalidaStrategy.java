package com.bulon.stock.movimientos.strategy;

import com.bulon.stock.movimientos.MovimientoStock;
import com.bulon.stock.movimientos.TipoMovimiento;
import com.bulon.stock.stock.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

/** Venta o rotura: resta en la ubicación si hay stock suficiente ahí. */
@Component
@RequiredArgsConstructor
public class SalidaStrategy implements MovimientoStrategy {

    private final StockService stockService;

    @Override
    public Set<TipoMovimiento> tipos() {
        return Set.of(TipoMovimiento.SALIDA_VENTA, TipoMovimiento.ROTURA_PERDIDA);
    }

    @Override
    public void validar(MovimientoStock m) {
        stockService.verificarDisponible(m.getProducto(), m.getUbicacion(), m.getCantidad());
    }

    @Override
    public void actualizarStock(MovimientoStock m) {
        stockService.mover(m.getProducto(), m.getUbicacion(), -m.getCantidad());
    }
}
