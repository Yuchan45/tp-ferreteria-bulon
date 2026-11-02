package com.bulon.stock.movimientos.estrategias;

import com.bulon.stock.common.exception.BusinessException;
import com.bulon.stock.movimientos.TipoMovimiento;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Corrección por recuento: la cantidad trae su signo (+ sobrante, − faltante),
 * por eso redefine la regla de cantidad de la clase base.
 */
@Component
class Ajuste extends EstrategiaBase {

    @Override
    public TipoMovimiento tipo() {
        return TipoMovimiento.AJUSTE;
    }

    @Override
    protected boolean requiereMotivo() {
        return true;
    }

    @Override
    protected void validarCantidad(BigDecimal cantidad) {
        if (cantidad.signum() == 0) {
            throw new BusinessException("El ajuste no puede ser 0");
        }
    }

    @Override
    public BigDecimal delta(BigDecimal cantidad) {
        return cantidad;
    }
}
