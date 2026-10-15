package com.bulon.stock.productos;

import com.bulon.stock.proveedores.Proveedor;
import com.bulon.stock.stock.ExistenciaDTO;

import java.math.BigDecimal;
import java.util.List;

public record ProductoDTO(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        Long categoriaId,
        String categoria,
        UnidadMedida unidadMedida,
        BigDecimal precio,
        int stockMinimo,
        int puntoPedido,
        boolean activo,
        List<Long> proveedorIds,
        Long proveedorPreferidoId,
        List<ExistenciaDTO> existencias,
        int stockTotal,
        EstadoStock estadoStock
) {

    public static ProductoDTO from(Producto p, List<ExistenciaDTO> existencias) {
        int total = existencias.stream().mapToInt(ExistenciaDTO::cantidad).sum();
        return new ProductoDTO(
                p.getId(),
                p.getCodigo(),
                p.getNombre(),
                p.getDescripcion(),
                p.getCategoria().getId(),
                p.getCategoria().getNombre(),
                p.getUnidadMedida(),
                p.getPrecio(),
                p.getStockMinimo(),
                p.getPuntoPedido(),
                p.isActivo(),
                p.getProveedores().stream().map(Proveedor::getId).sorted().toList(),
                p.getProveedorPreferido() != null ? p.getProveedorPreferido().getId() : null,
                existencias,
                total,
                p.estadoStock(total)
        );
    }
}
