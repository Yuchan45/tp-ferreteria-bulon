package com.bulon.stock.movimientos.estrategias;

import com.bulon.stock.movimientos.TipoMovimiento;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Entra stock comprado: exige el proveedor que lo vendió. */
@Component
class IngresoCompra extends EstrategiaBase {

    @Override
    public TipoMovimiento tipo() {
        return TipoMovimiento.INGRESO_COMPRA;
    }

    @Override
    protected boolean requiereProveedor() {
        return true;
    }

    @Override
    public BigDecimal delta(BigDecimal cantidad) {
        return cantidad;
    }
}
