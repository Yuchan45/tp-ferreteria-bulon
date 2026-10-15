package com.bulon.stock.movimientos.strategy;

import com.bulon.stock.movimientos.MovimientoStock;
import com.bulon.stock.movimientos.TipoMovimiento;

import java.util.Set;

/**
 * Strategy (Entrega 2, 3.3): cada tipo de movimiento valida y actualiza el stock a su manera.
 * {@code validar} lanza {@code BusinessException} si el movimiento no se puede aplicar.
 */
public interface MovimientoStrategy {

    Set<TipoMovimiento> tipos();

    void validar(MovimientoStock movimiento);

    void actualizarStock(MovimientoStock movimiento);
}
