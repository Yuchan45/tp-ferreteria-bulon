package com.bulon.stock.movimientos.estrategias;

import com.bulon.stock.movimientos.TipoMovimiento;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Rotura o pérdida: sale stock y se registra el motivo. */
@Component
class Rotura extends EstrategiaBase {

    @Override
    public TipoMovimiento tipo() {
        return TipoMovimiento.ROTURA;
    }

    @Override
    protected boolean requiereMotivo() {
        return true;
    }

    @Override
    public BigDecimal delta(BigDecimal cantidad) {
        return cantidad.negate();
    }
}
