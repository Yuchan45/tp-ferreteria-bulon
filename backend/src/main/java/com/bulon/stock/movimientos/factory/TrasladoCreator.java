package com.bulon.stock.movimientos.factory;

import com.bulon.stock.common.exception.BusinessException;
import com.bulon.stock.movimientos.MovimientoStock;
import com.bulon.stock.movimientos.TipoMovimiento;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class TrasladoCreator extends MovimientoStockCreator {

    @Override
    public Set<TipoMovimiento> tipos() {
        return Set.of(TipoMovimiento.TRASLADO);
    }

    @Override
    protected MovimientoStock crearMovimiento(DatosMovimiento datos) {
        exigirCantidadPositiva(datos);
        if (datos.ubicacionDestino() == null) {
            throw new BusinessException("Un traslado necesita ubicación de destino");
        }
        if (datos.ubicacionDestino().getId().equals(datos.ubicacion().getId())) {
            throw new BusinessException("El origen y el destino del traslado deben ser distintos");
        }
        MovimientoStock movimiento = new MovimientoStock();
        movimiento.setUbicacionDestino(datos.ubicacionDestino());
        return movimiento;
    }
}
