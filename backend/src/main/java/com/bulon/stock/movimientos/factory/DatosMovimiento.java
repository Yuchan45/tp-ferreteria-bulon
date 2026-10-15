package com.bulon.stock.movimientos.factory;

import com.bulon.stock.movimientos.TipoMovimiento;
import com.bulon.stock.productos.Producto;
import com.bulon.stock.ubicaciones.Ubicacion;
import com.bulon.stock.usuarios.Usuario;

/** Datos del request ya resueltos a entidades. {@code ubicacionDestino} es null si no se envió. */
public record DatosMovimiento(
        TipoMovimiento tipo,
        Producto producto,
        int cantidad,
        Ubicacion ubicacion,
        Ubicacion ubicacionDestino,
        Usuario usuario,
        String motivo,
        String observacion
) {
}
