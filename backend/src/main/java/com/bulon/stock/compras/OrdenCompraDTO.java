package com.bulon.stock.compras;

import java.time.LocalDateTime;
import java.util.List;

public record OrdenCompraDTO(
        Long id,
        Long proveedorId,
        String proveedor,
        EstadoOrdenCompra estado,
        String usuario,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaRecepcion,
        String ubicacionRecepcion,
        String observacion,
        List<Item> items
) {

    public record Item(Long productoId, String productoCodigo, String productoNombre, int cantidad) {
    }

    public static OrdenCompraDTO from(OrdenCompra o) {
        return new OrdenCompraDTO(
                o.getId(),
                o.getProveedor().getId(),
                o.getProveedor().getRazonSocial(),
                o.getEstado(),
                o.getUsuario().getUsername(),
                o.getFechaCreacion(),
                o.getFechaRecepcion(),
                o.getUbicacionRecepcion() != null ? o.getUbicacionRecepcion().getNombre() : null,
                o.getObservacion(),
                o.getItems().stream()
                        .map(i -> new Item(i.getProducto().getId(), i.getProducto().getCodigo(),
                                i.getProducto().getNombre(), i.getCantidad()))
                        .toList()
        );
    }
}
