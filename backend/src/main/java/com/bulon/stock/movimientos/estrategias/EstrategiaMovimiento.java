package com.bulon.stock.movimientos.estrategias;

import com.bulon.stock.movimientos.MovimientoStockDTO;
import com.bulon.stock.movimientos.TipoMovimiento;

import java.math.BigDecimal;

/**
 * Patrón Strategy: cada tipo de movimiento sabe cómo se valida y cómo afecta al stock.
 * El service no pregunta el tipo; le pide a la estrategia que corresponda que lo resuelva.
 */
public interface EstrategiaMovimiento {

    /** Tipo de movimiento que resuelve esta estrategia. */
    TipoMovimiento tipo();

    /** Reglas propias del tipo: cantidad, motivo y proveedor. */
    void validar(MovimientoStockDTO dto);

    /** Cuánto cambia el stock: positivo suma, negativo resta. */
    BigDecimal delta(BigDecimal cantidad);
}
