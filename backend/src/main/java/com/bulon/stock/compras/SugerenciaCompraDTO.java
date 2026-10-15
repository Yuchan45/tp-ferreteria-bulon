package com.bulon.stock.compras;

import com.bulon.stock.productos.EstadoStock;

import java.util.List;

/** Productos pendientes de reposición agrupados por proveedor preferido (null = sin preferido). */
public record SugerenciaCompraDTO(Long proveedorId, String proveedor, List<Item> items) {

    public record Item(Long productoId, String productoCodigo, String productoNombre, EstadoStock estado,
                       int stockTotal, int puntoPedido, int cantidadSugerida) {
    }
}
