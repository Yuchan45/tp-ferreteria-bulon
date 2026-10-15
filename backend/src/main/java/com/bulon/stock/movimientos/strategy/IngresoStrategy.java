package com.bulon.stock.movimientos.strategy;

import com.bulon.stock.movimientos.MovimientoStock;
import com.bulon.stock.movimientos.TipoMovimiento;
import com.bulon.stock.stock.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

/** Compra o devolución de un cliente: suma en la ubicación. */
@Component
@RequiredArgsConstructor
public class IngresoStrategy implements MovimientoStrategy {

    private final StockService stockService;

    @Override
    public Set<TipoMovimiento> tipos() {
        return Set.of(TipoMovimiento.INGRESO_COMPRA, TipoMovimiento.DEVOLUCION);
    }

    @Override
    public void validar(MovimientoStock movimiento) {
    }

    @Override
    public void actualizarStock(MovimientoStock m) {
        stockService.mover(m.getProducto(), m.getUbicacion(), m.getCantidad());
    }
}
