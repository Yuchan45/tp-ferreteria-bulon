package com.bulon.stock.productos;

import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Un proveedor de un producto. En la respuesta incluye la razón social y si el proveedor
 * está activo; al asignar se envían solo las condiciones (el proveedor va en la URL).
 */
public record ProductoProveedorDTO(
        Long proveedorId,
        String razonSocial,
        boolean preferido,
        String codigoProveedor,
        @PositiveOrZero BigDecimal precioCosto,
        @PositiveOrZero Integer plazoEntregaDias,
        /** Solo en la respuesta: a un proveedor inactivo no se le registran compras. */
        Boolean activo
) {

    public static ProductoProveedorDTO from(ProductoProveedor pp) {
        return new ProductoProveedorDTO(
                pp.getProveedor().getId(),
                pp.getProveedor().getRazonSocial(),
                pp.isPreferido(),
                pp.getCodigoProveedor(),
                pp.getPrecioCosto(),
                pp.getPlazoEntregaDias(),
                pp.getProveedor().isActivo()
        );
    }
}
