package com.bulon.stock.movimientos.estrategias;

import com.bulon.stock.movimientos.TipoMovimiento;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Un cliente devuelve mercadería: entra stock y se registra el motivo. */
@Component
class DevolucionCliente extends EstrategiaBase {

    @Override
    public TipoMovimiento tipo() {
        return TipoMovimiento.DEVOLUCION_CLIENTE;
    }

    @Override
    protected boolean requiereMotivo() {
        return true;
    }

    @Override
    public BigDecimal delta(BigDecimal cantidad) {
        return cantidad;
    }
}
