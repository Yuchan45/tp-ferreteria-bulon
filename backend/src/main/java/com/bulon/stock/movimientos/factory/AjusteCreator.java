package com.bulon.stock.movimientos.factory;

import com.bulon.stock.common.exception.BusinessException;
import com.bulon.stock.movimientos.MovimientoStock;
import com.bulon.stock.movimientos.TipoMovimiento;
import org.springframework.stereotype.Component;

import java.util.Set;

/** Ajuste de recuento: la cantidad lleva signo y el motivo es obligatorio (Entrega 1, 4.2). */
@Component
public class AjusteCreator extends MovimientoStockCreator {

    @Override
    public Set<TipoMovimiento> tipos() {
        return Set.of(TipoMovimiento.AJUSTE);
    }

    @Override
    protected MovimientoStock crearMovimiento(DatosMovimiento datos) {
        if (datos.cantidad() == 0) {
            throw new BusinessException("Un ajuste debe sumar o restar al menos una unidad");
        }
        if (datos.motivo() == null || datos.motivo().isBlank()) {
            throw new BusinessException("El motivo es obligatorio en un ajuste");
        }
        rechazarDestino(datos);
        return new MovimientoStock();
    }
}
