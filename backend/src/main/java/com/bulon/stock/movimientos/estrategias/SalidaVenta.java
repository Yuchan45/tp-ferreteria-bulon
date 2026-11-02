package com.bulon.stock.movimientos.estrategias;

import com.bulon.stock.movimientos.TipoMovimiento;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Sale stock vendido: no exige motivo ni proveedor. */
@Component
class SalidaVenta extends EstrategiaBase {

    @Override
    public TipoMovimiento tipo() {
        return TipoMovimiento.SALIDA_VENTA;
    }

    @Override
    public BigDecimal delta(BigDecimal cantidad) {
        return cantidad.negate();
    }
}
