package com.bulon.stock.movimientos.factory;

import com.bulon.stock.common.exception.BusinessException;
import com.bulon.stock.movimientos.MovimientoStock;
import com.bulon.stock.movimientos.TipoMovimiento;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Factory Method (Entrega 2, 3.2). Cada subclase decide cómo se arma un movimiento de sus tipos
 * (qué ubicaciones exige, qué signo admite la cantidad, si el motivo es obligatorio).
 * Agregar un tipo nuevo es agregar una subclase: el servicio no cambia.
 */
public abstract class MovimientoStockCreator {

    public abstract Set<TipoMovimiento> tipos();

    /** El método fábrica. */
    protected abstract MovimientoStock crearMovimiento(DatosMovimiento datos);

    public final MovimientoStock registrar(DatosMovimiento datos) {
        MovimientoStock movimiento = crearMovimiento(datos);
        movimiento.setTipo(datos.tipo());
        movimiento.setProducto(datos.producto());
        movimiento.setCantidad(datos.cantidad());
        movimiento.setUbicacion(datos.ubicacion());
        movimiento.setUsuario(datos.usuario());
        movimiento.setMotivo(blankANull(datos.motivo()));
        movimiento.setObservacion(blankANull(datos.observacion()));
        movimiento.setFecha(LocalDateTime.now());
        return movimiento;
    }

    protected static void exigirCantidadPositiva(DatosMovimiento datos) {
        if (datos.cantidad() <= 0) {
            throw new BusinessException("La cantidad debe ser mayor a cero");
        }
    }

    protected static void rechazarDestino(DatosMovimiento datos) {
        if (datos.ubicacionDestino() != null) {
            throw new BusinessException("Solo un TRASLADO lleva ubicación de destino");
        }
    }

    private static String blankANull(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
