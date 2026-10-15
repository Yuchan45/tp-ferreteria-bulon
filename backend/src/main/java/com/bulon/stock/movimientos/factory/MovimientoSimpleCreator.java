package com.bulon.stock.movimientos.factory;

import com.bulon.stock.movimientos.MovimientoStock;
import com.bulon.stock.movimientos.TipoMovimiento;
import org.springframework.stereotype.Component;

import java.util.Set;

/** Movimientos sobre una sola ubicación y con cantidad positiva. */
@Component
public class MovimientoSimpleCreator extends MovimientoStockCreator {

    @Override
    public Set<TipoMovimiento> tipos() {
        return Set.of(TipoMovimiento.INGRESO_COMPRA, TipoMovimiento.SALIDA_VENTA,
                TipoMovimiento.DEVOLUCION, TipoMovimiento.ROTURA_PERDIDA);
    }

    @Override
    protected MovimientoStock crearMovimiento(DatosMovimiento datos) {
        exigirCantidadPositiva(datos);
        rechazarDestino(datos);
        return new MovimientoStock();
    }
}
