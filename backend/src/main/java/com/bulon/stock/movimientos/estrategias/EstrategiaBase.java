package com.bulon.stock.movimientos.estrategias;

import com.bulon.stock.common.exception.BusinessException;
import com.bulon.stock.movimientos.MovimientoStockDTO;

import java.math.BigDecimal;

/**
 * Validación que comparten todas las estrategias. Cada tipo solo declara lo que lo
 * diferencia (si exige motivo o proveedor) y, si hace falta, redefine una regla.
 */
abstract class EstrategiaBase implements EstrategiaMovimiento {

    protected boolean requiereMotivo() {
        return false;
    }

    protected boolean requiereProveedor() {
        return false;
    }

    @Override
    public void validar(MovimientoStockDTO dto) {
        validarCantidad(dto.cantidad());
        if (requiereMotivo() && (dto.motivo() == null || dto.motivo().isBlank())) {
            throw new BusinessException("El motivo es obligatorio para " + tipo());
        }
        if (requiereProveedor() && dto.proveedorId() == null) {
            throw new BusinessException("El proveedor es obligatorio para " + tipo());
        }
        if (!requiereProveedor() && dto.proveedorId() != null) {
            throw new BusinessException("El proveedor solo se indica en compras y devoluciones a proveedor");
        }
    }

    /** Por defecto la cantidad es positiva y el tipo decide si suma o resta. */
    protected void validarCantidad(BigDecimal cantidad) {
        if (cantidad.signum() <= 0) {
            throw new BusinessException("La cantidad debe ser mayor a cero");
        }
    }
}
