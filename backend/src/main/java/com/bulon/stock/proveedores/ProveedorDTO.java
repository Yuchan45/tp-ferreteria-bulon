package com.bulon.stock.proveedores;

public record ProveedorDTO(Long id, String razonSocial, String cuit, String telefono, String email) {

    public static ProveedorDTO from(Proveedor p) {
        return new ProveedorDTO(p.getId(), p.getRazonSocial(), p.getCuit(), p.getTelefono(), p.getEmail());
    }
}
