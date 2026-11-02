package com.bulon.stock.movimientos.estrategias;

import com.bulon.stock.movimientos.TipoMovimiento;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Se devuelve mercadería al proveedor: sale stock y exige proveedor y motivo. */
@Component
class DevolucionProveedor extends EstrategiaBase {

    @Override
    public TipoMovimiento tipo() {
        return TipoMovimiento.DEVOLUCION_PROVEEDOR;
    }

    @Override
    protected boolean requiereMotivo() {
        return true;
    }

    @Override
    protected boolean requiereProveedor() {
        return true;
    }

    @Override
    public BigDecimal delta(BigDecimal cantidad) {
        return cantidad.negate();
    }
}
